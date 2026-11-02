package dev.codeonigiri.exmekdeorum.tile;

import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.holder.fluid.FluidTankHelper;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import dev.codeonigiri.exmekdeorum.inventory.DeorumInputSlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.exdeorum.config.EConfig;
import thedarkcolour.exdeorum.recipe.RecipeCaches;
import thedarkcolour.exdeorum.recipe.barrel.BarrelCompostRecipe;
import thedarkcolour.exdeorum.recipe.barrel.BarrelFluidMixingRecipe;
import thedarkcolour.exdeorum.recipe.barrel.BarrelMixingRecipe;
import thedarkcolour.exdeorum.recipe.barrel.FluidTransformationRecipe;

/**
 * Machine version of Ex Deorum's barrel.
 *
 * <p>GUI layout: two item input slots (composting / mixing item), one item output slot (dirt or
 * mixing result), one fluid input tank filled from outside or by rain, and one fluid output tank
 * for catalyst-based fluid transformation. The two input slots are interchangeable: each tick the
 * machine tries the first slot that has an item.</p>
 *
 * <p>Recipes come from Ex Deorum's recipe caches: composting (item to dirt), item + fluid mixing,
 * in-world fluid mixing (fluid source above the barrel) and catalyst fluid transformation (full
 * input tank with a catalyst block below). A factory's compost/tank capacities are multiplied by
 * the process count and a completed compost cycle produces that many dirt at once.</p>
 */
public class TileEntityDeorumBarrel extends TileEntityDeorumMachine {

    private static final int BASE_TANK_CAPACITY = 1_000;
    private static final int BASE_COMPOST_CAPACITY = 1_000;

    // The item column (two inputs + energy) is vertically centred on the progress bar line.
    private static final int INPUT_A_X = 40;
    private static final int INPUT_A_Y = 22;
    private static final int INPUT_B_X = 40;
    private static final int INPUT_B_Y = 40;
    private static final int OUTPUT_X = 145;
    private static final int OUTPUT_Y = 22;
    /** Energy sits directly under the left-hand item column. */
    private static final int ENERGY_X = 40;
    private static final int ENERGY_Y = 58;

    private BasicFluidTank inputTank;
    private BasicFluidTank outputTank;

    private int compost;
    private float compostProgress;
    private float transformProgress;
    @Nullable
    private FluidTransformationRecipe currentTransform;

    public TileEntityDeorumBarrel(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
    }

