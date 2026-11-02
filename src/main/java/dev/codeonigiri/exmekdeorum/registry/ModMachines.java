package dev.codeonigiri.exmekdeorum.registry;

import dev.codeonigiri.exmekdeorum.ExMekanismDeorum;
import dev.codeonigiri.exmekdeorum.compat.TierAttributes;
import dev.codeonigiri.exmekdeorum.block.BlockDeorumMachine;
import dev.codeonigiri.exmekdeorum.machine.AttributeMachineFactory;
import dev.codeonigiri.exmekdeorum.machine.AttributeMachineFamily;
import dev.codeonigiri.exmekdeorum.machine.AttributeMachineTier;
import dev.codeonigiri.exmekdeorum.machine.DeorumMachineFamily;
import dev.codeonigiri.exmekdeorum.machine.MachineTier;
import dev.codeonigiri.exmekdeorum.tile.TileEntityDeorumBarrel;
import dev.codeonigiri.exmekdeorum.tile.TileEntityDeorumCrucible;
import dev.codeonigiri.exmekdeorum.tile.TileEntityDeorumHammer;
import dev.codeonigiri.exmekdeorum.tile.TileEntityDeorumMachine;
import dev.codeonigiri.exmekdeorum.tile.TileEntityDeorumSieve;
import dev.codeonigiri.exmekdeorum.tile.TileEntityDeorumWaterCrucible;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import mekanism.api.text.ILangEntry;
import mekanism.common.attachments.component.AttachedEjector;
import mekanism.common.attachments.component.AttachedSideConfig;
import mekanism.common.block.attribute.AttributeEnergy;
import mekanism.common.block.attribute.AttributeGui;
import mekanism.common.block.attribute.AttributeSideConfig;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registration.MekanismDeferredHolder;
import mekanism.common.registration.impl.BlockDeferredRegister;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registration.impl.CreativeTabDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * Registers the machine blocks.
 *
 * <p>This follows Mekanism's naming convention: the untiered machines are
 * {@code hammer_machine}, {@code sieve_machine}, {@code barrel_machine} and
 * {@code crucible_machine}, while factories are {@code basic_hammer_factory},
 * {@code advanced_hammer_factory}, and so on (4 families × (1 machine + 4 factories) = 20
 * blocks).</p>
 *
 * <p>Each block gets its own block entity type because Mekanism's
 * {@code TileEntityTypeDeferredRegister.mekBuilder} builds a type that contains exactly one block.
 * The tile class is shared per family; the family / factory / tier data is carried by block
 * attributes and read by the tile in {@code presetVariables()}.</p>
 */
public final class ModMachines {

    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(ExMekanismDeorum.MOD_ID);
    public static final TileEntityTypeDeferredRegister TILE_TYPES = new TileEntityTypeDeferredRegister(ExMekanismDeorum.MOD_ID);
    public static final CreativeTabDeferredRegister TABS = new CreativeTabDeferredRegister(ExMekanismDeorum.MOD_ID);

    private static final List<BlockRegistryObject<?, ?>> ALL = new ArrayList<>();
    private static final Map<String, BlockRegistryObject<?, ?>> BY_NAME = new HashMap<>();
    public static final MekanismDeferredHolder<CreativeModeTab, CreativeModeTab> TAB;

    /**
     * Item side configuration used by the two fluid families (barrel, crucible).
     *
     * <p>Item and energy behave exactly like a normal Mekanism machine; the fluid tank can be both
     * filled and drained from every side, so Mekanism mechanical pipes connect to the barrel and
     * the crucible out of the box. Mekanism's own machines carry this information on the item
     * ({@code SIDE_CONFIG} data component) and apply it to the tile when placed. Without it every
     * side would default to {@code DataType.NONE}, the energy/fluid/item capabilities would be
     * disabled and cables/pipes would not connect at all.</p>
     */
    private static final AttachedSideConfig FLUID_MACHINE;

    static {
        Map<TransmissionType, AttachedSideConfig.LightConfigInfo> configInfo = new EnumMap<>(TransmissionType.class);
        configInfo.put(TransmissionType.ITEM, AttachedSideConfig.LightConfigInfo.MACHINE);
        configInfo.put(TransmissionType.ENERGY, AttachedSideConfig.LightConfigInfo.INPUT_ONLY);
        configInfo.put(TransmissionType.FLUID, AttachedSideConfig.LightConfigInfo.INPUT_OUT_ALL);
        FLUID_MACHINE = new AttachedSideConfig(configInfo);
    }

    static {
        for (DeorumMachineFamily family : DeorumMachineFamily.values()) {
            createMachine(family);
            for (MachineTier tier : MachineTier.values()) {
                if (!tier.isAvailable()) {
                    continue;
                }
                createFactory(family, tier);
            }
        }
        TAB = TABS.registerMain(new DeorumLangEntry("itemGroup.exmekdeorum"), ALL.get(0).getItemHolder(),
                builder -> builder.displayItems((params, output) -> CreativeTabDeferredRegister.addToDisplay(BLOCKS, output)));
    }

    private ModMachines() {
    }

