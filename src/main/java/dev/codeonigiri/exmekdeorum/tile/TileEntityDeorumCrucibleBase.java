package dev.codeonigiri.exmekdeorum.tile;

import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.holder.fluid.FluidTankHelper;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import dev.codeonigiri.exmekdeorum.inventory.DeorumInputSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.exdeorum.recipe.RecipeUtil;
import thedarkcolour.exdeorum.recipe.crucible.CrucibleRecipe;

/**
 * Shared implementation of the lava and water crucible machines.
 *
 * <p>One item at a time is melted: the input item stays in its slot, the progress bar fills up at
 * a speed that depends on the heat source (lava crucible) or the fixed rate of 4 (water crucible),
 * and when the bar is full the recipe's fluid is put into the tank and the item is consumed. The
 * lava crucible only uses {@code LavaCrucibleRecipe}s and needs a heat source below the machine;
 * the water crucible only uses {@code WaterCrucibleRecipe}s.</p>
 *
 * <p>Factories do not run parallel item lanes here. Instead the tier multiplies the melting speed
 * (5x / 10x / 20x / 30x) and spends the same multiple of energy per tick, so the energy cost per
 * unit of fluid is unchanged. The tank scales with the multiplier as well.</p>
 */
public abstract class TileEntityDeorumCrucibleBase extends TileEntityDeorumMachine {

    private static final int BASE_TANK_CAPACITY = 4_000;
    private static final int WATER_MELTING_RATE = 4;
    /**
     * Item input sits 8 px left of the centred progress bar; the fluid gauge is on its right.
     * Input + energy form one group whose vertical centre is on the progress bar line.
     */
    private static final int CRUCIBLE_INPUT_X = 64;
    private static final int CRUCIBLE_INPUT_Y = 31;

    /** Progress of the item currently being melted, 0..1. */
    private float meltProgress;
    /** The item whose melt is in progress; changing the input restarts the progress. */
    private ItemStack meltingItem = ItemStack.EMPTY;

    protected TileEntityDeorumCrucibleBase(IBlockProvider blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
    }

    /**
     * Crucibles only expose the item input and the energy slot: the output is fluid, so there is no
     * item output area in the GUI.
     */
    @Override
    protected void initInventory(InventorySlotHelper helper, IContentsListener listener) {
        inputSlots = new ArrayList<>(1);
        outputSlots = new ArrayList<>(0);
        inputSlots.add(helper.addSlot(DeorumInputSlot.at(this::isValidInput, listener, CRUCIBLE_INPUT_X, CRUCIBLE_INPUT_Y, inputLimit())));
        // Energy sits directly under the single input slot.
        energySlot = helper.addSlot(EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, CRUCIBLE_INPUT_X, CRUCIBLE_INPUT_Y + 18));
    }

    /**
     * @return true for the lava crucible (heat source below, lava recipes); false for water.
     */
    protected abstract boolean isLava();

    /**
     * Looks the input up in the recipe cache that belongs to this crucible.
     */
    @Nullable
    protected abstract CrucibleRecipe recipeFor(ItemStack stack);

    /**
     * Melting / conversion multiplier: 1 for an untiered crucible, 5/10/20/30 for factories.
     */
    protected int meltMultiplier() {
        return factory && tier != null ? tier.getMeltMultiplier() : 1;
    }

    public int getTankCapacity() {
        return BASE_TANK_CAPACITY * meltMultiplier();
    }

    public float getMeltProgress() {
        return meltProgress;
    }

    @Override
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        // Reject external filling; the machine only outputs the fluid it melts.
        tank = BasicFluidTank.create(getTankCapacity(), ConstantPredicates.alwaysTrueBi(),
                ConstantPredicates.notExternal(), ConstantPredicates.alwaysTrue(), listener);
        setFluidTanks(List.of(tank));
        FluidTankHelper helper = FluidTankHelper.forSideWithConfig(this::getDirection, this::getConfig);
        helper.addTank(tank);
        return helper.build();
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // The menu asks the client-side tile for mayPlace; the recipe cache is not guaranteed to be
        // populated there, so accept on the client and let the server validate.
        if (level == null || level.isClientSide) {
            return true;
        }
        return recipeFor(stack) != null;
    }

    @Override
    protected boolean tickMachine(ServerLevel level) {
        int multiplier = meltMultiplier();
        ItemStack input = getInputStack();
        if (input.isEmpty()) {
            meltProgress = 0.0F;
            progress = 0.0F;
            return false;
        }

        if (!ItemStack.isSameItemSameTags(input, meltingItem)) {
            meltingItem = input.copyWithCount(1);
            meltProgress = 0.0F;
        }
        CrucibleRecipe recipe = recipeFor(input);
        if (recipe == null) {
            // No recipe: hold the item and do nothing (the input slot filter normally prevents this).
            meltProgress = 0.0F;
            progress = 0.0F;
            return false;
        }
        FluidStack result = recipe.getResult();
        if (!canAccept(result)) {
            progress = Math.min(1.0F, meltProgress);
            return false;
        }

        // Ex Deorum converts `heat` mB per 10 ticks; a factory melts `multiplier` times faster.
        int rate = isLava() ? RecipeUtil.getHeatValue(level.getBlockState(worldPosition.below())) : WATER_MELTING_RATE;
        if (rate <= 0) {
            // Lava crucible without a heat source below: nothing melts, but keep the item.
            progress = Math.min(1.0F, meltProgress);
            return false;
        }
        if (!hasEnergy(multiplier)) {
            progress = Math.min(1.0F, meltProgress);
            return false;
        }
        useEnergy(multiplier);

        meltProgress += (rate * multiplier * stackMultiplier()) / (result.getAmount() * 10.0F);
        if (meltProgress >= 1.0F) {
            tank.insert(result.copy(), Action.EXECUTE, AutomationType.INTERNAL);
            input.shrink(1);
            meltProgress = 0.0F;
            if (input.isEmpty()) {
                meltingItem = ItemStack.EMPTY;
            }
        }
        progress = Math.min(1.0F, meltProgress);
        return true;
    }

    private boolean canAccept(FluidStack result) {
        if (!tank.isEmpty() && tank.getFluid().getFluid() != result.getFluid()) {
            return false;
        }
        return tank.getFluidAmount() + result.getAmount() <= tank.getCapacity();
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putFloat("melt_progress", meltProgress);
        // The melting item must be saved together with the progress: without it the first
        // tick after a reload sees a different item than the (empty) melting item and
        // resets the restored progress to zero.
        if (!meltingItem.isEmpty()) {
            nbt.put("melting_item", meltingItem.save(new CompoundTag()));
        }
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        meltProgress = nbt.getFloat("melt_progress");
        meltingItem = nbt.contains("melting_item") ? ItemStack.of(nbt.getCompound("melting_item")) : ItemStack.EMPTY;
    }
}