    @Override
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        // Input tank: pipes may fill it while the barrel is idle; the machine drains it internally.
        inputTank = BasicFluidTank.create(getTankCapacity(),
                ConstantPredicates.alwaysTrueBi(),
                (stack, automationType) -> automationType == AutomationType.INTERNAL || isIdle(),
                ConstantPredicates.alwaysTrue(), listener);
        // Output tank: only the machine fills it (transformation result), pipes may drain it.
        outputTank = BasicFluidTank.create(getTankCapacity(),
                ConstantPredicates.alwaysTrueBi(),
                (stack, automationType) -> automationType == AutomationType.INTERNAL,
                ConstantPredicates.alwaysTrue(), listener);
        setFluidTanks(List.of(inputTank, outputTank));
        FluidTankHelper helper = FluidTankHelper.forSideWithConfig(this);
        helper.addTank(inputTank);
        helper.addTank(outputTank);
        return helper.build();
    }

    @Override
    protected void initInventory(InventorySlotHelper helper, IContentsListener listener) {
        inputSlots = new java.util.ArrayList<>(2);
        outputSlots = new java.util.ArrayList<>(1);
        inputSlots.add(helper.addSlot(DeorumInputSlot.at(this::isValidInput, listener, INPUT_A_X, INPUT_A_Y, inputLimit())));
        inputSlots.add(helper.addSlot(DeorumInputSlot.at(this::isValidInput, listener, INPUT_B_X, INPUT_B_Y, inputLimit())));
        outputSlots.add(helper.addSlot(OutputInventorySlot.at(listener, OUTPUT_X, OUTPUT_Y)));
        energySlot = helper.addSlot(EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, ENERGY_X, ENERGY_Y));
    }

    public int getTankCapacity() {
        return BASE_TANK_CAPACITY * operations();
    }

    public int getCompostCapacity() {
        return BASE_COMPOST_CAPACITY * operations();
    }

    public BasicFluidTank getInputTank() {
        return inputTank;
    }

    public BasicFluidTank getOutputTank() {
        return outputTank;
    }

    private boolean isIdle() {
        if (compost != 0) {
            return false;
        }
        for (IInventorySlot slot : inputSlots) {
            if (!slot.getStack().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return !stack.isEmpty();
    }

    @Override
    protected boolean tickMachine(ServerLevel level) {
        if (!hasEnergy()) {
            return false;
        }

        boolean didWork = false;
        boolean active = false;
        RecipeCaches caches = getRecipeCaches();

        // 1. Process one of the two input slots: mixing first (fluid present), otherwise composting.
        for (IInventorySlot slot : inputSlots) {
            ItemStack input = slot.getStack();
            if (input.isEmpty()) {
                continue;
            }
            if (tryCraft(caches, level, input)) {
                input.shrink(1);
                didWork = true;
                active = true;
            }
            break;
        }

        // 2. Collect rain water into the input tank.
        if (inputTank.getFluidAmount() < inputTank.getCapacity()
                && EConfig.SERVER.barrelsCollectRainWater.get()
                && level.isRainingAt(worldPosition.above())
                && isIdle()) {
            inputTank.insert(new FluidStack(Fluids.WATER, 1), Action.EXECUTE, AutomationType.INTERNAL);
        }

        // 3. Mix with a fluid source directly above the barrel.
        if (tryInWorldFluidMixing(caches, level)) {
            didWork = true;
            active = true;
        }

        // 4. Composting.
        if (compost >= getCompostCapacity()) {
            compostProgress += EConfig.SERVER.barrelProgressStep.get().floatValue() * stackMultiplier();
            active = true;
            if (compostProgress >= 1.0F) {
                ItemStack dirt = new ItemStack(Items.DIRT, operations());
                if (canInsertResult(dirt)) {
                    insertResult(dirt);
                    compost = 0;
                    compostProgress = 0.0F;
                    didWork = true;
                } else {
                    compostProgress = 1.0F;
                }
            }
        }

        // 5. Catalyst-based fluid transformation (input tank to output tank).
        if (tickTransformation(level, caches)) {
            didWork = true;
            active = true;
        }

        if (didWork) {
            useEnergy();
        }
        // Show the compost fill level while the barrel is still gathering biomass, then switch to
        // the dirt-producing step once the buffer is full; fluid transformation takes precedence as
        // it is the slowest of the three processes.
        float fill = compost <= 0 ? 0.0F : (float) compost / (float) getCompostCapacity();
        if (compost >= getCompostCapacity()) {
            fill = compostProgress;
        }
        progress = Math.max(transformProgress, fill);
        return active;
    }

    private boolean tryCraft(RecipeCaches caches, ServerLevel level, ItemStack stack) {
        if (inputTank.getFluidAmount() > 0) {
            BarrelMixingRecipe recipe = caches.getBarrelMixingRecipe(level.getRecipeManager(), stack, inputTank.getFluid());
            if (recipe != null && inputTank.getFluidAmount() >= recipe.getFluid().amount()) {
                ItemStack result = recipe.getResult().copy();
                if (canInsertResult(result)) {
                    inputTank.extract(recipe.getFluid().amount(), Action.EXECUTE, AutomationType.INTERNAL);
                    insertResult(result);
                    return true;
                }
            }
        }

        // A barrel that holds fluid can still be used for composting if the item has no mixing
        // recipe with that fluid (Ex Deorum also composts items independently of the tank).
        BarrelCompostRecipe recipe = caches.getBarrelCompostRecipe(stack);
        if (recipe != null && compost < getCompostCapacity()) {
            compost = Math.min(getCompostCapacity(), compost + recipe.getVolume());
            return true;
        }
        return false;
    }

    private boolean tryInWorldFluidMixing(RecipeCaches caches, ServerLevel level) {
        if (inputTank.isEmpty() || !isIdle()) {
            return false;
        }
        BlockPos abovePos = worldPosition.above();
        BlockState aboveState = level.getBlockState(abovePos);
        FluidState aboveFluidState = aboveState.getFluidState();
        Fluid aboveFluid = aboveFluidState.getType();
        if (aboveFluid == Fluids.EMPTY) {
            return false;
        }
        if (aboveFluid instanceof FlowingFluid flowing) {
            aboveFluid = flowing.getSource();
        }
        BarrelFluidMixingRecipe recipe = caches.getFluidMixingRecipe(inputTank.getFluid(), aboveFluid);
        if (recipe == null || inputTank.getFluidAmount() < recipe.baseFluid().amount()) {
            return false;
        }
        ItemStack result = recipe.result().copy();
        if (!canInsertResult(result)) {
            return false;
        }
        if (recipe.consumesAdditive()) {
            if (!(aboveState.getBlock() instanceof BucketPickup pickup)
                    || pickup.pickupBlock(null, level, abovePos, aboveState).isEmpty()) {
                return false;
            }
        }
        inputTank.extract(recipe.baseFluid().amount(), Action.EXECUTE, AutomationType.INTERNAL);
        insertResult(result);
        return true;
    }

    private boolean tickTransformation(ServerLevel level, RecipeCaches caches) {
        if (inputTank.getFluidAmount() != inputTank.getCapacity()) {
            currentTransform = null;
            transformProgress = 0.0F;
            return false;
        }
        if (currentTransform == null) {
            currentTransform = caches.getFluidTransformationRecipe(inputTank.getFluid().getFluid(), level.getBlockState(worldPosition.below()));
            transformProgress = 0.0F;
            if (currentTransform == null) {
                return false;
            }
        }

        FluidTransformationRecipe recipe = currentTransform;
        int catalysts = 0;
        for (BlockPos cursor : BlockPos.betweenClosed(worldPosition.getX() - 1, worldPosition.getY() - 1, worldPosition.getZ() - 1,
                worldPosition.getX() + 1, worldPosition.getY() - 1, worldPosition.getZ() + 1)) {
            if (!recipe.catalyst().test(level.getBlockState(cursor))) {
                continue;
            }
            catalysts++;
            if (!recipe.byproducts().isEmpty() && level.random.nextInt(1500) == 0) {
                BlockPos above = cursor.above();
                if (level.getBlockState(above).isAir()) {
                    BlockState byproduct = recipe.byproducts().getRandom(level.random);
                    if (byproduct.canSurvive(level, above)) {
                        level.setBlockAndUpdate(above, byproduct);
                    }
                }
            }
        }

        if (catalysts == 0) {
            currentTransform = null;
            transformProgress = 0.0F;
            return false;
        }

        transformProgress += (float) catalysts * stackMultiplier() / (float) recipe.duration();
        if (transformProgress >= 0.99999F) {
            transformProgress = 0.0F;
            if (outputTank.isEmpty()) {
                inputTank.setEmpty();
                outputTank.setStack(new FluidStack(recipe.resultFluid(), outputTank.getCapacity()));
                currentTransform = null;
            }
        }
        return true;
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, @NotNull Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.putInt("compost", compost);
        nbt.putFloat("compost_progress", compostProgress);
        nbt.putFloat("transform_progress", transformProgress);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull Provider provider) {
        super.loadAdditional(nbt, provider);
        compost = nbt.getInt("compost");
        compostProgress = nbt.getFloat("compost_progress");
        transformProgress = nbt.getFloat("transform_progress");
        currentTransform = null;
    }
}