    private static void createMachine(DeorumMachineFamily family) {
        String name = family.getName() + "_machine";
        switch (family) {
            case HAMMER -> createBlock(name, family, null, false, TileEntityDeorumHammer::new);
            case SIEVE -> createBlock(name, family, null, false, TileEntityDeorumSieve::new);
            case BARREL -> createBlock(name, family, null, false, TileEntityDeorumBarrel::new);
            case CRUCIBLE -> createBlock(name, family, null, false, TileEntityDeorumCrucible::new);
            case WATER_CRUCIBLE -> createBlock(name, family, null, false, TileEntityDeorumWaterCrucible::new);
        }
    }

    private static void createFactory(DeorumMachineFamily family, MachineTier tier) {
        String name = blockName(tier, family);
        switch (family) {
            case HAMMER -> createBlock(name, family, tier, true, TileEntityDeorumHammer::new);
            case SIEVE -> createBlock(name, family, tier, true, TileEntityDeorumSieve::new);
            case BARREL -> createBlock(name, family, tier, true, TileEntityDeorumBarrel::new);
            case CRUCIBLE -> createBlock(name, family, tier, true, TileEntityDeorumCrucible::new);
            case WATER_CRUCIBLE -> createBlock(name, family, tier, true, TileEntityDeorumWaterCrucible::new);
        }
    }

    private static <T extends TileEntityMekanism> void createBlock(String name, DeorumMachineFamily family,
            MachineTier tier, boolean factory, TileFactory<T> tileFactory) {
        TileTypeRef<T> ref = new TileTypeRef<>();
        long usage = family.getBaseEnergyUsage();
        long storage = DeorumMachineFamily.BASE_ENERGY_STORAGE;

        Machine<T> type = new Machine<>(ref::get, new DeorumLangEntry("description." + name));
        type.add(new AttributeGui(() -> ModContainers.DEORUM_MACHINE, null));
        type.add(new AttributeEnergy(() -> usage, () -> storage));
        type.add(family.isFluidMachine()
                ? AttributeSideConfig.create(TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.FLUID)
                : AttributeSideConfig.ELECTRIC_MACHINE);
        type.add(new AttributeMachineFamily(family));
        type.add(new AttributeMachineFactory(factory));
        if (tier != null) {
            type.add(new AttributeMachineTier(tier));
            FactoryTier factoryTier = resolveFactoryTier(tier);
            if (factoryTier != null) {
                type.add(new AttributeTier<>(factoryTier));
            }
            MachineTier next = tier.next();
            String nextName = next == null ? null : blockName(next, family);
            boolean extrasChain = TierAttributes.applyExtrasUpgrade(type, tier, nextName, BY_NAME);
            boolean emExtrasChain = TierAttributes.applyEmExtrasUpgrade(type, tier, nextName, BY_NAME);
            if (nextName != null && !extrasChain && !emExtrasChain) {
                type.add(new AttributeUpgradeable(() -> BY_NAME.get(nextName)));
            }
        }

        BlockRegistryObject<BlockDeorumMachine<T>, ItemBlockTooltip<BlockDeorumMachine<T>>> block = BLOCKS.register(name,
                () -> new BlockDeorumMachine<>(type),
                (created, properties) -> new ItemBlockTooltip<>(created, true, properties
                        .component(MekanismDataComponents.SIDE_CONFIG, family.isFluidMachine() ? FLUID_MACHINE : AttachedSideConfig.ELECTRIC_MACHINE)
                        .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)));
        ref.value = TILE_TYPES.mekBuilder(block, (pos, state) -> tileFactory.create(block, pos, state))
                .clientTicker(TileEntityMekanism::tickClient)
                .serverTicker(TileEntityMekanism::tickServer)
                .build();
        ALL.add(block);
        BY_NAME.put(name, block);
    }

    private static String blockName(MachineTier tier, DeorumMachineFamily family) {
        return tier.getSerializedName() + "_" + family.getName() + "_factory";
    }

    /**
     * Evolved Mekanism extends Mekanism's {@code FactoryTier} enum, so its extra tiers resolve
     * through the normal name lookup once that mod is installed. Absent addons simply return
     * {@code null}; the block is still registered, it just has no installer tier attribute.
     */
    private static FactoryTier resolveFactoryTier(MachineTier tier) {
        if (tier.isAddonTier() && !isLoaded("evolvedmekanism")) {
            return null;
        }
        try {
            return FactoryTier.valueOf(tier.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isLoaded(String modId) {
        try {
            return ModList.get().isLoaded(modId);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * @return every machine block, in registration order (hammer machine first).
     */
    public static List<BlockRegistryObject<?, ?>> getAll() {
        return List.copyOf(ALL);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        TILE_TYPES.register(modBus);
        ModContainers.register(modBus);
        TABS.register(modBus);
    }

    @FunctionalInterface
    private interface TileFactory<T extends TileEntityMekanism> {
        T create(Holder<Block> block, BlockPos pos, BlockState state);
    }

    private static final class TileTypeRef<T extends TileEntityMekanism> implements Supplier<TileEntityTypeRegistryObject<T>> {

        private TileEntityTypeRegistryObject<T> value;

        @Override
        public TileEntityTypeRegistryObject<T> get() {
            return value;
        }
    }
}
