package dev.codeonigiri.exmekdeorum.tile;

import dev.codeonigiri.exmekdeorum.inventory.DeorumAuxSlot;
import mekanism.api.IContentsListener;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import thedarkcolour.exdeorum.blockentity.logic.CompressedSieveLogic;
import thedarkcolour.exdeorum.blockentity.logic.SieveLogic;
import thedarkcolour.exdeorum.recipe.RecipeCaches;
import thedarkcolour.exdeorum.tag.EItemTags;

/**
 * Machine version of Ex Deorum's sieve.
 *
 * <p>This machine delegates all sifting maths to Ex Deorum's own public {@link SieveLogic}, so
 * mesh enchantments (Efficiency / Fortune), {@code by_hand_only} recipes, moss drop limiting and
 * the configured sieve interval behave exactly like the Ex Deorum mechanical sieve. Each lane is
 * an independent {@link SieveLogic}; factories run 4/8/12/16 lanes in parallel.</p>
 *
 * <p>The input item stays in the input slot while a lane is working and is consumed only when that
 * lane's sifting operation completes.</p>
 *
 * <p>Compressed blocks are detected from the installed mesh and sifted with Ex Deorum's
 * {@link CompressedSieveLogic} / {@code compressed_sieve} recipe table; regular blocks keep using
 * the normal sieve recipes. Only a mesh is needed in the auxiliary slot.</p>
 */
public class TileEntityDeorumSieve extends TileEntityDeorumMachine implements SieveLogic.Owner {

    private static final int BASE_TICKS = 100;
    private static final String LANES_KEY = "deorum_sieve_lanes";
    private static final String COMPRESSED_LANES_KEY = "deorum_sieve_compressed_lanes";

    private SieveLogic[] logics;
    private CompressedSieveLogic[] compressedLogics;
    private DeorumAuxSlot meshSlot;
    private ItemStack lastMesh = ItemStack.EMPTY;

    public TileEntityDeorumSieve(IBlockProvider blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        int lanes = operations();
        logics = new SieveLogic[lanes];
        compressedLogics = new CompressedSieveLogic[lanes];
        for (int i = 0; i < lanes; i++) {
            logics[i] = new SieveLogic(this, true);
            compressedLogics[i] = new CompressedSieveLogic(this, true);
        }
    }

    @Override
    protected void initInventory(InventorySlotHelper helper, IContentsListener listener) {
        super.initInventory(helper, listener);
        meshSlot = helper.addSlot(DeorumAuxSlot.at(stack -> stack.is(EItemTags.SIEVE_MESHES), listener, AUX_X, AUX_Y));
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
        if (mesh.isEmpty()) {
            return false;
        }
        return logics[0].isValidInput(stack) || compressedLogics[0].isValidInput(stack);
    }


    @Override
    protected boolean tickMachine(ServerLevel level) {
        syncMesh(level);

        int claimed = countRunningLanes();
        // Lanes beyond the current input count are not backed by an item: if the player
        // reduced the input stack mid-operation, the excess lanes are cancelled below.
        // matchesInput alone only compares the item type, so without this count check a
        // lane could complete (and drop items) for an item that is no longer there.
        int available = getInputStack().getCount();
        int backed = 0;
        boolean active = false;
        float increment = stackMultiplier() / (float) ticksFor(BASE_TICKS);
        for (int i = 0; i < logics.length; i++) {
            // A lane keeps whichever logic it started with, so swapping the hammer mid-operation
            // never mixes compressed and normal drops for the same item.
            SieveLogic lane = !logics[i].getContents().isEmpty() ? logics[i]
                    : !compressedLogics[i].getContents().isEmpty() ? compressedLogics[i] : null;
            if (lane == null) {
                ItemStack input = getInputStack();
                if (input.isEmpty() || claimed >= input.getCount()) {
                    continue;
                }
                SieveLogic target = null;
                if (compressedLogics[i].isValidInput(input)) {
                    target = compressedLogics[i];
                } else if (logics[i].isValidInput(input)) {
                    target = logics[i];
                }
                if (target == null) {
                    continue;
                }
                // Keep the item in the input slot; it is consumed when the sifting operation completes.
                target.startSifting(input.copyWithCount(1));
                lane = target;
                claimed++;
            } else if (!matchesInput(lane.getContents()) || backed >= available) {
                // The player removed or replaced the input, or reduced its count below the
                // number of running lanes: cancel rather than produce free drops.
                lane.setContents(ItemStack.EMPTY);
                lane.setProgress(0.0F);
                claimed--;
                continue;
            } else {
                backed++;
            }

            if (!hasEnergy()) {
                break;
            }
            lane.sift(increment, level.getGameTime());
            useEnergy();
            active = true;
            if (lane.getContents().isEmpty()) {
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
            for (int i = 0; i < logics.length; i++) {
                if (!logics[i].getContents().isEmpty()) {
                    total += logics[i].getProgress();
                    running++;
                }
                if (!compressedLogics[i].getContents().isEmpty()) {
                    total += compressedLogics[i].getProgress();
                    running++;
                }
            }
            progress = running == 0 ? 0.0F : total / running;
        }
        return active;
    }

    private boolean matchesInput(ItemStack lane) {
        ItemStack input = getInputStack();
        return !input.isEmpty() && ItemStack.isSameItemSameTags(input, lane);
    }

    private int countRunningLanes() {
        int running = 0;
        for (int i = 0; i < logics.length; i++) {
            if (!logics[i].getContents().isEmpty()) {
                running++;
            }
            if (!compressedLogics[i].getContents().isEmpty()) {
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
        for (int i = 0; i < logics.length; i++) {
            logics[i].setMesh(level.registryAccess(), mesh.copy());
            compressedLogics[i].setMesh(level.registryAccess(), mesh.copy());
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
    public void saveAdditional(@NotNull CompoundTag nbt) {
        super.saveAdditional(nbt);
        CompoundTag lanes = new CompoundTag();
        for (int i = 0; i < logics.length; i++) {
            CompoundTag lane = new CompoundTag();
            logics[i].saveNbt(lane, provider);
            lanes.put("lane_" + i, lane);
        }
        nbt.put(LANES_KEY, lanes);
        CompoundTag compressedLanes = new CompoundTag();
        for (int i = 0; i < compressedLogics.length; i++) {
            CompoundTag lane = new CompoundTag();
            compressedLogics[i].saveNbt(lane, provider);
            compressedLanes.put("lane_" + i, lane);
        }
        nbt.put(COMPRESSED_LANES_KEY, compressedLanes);
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        CompoundTag lanes = nbt.getCompound(LANES_KEY);
        for (int i = 0; i < logics.length; i++) {
            String key = "lane_" + i;
            if (lanes.contains(key, Tag.TAG_COMPOUND)) {
                logics[i].loadNbt(lanes.getCompound(key), provider);
            }
        }
        CompoundTag compressedLanes = nbt.getCompound(COMPRESSED_LANES_KEY);
        for (int i = 0; i < compressedLogics.length; i++) {
            String key = "lane_" + i;
            if (compressedLanes.contains(key, Tag.TAG_COMPOUND)) {
                compressedLogics[i].loadNbt(compressedLanes.getCompound(key), provider);
            }
        }
    }

    @Override
    public mekanism.api.inventory.IInventorySlot getAutomationAuxSlot() {
        return meshSlot;
    }
}
