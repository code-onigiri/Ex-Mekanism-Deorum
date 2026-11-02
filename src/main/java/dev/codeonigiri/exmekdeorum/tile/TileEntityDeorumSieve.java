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
 * an independent {@link SieveLogic}; factories run 3/5/7/9 lanes in parallel.</p>
 *
 * <p>The input item stays in the input slot while a lane is working and is consumed only when that
 * lane's sifting operation completes.</p>
 *
 * <p>When a compressed hammer is placed in the hammer slot, new lanes switch to Ex Deorum's
 * {@link CompressedSieveLogic}, so compressed blocks are sifted with the {@code compressed_sieve}
 * recipe table. The mesh is still required (as in Ex Deorum's compressed sieve block); lanes that
 * were already running keep their original logic.</p>
 */
public class TileEntityDeorumSieve extends TileEntityDeorumMachine implements SieveLogic.Owner {

    private static final int BASE_TICKS = 100;
    private static final String LANES_KEY = "deorum_sieve_lanes";
    private static final String COMPRESSED_LANES_KEY = "deorum_sieve_compressed_lanes";
    private static final int HAMMER_X = 48;
    private static final int HAMMER_Y = 54;

    private SieveLogic[] logics;
    private CompressedSieveLogic[] compressedLogics;
    private BasicInventorySlot meshSlot;
    private BasicInventorySlot hammerSlot;
    private ItemStack lastMesh = ItemStack.EMPTY;

    public TileEntityDeorumSieve(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
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
        meshSlot = helper.addSlot(BasicInventorySlot.at(stack -> stack.is(EItemTags.SIEVE_MESHES), listener, AUX_X, AUX_Y, 1));
        hammerSlot = helper.addSlot(BasicInventorySlot.at(stack -> stack.is(EItemTags.COMPRESSED_HAMMERS), listener, HAMMER_X, HAMMER_Y, 1));
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

    private boolean hasCompressedHammer() {
        return hammerSlot != null && !hammerSlot.getStack().isEmpty();
    }

    @Override
    protected boolean tickMachine(ServerLevel level) {
        syncMesh(level);

        boolean compressed = hasCompressedHammer();
        int claimed = countRunningLanes();
        boolean active = false;
        float increment = 1.0F / ticksFor(BASE_TICKS);
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
                if (compressed && compressedLogics[i].isValidInput(input)) {
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
            } else if (!matchesInput(lane.getContents())) {
                // The player removed or replaced the input: cancel rather than produce free drops.
                lane.setContents(ItemStack.EMPTY);
                lane.setProgress(0.0F);
                claimed--;
                continue;
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
                for (SieveLogic lane : new SieveLogic[]{logics[i], compressedLogics[i]}) {
                    if (!lane.getContents().isEmpty()) {
                        total += lane.getProgress();
                        running++;
                    }
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
    public void saveAdditional(@NotNull CompoundTag nbt, @NotNull Provider provider) {
        super.saveAdditional(nbt, provider);
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
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull Provider provider) {
        super.loadAdditional(nbt, provider);
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
