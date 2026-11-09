package dev.codeonigiri.exmekdeorum.registry;

import dev.codeonigiri.exmekdeorum.ExMekanismDeorum;
import dev.codeonigiri.exmekdeorum.block.BlockDeorumMachine;
import dev.codeonigiri.exmekdeorum.compat.TierAttributes;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import mekanism.api.Upgrade;
import mekanism.api.math.FloatingLong;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.block.attribute.AttributeEnergy;
import mekanism.common.block.attribute.AttributeGui;
import mekanism.common.block.attribute.AttributeParticleFX;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.attribute.AttributeUpgradeSupport;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.content.blocktype.BlockType;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.lib.math.Pos3D;
import mekanism.common.registration.impl.BlockDeferredRegister;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.registration.impl.CreativeTabDeferredRegister;
import mekanism.common.registration.impl.CreativeTabRegistryObject;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

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
 * {@code TileEntityTypeDeferredRegister} builds a type that contains exactly one block.
 * The tile class is shared per family; the family / factory / tier data is carried by block
 * attributes and read by the tile in {@code presetVariables()}.</p>
 *
 * <p>Mekanism 10.4 (1.20.1) has no item data components: side configuration and the ejector
 * live in the tile components and are persisted in tile NBT. The tile therefore installs its
 * own default side configuration when it is constructed.</p>
 */
public final class ModMachines {

    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(ExMekanismDeorum.MOD_ID);
    public static final TileEntityTypeDeferredRegister TILE_TYPES = new TileEntityTypeDeferredRegister(ExMekanismDeorum.MOD_ID);
    public static final CreativeTabDeferredRegister TABS = new CreativeTabDeferredRegister(ExMekanismDeorum.MOD_ID);

    private static final List<BlockRegistryObject<?, ?>> ALL = new ArrayList<>();
    private static final Map<String, BlockRegistryObject<?, ?>> BY_NAME = new HashMap<>();
    public static final CreativeTabRegistryObject TAB;

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
        TAB = TABS.registerMain(new DeorumLangEntry("itemGroup.exmekdeorum"), ALL.get(0),
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
        // ファクトリーは 1 tick の消費が増える分だけ内部バッファも同じ比率で増やす
        // (バッファで動ける時間を無印と揃える)。ティアが上がるほど貯蓄も増える。
        // 消費の倍率は通常ファミリーでは並列数だが、るつぼ系は並列レーンではなく
        // 溶解倍率で消費が増えるため、そちらに合わせる。
        int processes = factory && tier != null ? tier.getFactoryProcesses() : 1;
        int scale = factory && tier != null && (family == DeorumMachineFamily.CRUCIBLE || family == DeorumMachineFamily.WATER_CRUCIBLE)
                ? tier.getMeltMultiplier() : processes;
        long storage = Math.multiplyExact(DeorumMachineFamily.BASE_ENERGY_STORAGE, scale);

        BlockTypeTile<T> type = new BlockTypeTile<>(ref::get, new DeorumLangEntry("description." + name));
        // These are the attributes Mekanism's own Machine block type adds.
        type.add(new AttributeParticleFX()
                        .add(ParticleTypes.SMOKE, rand -> new Pos3D(rand.nextFloat() * 0.6F - 0.3F, rand.nextFloat() * 6.0F / 16.0F, 0.52))
                        .add(DustParticleOptions.REDSTONE, rand -> new Pos3D(rand.nextFloat() * 0.6F - 0.3F, rand.nextFloat() * 6.0F / 16.0F, 0.52)),
                Attributes.ACTIVE_LIGHT, new AttributeStateFacing(), Attributes.INVENTORY,
                Attributes.SECURITY, Attributes.REDSTONE, Attributes.COMPARATOR,
                new AttributeUpgradeSupport(EnumSet.of(Upgrade.SPEED, Upgrade.ENERGY, Upgrade.MUFFLING)));
        type.add(new AttributeGui(() -> ModContainers.DEORUM_MACHINE, null));
        type.add(new AttributeEnergy(() -> FloatingLong.create(usage), () -> FloatingLong.create(storage)));
        type.add(new AttributeMachineFamily(family));
        applyEmpoweredUpgrades(type);
        applyExtrasUpgrades(type);
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

        BlockRegistryObject<BlockDeorumMachine<T>, ItemBlockTooltip<BlockDeorumMachine<T>>> block = BLOCKS.registerDefaultProperties(name,
                () -> new BlockDeorumMachine<>(type),
                (created, properties) -> new ItemBlockTooltip<BlockDeorumMachine<T>>(created, properties));
        ref.value = TILE_TYPES.register(block, (pos, state) -> tileFactory.create(block, pos, state),
                TileEntityMekanism::tickServer, TileEntityMekanism::tickClient);
        ALL.add(block);
        BY_NAME.put(name, block);
    }

