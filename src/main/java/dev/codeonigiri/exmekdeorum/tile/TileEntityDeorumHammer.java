package dev.codeonigiri.exmekdeorum.tile;

import dev.codeonigiri.exmekdeorum.inventory.DeorumAuxSlot;
import java.util.Arrays;
import mekanism.api.IContentsListener;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jetbrains.annotations.NotNull;
import thedarkcolour.exdeorum.loot.HammerLootModifier;
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
    /** Rolled results of lanes that finished but could not insert yet (output blocked). */
    private ItemStack[][] laneResults;
    private boolean[] laneFinished;
    private DeorumAuxSlot hammerSlot;

    public TileEntityDeorumHammer(IBlockProvider blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        int lanes = operations();
        laneInputs = new ItemStack[lanes];
        Arrays.fill(laneInputs, ItemStack.EMPTY);
        laneProgress = new float[lanes];
        laneResults = new ItemStack[lanes][];
        laneFinished = new boolean[lanes];
    }

    @Override
    protected void initInventory(InventorySlotHelper helper, IContentsListener listener) {
        super.initInventory(helper, listener);
        hammerSlot = helper.addSlot(DeorumAuxSlot.at(
                stack -> stack.is(EItemTags.HAMMERS) || stack.is(EItemTags.COMPRESSED_HAMMERS), listener, AUX_X, AUX_Y));
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
        return RecipeUtil.getHammerRecipe(stack.getItem()) != null || RecipeUtil.getCompressedHammerRecipe(stack.getItem()) != null;
    }

    @Override
    protected boolean tickMachine(ServerLevel level) {
        ItemStack hammer = hammerSlot.getStack();
        boolean compressed = hammer.is(EItemTags.COMPRESSED_HAMMERS);
        float efficiency = hammer.isEmpty() ? 1.0F : 2.0F + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, hammer) * 0.33F;
        float increment = efficiency * stackMultiplier() / ticksFor(BASE_TICKS);

        // Every running lane has claimed one item from the input slot; do not start more lanes than
        // there are items available. The item itself stays in the slot until a lane completes.
        int claimed = countRunningLanes();
        int available = getInputStack().getCount();

        boolean active = false;
        for (int i = 0; i < laneInputs.length; i++) {
            if (laneFinished[i]) {
                // The operation already finished and its result was rolled once; just wait
                // until the output accepts it. No energy is spent and nothing is re-rolled.
                if (finishLane(i, level)) {
                    claimed--;
                }
                continue;
            }
            ItemStack lane = laneInputs[i];
            if (lane.isEmpty()) {
                ItemStack input = getInputStack();
                if (input.isEmpty() || claimed >= available) {
                    continue;
                }
                if (recipeFor(input.getItem(), hammer) == null) {
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

            HammerRecipe recipe = recipeFor(lane.getItem(), hammer);
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
            if (count > 0) {
                count += HammerLootModifier.calculateFortuneBonus(hammer, context.getRandom(), count == 0);
            }
            // Roll once and keep the result with the lane; while the output is blocked the
            // lane waits in the finished state instead of re-rolling (and re-charging) per tick.
            laneResults[i] = buildResults(recipe, compressedRecipe, count);
            laneFinished[i] = true;
            laneProgress[i] = 1.0F;
            if (finishLane(i, level)) {
                claimed--;
            }
        }

        if (active) {
            progress = averageProgress();
        }
        return active;
    }

    /**
     * Builds the (up to two) result stacks for a finished operation. A compressed recipe keeps
     * its compression level: every full 9 results become one compressed item, and the remainder
     * is returned as ordinary items so nothing is lost.
     */
    private static ItemStack[] buildResults(HammerRecipe recipe, boolean compressedRecipe, int count) {
        if (count <= 0) {
            return new ItemStack[0];
        }
        ItemStack plain = hammerResult(recipe, count);
        if (compressedRecipe) {
            ItemStack compressedResult = compressedEquivalent(plain);
            if (!compressedResult.isEmpty()) {
                if (count < 9) {
                    return new ItemStack[]{plain};
                }
                compressedResult.setCount(count / 9);
                int remainder = count % 9;
                return remainder == 0
                        ? new ItemStack[]{compressedResult}
                        : new ItemStack[]{compressedResult, hammerResult(recipe, remainder)};
            }
        }
        return new ItemStack[]{plain};
    }

    private static ItemStack hammerResult(HammerRecipe recipe, int count) {
        ItemStack stack = new ItemStack(recipe.result, count);
        stack.setTag(recipe.getResultNbt());
        return stack;
    }

    /**
     * Tries to deliver a finished lane's stored results.
     *
     * @return true when the lane was resolved (delivered, or cancelled because the player
     * changed the input) and is free again.
     */
    private boolean finishLane(int i, ServerLevel level) {
        ItemStack[] results = laneResults[i];
        if (results.length > 0 && !canInsertResults(java.util.List.of(results))) {
            // Output still blocked: keep the input and the full progress bar, wait.
            return false;
        }
        if (!matchesInput(laneInputs[i])) {
            // The player removed or replaced the input: cancel without consuming anything.
            clearLane(i);
            return true;
        }
        // The operation finished: now consume the input item.
        getInputStack().shrink(1);
        for (ItemStack result : results) {
            insertResult(result);
        }
        damageHammer(level);
        clearLane(i);
        return true;
    }

    private void clearLane(int i) {
        laneInputs[i] = ItemStack.EMPTY;
        laneProgress[i] = 0.0F;
        laneResults[i] = null;
        laneFinished[i] = false;
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
        ResourceLocation compressedId = new ResourceLocation("exdeorum", "compressed_" + id.getPath());
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
    private static HammerRecipe recipeFor(Item item, ItemStack hammer) {
        if (hammer.is(EItemTags.COMPRESSED_HAMMERS)) {
            HammerRecipe compressed = RecipeUtil.getCompressedHammerRecipe(item);
            if (compressed != null) {
                return compressed;
            }
        }
        return RecipeUtil.getHammerRecipe(item);
    }

    /**
     * @return true if the input slot still holds the item a lane is working on.
     */
    private boolean matchesInput(ItemStack lane) {
        ItemStack input = getInputStack();
        return !input.isEmpty() && ItemStack.isSameItemSameTags(input, lane);
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
            if (hammer.hurt(1, level.getRandom(), null)) {
                hammer.shrink(1);
                if (hammer.isEmpty()) {
                    hammerSlot.setStack(ItemStack.EMPTY);
                }
            }
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

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt) {
        super.saveAdditional(nbt);
        ListTag lanes = new ListTag();
        for (int i = 0; i < laneInputs.length; i++) {
            CompoundTag lane = new CompoundTag();
            if (!laneInputs[i].isEmpty()) {
                lane.put("input", laneInputs[i].save(new CompoundTag()));
            }
            lane.putFloat("progress", laneProgress[i]);
            if (laneFinished[i] && laneResults[i] != null) {
                ListTag results = new ListTag();
                for (ItemStack result : laneResults[i]) {
                    results.add(result.save(new CompoundTag()));
                }
                lane.put("results", results);
            }
            lanes.add(lane);
        }
        nbt.put(LANES_KEY, lanes);
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        ListTag lanes = nbt.getList(LANES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < laneInputs.length && i < lanes.size(); i++) {
            CompoundTag lane = lanes.getCompound(i);
            laneInputs[i] = lane.contains("input") ? ItemStack.of(lane.getCompound("input")) : ItemStack.EMPTY;
            laneProgress[i] = lane.getFloat("progress");
            if (lane.contains("results", Tag.TAG_LIST)) {
                ListTag results = lane.getList("results", Tag.TAG_COMPOUND);
                laneResults[i] = new ItemStack[results.size()];
                for (int j = 0; j < results.size(); j++) {
                    laneResults[i][j] = ItemStack.of(results.getCompound(j));
                }
                laneFinished[i] = true;
            } else {
                // Older saves stored no results; a full bar simply completes and rolls again.
                laneResults[i] = null;
                laneFinished[i] = false;
            }
        }
    }

    @Override
    public mekanism.api.inventory.IInventorySlot getAutomationAuxSlot() {
        return hammerSlot;
    }
}
