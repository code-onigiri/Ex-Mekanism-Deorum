package dev.codeonigiri.exmekdeorum.tile;

import dev.codeonigiri.exmekdeorum.machine.AttributeMachineFactory;
import dev.codeonigiri.exmekdeorum.machine.AttributeMachineFamily;
import dev.codeonigiri.exmekdeorum.machine.AttributeMachineTier;
import dev.codeonigiri.exmekdeorum.machine.DeorumMachineFamily;
import dev.codeonigiri.exmekdeorum.machine.MachineTier;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.common.attachments.component.AttachedSideConfig;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableFloat;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.exdeorum.recipe.RecipeCaches;
import thedarkcolour.exdeorum.recipe.RecipeUtil;

/**
 * Shared base for the Ex Deorum machine variants.
 *
 * <p>Recipes are not re-declared: every tick the concrete machine asks Ex Deorum's
 * {@link RecipeCaches} (rebuilt from the server recipe manager on reload), so any recipe added by
 * a datapack, by Ex Deorum itself or by another addon is picked up automatically.</p>
 *
 * <p>Tier / family / factory variant are stored as block attributes and read in
 * {@link #presetVariables()}, which runs before the inventory and energy holders are created.</p>
 */
public abstract class TileEntityDeorumMachine extends TileEntityConfigurableMachine {

    protected static final int INPUT_X = 30;
    protected static final int INPUT_Y = 18;
    protected static final int AUX_X = 30;
    protected static final int AUX_Y = 36;
    protected static final int ENERGY_X = 30;
    protected static final int ENERGY_Y = 54;
    protected static final int OUTPUT_X = 66;
    protected static final int OUTPUT_Y = 18;
    protected static final int OUTPUT_COLUMNS = 5;
    protected static final int OUTPUT_ROWS = 4;
    public static final int OUTPUT_SLOTS = OUTPUT_COLUMNS * OUTPUT_ROWS;

    @Nullable
    protected MachineTier tier;
    protected DeorumMachineFamily family;
    protected boolean factory;

    protected MachineEnergyContainer<TileEntityDeorumMachine> energyContainer;
    protected EnergyInventorySlot energySlot;
    protected List<IInventorySlot> inputSlots;
    protected List<IInventorySlot> outputSlots;
    /**
     * Fluid tanks in GUI order: the barrel has an input tank first and an output tank second, the
     * crucibles have a single output tank. Assigned by subclasses while the tile is constructed.
     */
    // No initializer: subclasses assign this from getInitialFluidTanks(), which the super
    // constructor calls before this class's field initialisers would run (an initializer here
    // would silently wipe the tanks for barrel/crucible machines).
    protected List<BasicFluidTank> fluidTanks;
    @Nullable
    protected BasicFluidTank tank;

    /**
     * Progress shown by the GUI, in the range [0, 1].
     */
    protected float progress;

