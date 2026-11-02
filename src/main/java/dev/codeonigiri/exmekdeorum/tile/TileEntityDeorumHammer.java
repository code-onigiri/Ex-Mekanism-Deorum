package dev.codeonigiri.exmekdeorum.tile;

import java.util.Arrays;
import mekanism.api.IContentsListener;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jetbrains.annotations.NotNull;
import thedarkcolour.exdeorum.loot.HammerLootModifier;
import thedarkcolour.exdeorum.recipe.RecipeCaches;
import thedarkcolour.exdeorum.recipe.RecipeUtil;
import thedarkcolour.exdeorum.recipe.hammer.CompressedHammerRecipe;
import thedarkcolour.exdeorum.recipe.hammer.HammerRecipe;
import thedarkcolour.exdeorum.tag.EItemTags;

/**
 * Machine version of Ex Deorum's hammer.
 *
 * <p>Each lane holds one input item and fills a progress bar. Running the bar to 1 rolls
 * {@code result_amount} from the inherited {@link HammerRecipe} plus the same Fortune bonus Ex
 * Deorum's mechanical hammer rolls. A hammer in the auxiliary slot changes the speed (2x + 0.33x
 * per Efficiency level) and is damaged once per completed operation, exactly like Ex Deorum.</p>
 *
 * <p>If the auxiliary slot holds a compressed hammer (any item in {@code EItemTags.COMPRESSED_HAMMERS})
 * the machine uses Ex Deorum's {@code CompressedHammerRecipe} cache instead, so compressed blocks
 * are processed without a separate machine. The input item stays in the input slot for the whole
 * operation and is only consumed when the operation finishes.</p>
 */
public class TileEntityDeorumHammer extends TileEntityDeorumMachine {

    private static final int BASE_TICKS = 200;
    private static final String LANES_KEY = "deorum_hammer_lanes";

    private ItemStack[] laneInputs;
    private float[] laneProgress;
    private BasicInventorySlot hammerSlot;

    public TileEntityDeorumHammer(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        int lanes = operations();
        laneInputs = new ItemStack[lanes];
        Arrays.fill(laneInputs, ItemStack.EMPTY);
        laneProgress = new float[lanes];
    }

    @Override
    protected void initInventory(InventorySlotHelper helper, IContentsListener listener) {
        super.initInventory(helper, listener);
        hammerSlot = helper.addSlot(BasicInventorySlot.at(
                stack -> stack.is(EItemTags.HAMMERS) || stack.is(EItemTags.COMPRESSED_HAMMERS), listener, AUX_X, AUX_Y, 1));
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (level == null || level.isClientSide || hammerSlot == null) {
            // Accept while the inventory is being built and on the client.
            return true;
        }
        // Accept anything either hammer type can process, independent of which hammer happens to be
        // installed right now: the player may insert the blocks before the hammer. A lane only starts
        // once a recipe matches the currently installed hammer.
        RecipeCaches caches = getRecipeCaches();
        return caches.getHammerRecipe(stack.getItem()) != null || caches.getCompressedHammerRecipe(stack.getItem()) != null;
    }

    @Override
    protected boolean tickMachine(ServerLevel level) {
        RecipeCaches caches = getRecipeCaches();
        ItemStack hammer = hammerSlot.getStack();
        boolean compressed = hammer.is(EItemTags.COMPRESSED_HAMMERS);
        float efficiency = hammer.isEmpty() ? 1.0F : 2.0F + enchantLevel(hammer, Enchantments.EFFICIENCY, level) * 0.33F;
        float increment = efficiency * stackMultiplier() / ticksFor(BASE_TICKS);

        // Every running lane has claimed one item from the input slot; do not start more lanes than
        // there are items available. The item itself stays in the slot until a lane completes.
        int claimed = countRunningLanes();
        int available = getInputStack().getCount();

        boolean active = false;
        for (int i = 0; i < laneInputs.length; i++) {
            ItemStack lane = laneInputs[i];
            if (lane.isEmpty()) {
                ItemStack input = getInputStack();
                if (input.isEmpty() || claimed >= available) {
                    continue;
                }
                if (recipeFor(caches, input.getItem(), hammer) == null) {
                    continue;
                }
                laneInputs[i] = input.copyWithCount(1);
                lane = laneInputs[i];
                laneProgress[i] = 0.0F;
                claimed++;
            } else if (!matchesInput(lane)) {
                // The player removed or replaced the input: cancel without consuming anything.
                laneInputs[i] = ItemStack.EMPTY;
                laneProgress[i] = 0.0F;
                claimed--;
                continue;
            }

            HammerRecipe recipe = recipeFor(caches, lane.getItem(), hammer);
            if (recipe == null) {
                laneInputs[i] = ItemStack.EMPTY;
                laneProgress[i] = 0.0F;
                claimed--;
                continue;
            }

            if (!hasEnergy()) {
                break;
            }
            laneProgress[i] += increment;
            useEnergy();
            active = true;
            if (laneProgress[i] < 1.0F) {
                continue;
            }

            LootContext context = RecipeUtil.emptyLootContext(level);
            int count = recipe.resultAmount.getInt(context);
            boolean compressedRecipe = recipe instanceof CompressedHammerRecipe;
            if (count > 0 && !lane.is(compressedRecipe ? EItemTags.COMPRESSED_HAMMER_FORTUNE_BLACKLIST : EItemTags.HAMMER_FORTUNE_BLACKLIST)) {
                count += HammerLootModifier.calculateFortuneBonus(level.registryAccess(), hammer, context.getRandom(), count == 0);
            }
            ItemStack result;
            if (compressedRecipe) {
                // 圧縮ハンマーは圧縮段階を保つ: 9 個ぶんの結果を 1 個の圧縮アイテムにまとめる。
                ItemStack compressedResult = compressedEquivalent(recipe.result);
                if (!compressedResult.isEmpty() && count >= 9) {
                    compressedResult.setCount(count / 9);
                    result = compressedResult;
                } else {
                    result = count > 0 ? recipe.result.copyWithCount(count) : ItemStack.EMPTY;
                }
            } else {
                result = count > 0 ? recipe.result.copyWithCount(count) : ItemStack.EMPTY;
            }
            if (!result.isEmpty() && !canInsertResult(result)) {
                // Retry next tick: keep the progress bar full and the input in the slot.
                laneProgress[i] = 1.0F;
                continue;
            }
            if (!matchesInput(lane)) {
                laneInputs[i] = ItemStack.EMPTY;
                laneProgress[i] = 0.0F;
                claimed--;
                continue;
            }
            // The operation finished: now consume the input item.
            getInputStack().shrink(1);
            if (!result.isEmpty()) {
                insertResult(result);
            }
            damageHammer(level);
            laneInputs[i] = ItemStack.EMPTY;
            laneProgress[i] = 0.0F;
            claimed--;
        }

        if (active) {
            progress = averageProgress();
        }
        return active;
    }