    /**
     * Mekanism: Empowered の追加アップグレード (empowered_speed など) は、同 MOD が
     * Mekanism 標準のブロックタイプにだけ登録する。ここで自前のブロックタイプにも
     * 同じ登録を行い、Empowered 製アップグレードを受け付けられるようにする。
     * MOD が無いときは何もしない (リフレクションのみで依存しない)。
     */
    private static void applyEmpoweredUpgrades(BlockType type) {
        if (!ModList.get().isLoaded("mekanism_empowered")) {
            return;
        }
        try {
            Class<?> api = Class.forName("dev.lapis256.mekanism_empowered.api.MekEmpUpgrade");
            Class<?> util = Class.forName("dev.lapis256.mekanism_empowered.core.common.util.AdditionalUpgradeUtil");
            List<Upgrade> upgrades = new ArrayList<>();
            for (String getter : new String[]{"getEMPOWERED_SPEED", "getEMPOWERED_ENERGY", "getIO_CAPACITY",
                    "getFAST_ITEM_INSERT", "getFAST_ITEM_EJECT", "getAUTO_INSERTER"}) {
                try {
                    upgrades.add((Upgrade) api.getMethod(getter).invoke(null));
                } catch (NoSuchMethodException ignored) {
                    // Older Empowered builds may not have every upgrade.
                }
            }
            if (upgrades.isEmpty()) {
                return;
            }
            util.getMethod("addSupported", BlockType.class, Upgrade[].class)
                    .invoke(null, type, upgrades.toArray(new Upgrade[0]));
        } catch (ReflectiveOperationException e) {
            ExMekanismDeorum.LOGGER.warn("Failed to add Mekanism: Empowered upgrade support", e);
        }
    }

    /**
     * Mekanism Extras の追加アップグレード (STACK / CREATIVE) を自前のブロックタイプでも
     * 受け付けられるようにする。Empowered 側の登録が先に走っている場合はその集合に追加する。
     */
    private static void applyExtrasUpgrades(BlockType type) {
        if (!ModList.get().isLoaded("mekanism_extras")) {
            return;
        }
        try {
            Class<?> api = Class.forName("com.jerry.mekextras.api.ExtraUpgrade");
            Set<Upgrade> upgrades = new LinkedHashSet<>();
            AttributeUpgradeSupport current = type.get(AttributeUpgradeSupport.class);
            if (current != null) {
                upgrades.addAll(current.supportedUpgrades());
            }
            for (String fieldName : new String[]{"STACK", "CREATIVE"}) {
                upgrades.add((Upgrade) api.getField(fieldName).get(null));
            }
            type.add(new AttributeUpgradeSupport(upgrades));
        } catch (ReflectiveOperationException e) {
            ExMekanismDeorum.LOGGER.warn("Failed to add Mekanism Extras upgrade support", e);
        }
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
     * 登録済みブロックの ID。DeferredRegister のバインド前 (Mod 構築中) でも使える。
     */
    public static List<ResourceLocation> getBlockIds() {
        return BY_NAME.keySet().stream()
                .map(name -> new ResourceLocation(ExMekanismDeorum.MOD_ID, name))
                .toList();
    }

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
        T create(IBlockProvider blockProvider, BlockPos pos, BlockState state);
    }

    private static final class TileTypeRef<T extends TileEntityMekanism> implements Supplier<TileEntityTypeRegistryObject<T>> {

        private TileEntityTypeRegistryObject<T> value;

        @Override
        public TileEntityTypeRegistryObject<T> get() {
            return value;
        }
    }
}
