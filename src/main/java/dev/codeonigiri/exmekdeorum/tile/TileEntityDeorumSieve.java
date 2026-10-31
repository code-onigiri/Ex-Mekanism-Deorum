package dev.codeonigiri.exmekdeorum.tile;

import mekanism.api.IContentsListener;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import thedarkcolour.exdeorum.blockentity.logic.SieveLogic;
import thedarkcolour.exdeorum.recipe.RecipeCaches;
import thedarkcolour.exdeorum.tag.EItemTags;

/**
 * Machine version of Ex Deorum's sieve.
 *
 * <p>This machine delegates all sifting maths to Ex Deorum's own public {@link SieveLogic}, so
 * mesh enchantments (Efficiency / Fortune), {@code by_hand_only} recipes, moss drop limiting and
 * the configured sieve interval behave exactly like the Ex Deorum mechanical sieve. Each lane is
 * an independent {@link SieveLogic}; factories run 3/5/7/9 lanes in parallel.</p>
 *
 * <p>The input item stays in the input slot while a lane is working and is consumed only when that
 * lane's sifting operation completes.</p>
 */
public class TileEntityDeorumSieve extends TileEntityDeorumMachine implements SieveLogic.Owner {

    private static final int BASE_TICKS = 100;
    private static final String LANES_KEY = "deorum_sieve_lanes";

    private SieveLogic[] logics;
    private BasicInventorySlot meshSlot;
    private ItemStack lastMesh = ItemStack.EMPTY;

    public TileEntityDeorumSieve(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        int lanes = operations();
        logics = new SieveLogic[lanes];
        for (int i = 0; i < lanes; i++) {
            logics[i] = new SieveLogic(this, true);
        }
    }

    @Override
    protected void initInventory(InventorySlotHelper helper, IContentsListener listener) {
        super.initInventory(helper, listener);
        meshSlot = helper.addSlot(BasicInventorySlot.at(stack -> stack.is(EItemTags.SIEVE_MESHES), listener, AUX_X, AUX_Y, 1));
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (level == null || level.isClientSide || meshSlot == null || logics == null) {
            return true;
        }
        ItemStack mesh = meshSlot.getStack();
        return !mesh.isEmpty() && logics[0].isValidInput(stack);
    }

    @Override
    protected boolean tickMachine(ServerLevel level) {
        syncMesh(level);

        int claimed = countRunningLanes();
        boolean active = false;
        float increment = 1.0F / ticksFor(BASE_TICKS);
        for (SieveLogic logic : logics) {
            ItemStack contents = logic.getContents();
            if (contents.isEmpty()) {
                ItemStack input = getInputStack();
                if (input.isEmpty() || claimed >= input.getCount() || !logic.isValidInput(input)) {
                    continue;
                }
                // Keep the item in the input slot; it is consumed when the sifting operation completes.
                logic.startSifting(input.copyWithCount(1));
                contents = logic.getContents();
                claimed++;
            } else if (!matchesInput(contents)) {
                // The player removed or replaced the input: cancel rather than produce free drops.
                logic.setContents(ItemStack.EMPTY);
                logic.setProgress(0.0F);
                claimed--;
                continue;
            }

            if (!hasEnergy()) {
                break;
            }
            logic.sift(increment, level.getGameTime());
            useEnergy();
            active = true;
            if (logic.getContents().isEmpty()) {
                // sift() only clears its contents after the operation has completed and the drops
                // have been handled; at that point the source item may be consumed.
                getInputStack().shrink(1);
            }
        }

        if (active) {
            // Average only over running lanes, so a partially filled factory still shows a bar
            // that reaches 1.0 when its single running operation completes.
            float total = 0.0F;
            int running = 0;
            for (SieveLogic logic : logics) {
                if (!logic.getContents().isEmpty()) {
                    total += logic.getProgress();
                    running++;
                }
            }
            progress = running == 0 ? 0.0F : total / running;
        }
        return active;
    }

    private boolean matchesInput(ItemStack lane) {
        ItemStack input = getInputStack();
        return !input.isEmpty() && ItemStack.isSameItemSameComponents(input, lane);
    }

    private int countRunningLanes() {
        int running = 0;
        for (SieveLogic logic : logics) {
            if (!logic.getContents().isEmpty()) {
                running++;
            }
        }
        return running;
    }

    private void syncMesh(ServerLevel level) {
        ItemStack mesh = meshSlot.getStack();
        if (ItemStack.matches(mesh, lastMesh)) {
            return;
        }
        lastMesh = mesh.copy();
        for (SieveLogic logic : logics) {
            logic.setMesh(level.registryAccess(), mesh.copy());
        }
    }

    @Override
    public RecipeCaches getRecipeCaches() {
        return super.getRecipeCaches();
    }

    @Override
    public ServerLevel getServerLevel() {
        return (ServerLevel) level;
    }

    @Override
    public boolean handleResultItem(ItemStack stack, ServerLevel serverLevel, RandomSource random) {
        return insertResultAny(stack);
    }

    @Override
    public void markUpdated() {
        markForSave();
    }

    @Override
    public SieveLogic getLogic() {
        return logics[0];
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, @NotNull Provider provider) {
        super.saveAdditional(nbt, provider);
        CompoundTag lanes = new CompoundTag();
        for (int i = 0; i < logics.length; i++) {
            CompoundTag lane = new CompoundTag();
            logics[i].saveNbt(lane, provider);
            lanes.put("lane_" + i, lane);
        }
        nbt.put(LANES_KEY, lanes);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull Provider provider) {
        super.loadAdditional(nbt, provider);
        CompoundTag lanes = nbt.getCompound(LANES_KEY);
        for (int i = 0; i < logics.length; i++) {
            String key = "lane_" + i;
            if (lanes.contains(key, Tag.TAG_COMPOUND)) {
                logics[i].loadNbt(lanes.getCompound(key), provider);
            }
        }
    }
}
