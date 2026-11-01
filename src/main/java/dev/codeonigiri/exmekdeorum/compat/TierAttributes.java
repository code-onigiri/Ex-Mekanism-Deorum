package dev.codeonigiri.exmekdeorum.compat;

import dev.codeonigiri.exmekdeorum.machine.MachineTier;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.Supplier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.content.blocktype.BlockType;
import mekanism.common.registration.impl.BlockRegistryObject;
import net.neoforged.fml.ModList;

/**
 * Adds optional addon tier attributes without compiling against the addons.
 *
 * <p>Evolved Mekanism extends Mekanism's {@code FactoryTier} enum through a mixin, so its tiers
 * are reached through the normal {@code AttributeTier<FactoryTier>} path in
 * {@code ModMachines}. Mekanism Extras has its own tier enum and its own
 * {@code ExtraAttributeTier}/{@code ExtraAttributeUpgradeable} classes, so those are constructed
 * reflectively here. Everything is a no-op when the corresponding mod is absent.</p>
 */
public final class TierAttributes {

    private static final String EXTRAS_TIER = "com.jerry.mekextras.api.tier.AdvancedTier";
    private static final String EXTRAS_I_TIER = "com.jerry.mekextras.api.tier.IAdvancedTier";
    private static final String EXTRAS_TIER_ATTRIBUTE = "com.jerry.mekextras.common.block.attribute.ExtraAttributeTier";
    private static final String EXTRAS_UPGRADEABLE = "com.jerry.mekextras.common.block.attribute.ExtraAttributeUpgradeable";

    private static Boolean extrasPresent;

    private TierAttributes() {
    }

    /**
     * @return {@code true} when this tier's upgrade path was registered through Mekanism Extras,
     *         so the caller must not also add the Mekanism {@code AttributeUpgradeable}.
     */
    public static boolean applyExtrasUpgrade(BlockType type, MachineTier tier, String nextName,
            Map<String, BlockRegistryObject<?, ?>> byName) {
        if (!extrasPresent()) {
            return false;
        }
        try {
            Class<?> tierClass = Class.forName(EXTRAS_TIER);
            Object advanced;
            try {
                advanced = Enum.valueOf((Class) tierClass, tier.name());
            } catch (IllegalArgumentException e) {
                return false;
            }
            Class<?> tierInterface = Class.forName(EXTRAS_I_TIER);
            Class<?> attributeClass = Class.forName(EXTRAS_TIER_ATTRIBUTE);
            Constructor<?> ctor = attributeClass.getConstructor(tierInterface);
            add(type, ctor.newInstance(advanced));
            if (nextName != null) {
                Class<?> upgradeableClass = Class.forName(EXTRAS_UPGRADEABLE);
                Supplier<BlockRegistryObject<?, ?>> supplier = () -> byName.get(nextName);
                Object attribute = upgradeableClass.getConstructor(Supplier.class).newInstance(supplier);
                add(type, attribute);
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }

    private static void add(BlockType type, Object attribute) {
        type.add((Attribute) attribute);
    }

    private static boolean extrasPresent() {
        if (extrasPresent == null) {
            try {
                extrasPresent = ModList.get().isLoaded("mekanism_extras");
            } catch (RuntimeException e) {
                extrasPresent = false;
            }
            if (!extrasPresent) {
                try {
                    Class.forName(EXTRAS_TIER);
                    extrasPresent = true;
                } catch (ClassNotFoundException | LinkageError e) {
                    extrasPresent = false;
                }
            }
        }
        return extrasPresent;
    }
}