    /**
     * Maps an Ex Deorum hammer result to its compressed counterpart when one exists, e.g.
     * {@code minecraft:gravel} to {@code exdeorum:compressed_gravel}. Used by compressed hammers so
     * the compression level survives the operation instead of being expanded to nine items.
     */
    private static ItemStack compressedEquivalent(ItemStack result) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(result.getItem());
        if (id == null) {
            return ItemStack.EMPTY;
        }
        ResourceLocation compressedId = ResourceLocation.fromNamespaceAndPath("exdeorum", "compressed_" + id.getPath());
        Item compressed = BuiltInRegistries.ITEM.get(compressedId);
        if (compressed == null || compressed == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(compressed);
    }

    /**
     * Prefers the compressed recipe when a compressed hammer is installed, but falls back to the
     * normal recipe so a compressed hammer still works like an ordinary hammer on ordinary blocks.
     */
    private static HammerRecipe recipeFor(RecipeCaches caches, Item item, ItemStack hammer) {
        if (hammer.is(EItemTags.COMPRESSED_HAMMERS)) {
            HammerRecipe compressed = caches.getCompressedHammerRecipe(item);
            if (compressed != null) {
                return compressed;
            }
        }
        return caches.getHammerRecipe(item);
    }

    /**
     * @return true if the input slot still holds the item a lane is working on.
     */
    private boolean matchesInput(ItemStack lane) {
        ItemStack input = getInputStack();
        return !input.isEmpty() && ItemStack.isSameItemSameComponents(input, lane);
    }

    private int countRunningLanes() {
        int running = 0;
        for (ItemStack lane : laneInputs) {
            if (!lane.isEmpty()) {
                running++;
            }
        }
        return running;
    }

    private void damageHammer(ServerLevel level) {
        ItemStack hammer = hammerSlot.getStack();
        if (!hammer.isEmpty() && hammer.isDamageableItem()) {
            hammer.hurtAndBreak(1, level, null, item -> {
            });
        }
    }

    private float averageProgress() {
        // Average only over lanes that are actually running: with a single item in a 9-lane
        // factory the bar must fill up instead of stopping at 1/9.
        float total = 0.0F;
        int running = 0;
        for (int i = 0; i < laneProgress.length; i++) {
            if (!laneInputs[i].isEmpty()) {
                total += laneProgress[i];
                running++;
            }
        }
        return running == 0 ? 0.0F : total / running;
    }

    private static int enchantLevel(ItemStack stack, ResourceKey<Enchantment> enchantment, ServerLevel level) {
        return stack.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment));
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, @NotNull Provider provider) {
        super.saveAdditional(nbt, provider);
        ListTag lanes = new ListTag();
        for (int i = 0; i < laneInputs.length; i++) {
            CompoundTag lane = new CompoundTag();
            if (!laneInputs[i].isEmpty()) {
                lane.put("input", laneInputs[i].save(provider));
            }
            lane.putFloat("progress", laneProgress[i]);
            lanes.add(lane);
        }
        nbt.put(LANES_KEY, lanes);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull Provider provider) {
        super.loadAdditional(nbt, provider);
        ListTag lanes = nbt.getList(LANES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < laneInputs.length && i < lanes.size(); i++) {
            CompoundTag lane = lanes.getCompound(i);
            laneInputs[i] = lane.contains("input") ? ItemStack.parseOptional(provider, lane.getCompound("input")) : ItemStack.EMPTY;
            laneProgress[i] = lane.getFloat("progress");
        }
    }

    @Override
    public mekanism.api.inventory.IInventorySlot getAutomationAuxSlot() {
        return hammerSlot;
    }
}