    protected TileEntityDeorumMachine(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        ejectorComponent = new TileComponentEjector(this);
        if (family.isFluidMachine()) {
            ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM, TransmissionType.FLUID);
        } else {
            ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
        }
        configComponent.setupItemIOConfig(inputSlots, outputSlots, energySlot, false);
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);
        if (fluidTanks == null) {
            fluidTanks = List.of();
        }
        setupFluidConfig();
        applyDefaultSideConfig();
    }

    /**
     * Registers the fluid transmission.
     *
     * <p>One tank (crucibles) can be both filled and drained; two tanks (barrel) are registered as
     * an input/output pair so pipes fill the input tank and drain the output tank.</p>
     */
    private void setupFluidConfig() {
        if (fluidTanks.isEmpty()) {
            return;
        }
        if (fluidTanks.size() == 1) {
            configComponent.setupIOConfig(TransmissionType.FLUID, fluidTanks.get(0), RelativeSide.RIGHT);
        } else {
            configComponent.setupIOConfig(TransmissionType.FLUID, fluidTanks.get(0), fluidTanks.get(1), RelativeSide.RIGHT);
        }
    }

    /**
     * Assigns the fluid tanks and remembers the first one for the single-tank accessor.
     */
    protected void setFluidTanks(List<BasicFluidTank> tanks) {
        this.fluidTanks = List.copyOf(tanks);
        this.tank = tanks.isEmpty() ? null : tanks.get(0);
    }

    /**
     * Migration for machines that were saved before the default side config existed: their stored
     * config contains {@code NONE} on every side, which disables the capabilities (cables and pipes
     * do not connect). Only an all-{@code NONE} config is touched, so a player-configured machine is
     * never overwritten.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        ConfigInfo energy = configComponent.getConfig(TransmissionType.ENERGY);
        if (energy != null && allSidesNone(energy)) {
            applyDefaultSideConfig();
        }
    }

    private static boolean allSidesNone(ConfigInfo info) {
        for (RelativeSide side : EnumUtils.SIDES) {
            if (info.getDataType(side) != DataType.NONE) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets the side config a freshly placed machine starts with.
     *
     * <p>This mirrors the {@code SIDE_CONFIG} data component that the block item carries, so the
     * machine also works when it is created without item placement (structure blocks, /setblock,
     * world generation, ...). A side config of {@code DataType.NONE} on every side would disable
     * the item/energy/fluid capabilities, which in turn stops Mekanism cables and pipes from
     * connecting to the machine at all.</p>
     */
    private void applyDefaultSideConfig() {
        applySideConfig(TransmissionType.ITEM, AttachedSideConfig.LightConfigInfo.MACHINE);
        applySideConfig(TransmissionType.ENERGY, AttachedSideConfig.LightConfigInfo.INPUT_ONLY);
        if (!fluidTanks.isEmpty()) {
            applySideConfig(TransmissionType.FLUID, AttachedSideConfig.LightConfigInfo.INPUT_OUT_ALL);
        }
    }

    private void applySideConfig(TransmissionType type, AttachedSideConfig.LightConfigInfo lightConfigInfo) {
        ConfigInfo info = configComponent.getConfig(type);
        if (info == null) {
            return;
        }
        info.setEjecting(lightConfigInfo.isEjecting());
        for (RelativeSide side : EnumUtils.SIDES) {
            info.setDataType(lightConfigInfo.getDataType(side), side);
        }
    }

    @Override
    protected void presetVariables() {
        super.presetVariables();
        AttributeMachineTier tierAttribute = Attribute.get(getBlockHolder(), AttributeMachineTier.class);
        tier = tierAttribute == null ? null : tierAttribute.tier();
        family = Attribute.getOrThrow(getBlockHolder(), AttributeMachineFamily.class).family();
        factory = Attribute.getOrThrow(getBlockHolder(), AttributeMachineFactory.class).factory();
    }

    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        energyContainer = MachineEnergyContainer.input(this, listener);
        EnergyContainerHelper helper = EnergyContainerHelper.forSideWithConfig(this);
        helper.addContainer(energyContainer);
        return helper.build();
    }

    @Nullable
    @Override
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        return null;
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper helper = InventorySlotHelper.forSideWithConfig(this);
        initInventory(helper, listener);
        return helper.build();
    }

    /**
     * Builds the common inventory. Subclasses may override to add their auxiliary slot and must
     * call {@code super.initInventory(helper, listener)} first.
     */
    protected void initInventory(InventorySlotHelper helper, IContentsListener listener) {
        inputSlots = new ArrayList<>(1);
        outputSlots = new ArrayList<>(OUTPUT_SLOTS);
        inputSlots.add(helper.addSlot(InputInventorySlot.at(this::isValidInput, listener, INPUT_X, INPUT_Y)));
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            outputSlots.add(helper.addSlot(OutputInventorySlot.at(listener,
                    OUTPUT_X + (i % OUTPUT_COLUMNS) * 18, OUTPUT_Y + (i / OUTPUT_COLUMNS) * 18)));
        }
        energySlot = helper.addSlot(EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, ENERGY_X, ENERGY_Y));
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        if (level instanceof ServerLevel serverLevel) {
            energySlot.fillContainerOrConvert();
            boolean active = tickMachine(serverLevel);
            setActive(active);
            if (active) {
                sendUpdatePacket = true;
            }
        }
        return sendUpdatePacket;
    }

    /**
     * Runs one server tick of the machine.
     *
     * @return true if the machine did work this tick (used for the active light).
     */
    protected abstract boolean tickMachine(ServerLevel level);

    /**
     * Whether the given stack is a valid input for this machine. Called on the client as well, so
     * never touch the server recipe cache without checking {@code level}.
     */
    protected boolean isValidInput(ItemStack stack) {
        return !stack.isEmpty();
    }

    protected RecipeCaches getRecipeCaches() {
        return RecipeUtil.getCaches(level);
    }

    /**
     * Number of parallel operations: 1 for an untiered machine, 3/5/7/9 for a factory.
     */
    public int operations() {
        return factory && tier != null ? tier.getFactoryProcesses() : 1;
    }

    /**
     * Operation time for one lane. Factories do not speed up individual operations; they run more
     * of them in parallel, exactly like Mekanism factories.
     */
    protected int ticksFor(int baseTicks) {
        return Math.max(1, MekanismUtils.getTicks(this, baseTicks));
    }

    protected boolean hasEnergy() {
        return hasEnergy(1);
    }

    protected boolean hasEnergy(int operations) {
        long usage = energyContainer.getEnergyPerTick() * Math.max(1, operations);
        return energyContainer.extract(usage, Action.SIMULATE, AutomationType.INTERNAL) >= usage;
    }

    protected void useEnergy() {
        useEnergy(1);
    }

    protected void useEnergy(int operations) {
        long usage = energyContainer.getEnergyPerTick() * Math.max(1, operations);
        energyContainer.extract(usage, Action.EXECUTE, AutomationType.INTERNAL);
    }

    @NotNull
    protected ItemStack getInputStack() {
        return inputSlots.isEmpty() ? ItemStack.EMPTY : inputSlots.get(0).getStack();
    }

    /**
     * @return true if the full stack can be merged into the output slots.
     */
    protected boolean canInsertResult(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        int remaining = stack.getCount();
        for (IInventorySlot slot : outputSlots) {
            ItemStack existing = slot.getStack();
            if (existing.isEmpty()) {
                remaining -= Math.min(remaining, slot.getLimit(stack));
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                remaining -= Math.min(remaining, slot.getLimit(existing) - existing.getCount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inserts as much as possible of the stack into the output slots.
     *
     * @return true if at least one item was inserted.
     */
    protected boolean insertResultAny(ItemStack stack) {
        boolean inserted = false;
        ItemStack remaining = stack.copy();
        for (IInventorySlot slot : outputSlots) {
            int before = remaining.getCount();
            remaining = slot.insertItem(remaining, Action.EXECUTE, AutomationType.INTERNAL);
            if (remaining.getCount() != before) {
                inserted = true;
            }
            if (remaining.isEmpty()) {
                break;
            }
        }
        return inserted;
    }

    /**
     * Inserts the full stack. Callers should have checked {@link #canInsertResult(ItemStack)}.
     */
    protected boolean insertResult(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack remaining = stack.copy();
        for (IInventorySlot slot : outputSlots) {
            remaining = slot.insertItem(remaining, Action.EXECUTE, AutomationType.INTERNAL);
            if (remaining.isEmpty()) {
                return true;
            }
        }
        return remaining.getCount() < stack.getCount();
    }

    public MachineEnergyContainer<TileEntityDeorumMachine> getEnergyContainer() {
        return energyContainer;
    }

    @Nullable
    public BasicFluidTank getTank() {
        return tank;
    }

    /**
     * All fluid tanks in GUI order. Empty for hammer and sieve.
     */
    public List<BasicFluidTank> getFluidTanks() {
        return fluidTanks;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    @Nullable
    public MachineTier getTier() {
        return tier;
    }

    public DeorumMachineFamily getFamily() {
        return family;
    }

    public boolean isFactory() {
        return factory;
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableFloat.create(this::getProgress, this::setProgress));
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, @NotNull Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.putFloat("progress", progress);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull Provider provider) {
        super.loadAdditional(nbt, provider);
        progress = nbt.getFloat("progress");
    }
}
