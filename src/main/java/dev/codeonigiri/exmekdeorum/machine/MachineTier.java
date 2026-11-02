package dev.codeonigiri.exmekdeorum.machine;

import java.util.Locale;
import net.neoforged.fml.ModList;

/**
 * Factory tiers.
 *
 * <p>The first four tiers mirror Mekanism's factories (3/5/7/9 parallel operations). The
 * remaining constants mirror the tiers added by the optional addons
 * <a href="https://www.curseforge.com/minecraft/mc-mods/evolved-mekanism">Evolved Mekanism</a>
 * (Overclocked..Creative) and
 * <a href="https://www.curseforge.com/minecraft/mc-mods/mekanism-extras">Mekanism Extras</a>
 * (Absolute..Infinite). They are always registered so the blocks exist even without the addon;
 * the addon's tier installer can only reach them when it is installed, because the block then
 * also carries the addon's tier attribute.</p>
 *
 * <p>Per-operation time and energy are the same for every tier, so a factory is exactly
 * {@code processes} machines in one block. Crucible factories use the melt multiplier instead of
 * parallelism.</p>
 */
public enum MachineTier {
    // Mekanism's own ladder.
    BASIC(3, 5),
    ADVANCED(5, 10),
    ELITE(7, 20),
    ULTIMATE(9, 30),
    // Evolved Mekanism: 11 / 13 / 15 / 17 / 19 parallel operations.
    OVERCLOCKED(11, 40),
    QUANTUM(13, 50),
    DENSE(15, 60),
    MULTIVERSAL(17, 70),
    CREATIVE(19, 80),
    // Mekanism Extras: 11 / 13 / 15 / 17 parallel operations.
    ABSOLUTE(11, 40),
    SUPREME(13, 50),
    COSMIC(15, 60),
    INFINITE(17, 70),
    // Evolved Mekanism Extras: combined Evolved + Extras tiers (12 / 14 / 16 / 18 parallel).
    ABSOLUTE_OVERCLOCKED(12, 90),
    SUPREME_QUANTUM(14, 100),
    COSMIC_DENSE(16, 110),
    INFINITE_MULTIVERSAL(18, 120);

    private final int factoryProcesses;
    private final int meltMultiplier;

    MachineTier(int factoryProcesses, int meltMultiplier) {
        this.factoryProcesses = factoryProcesses;
        this.meltMultiplier = meltMultiplier;
    }

    public int getFactoryProcesses() {
        return factoryProcesses;
    }

    /**
     * Speed multiplier used by the crucible families, which have no parallel item outputs.
     *
     * <p>A crucible factory melts {@code meltMultiplier} times as much per operation and also
     * spends {@code meltMultiplier} times as much energy per tick, so the energy cost per unit of
     * fluid stays the same. The base ladder is 5x / 10x / 20x / 30x; addon tiers continue at
     * 40x / 50x / 60x / 70x / 80x.</p>
     */
    public int getMeltMultiplier() {
        return meltMultiplier;
    }

    /**
     * @return whether this tier comes from an optional addon rather than Mekanism itself.
     */
    public boolean isAddonTier() {
        return this.ordinal() > ULTIMATE.ordinal();
    }

    /**
     * @return the next tier in the upgrade ladder, or {@code null} when no further tier exists.
     *
     * <p>After Ultimate the ladder continues into whichever addon is installed; when both are
     * installed Evolved Mekanism's ladder takes priority (both addons add tiers with the same
     * parallel counts, so chaining one ladder keeps the installer path unambiguous).</p>
     */
    public MachineTier next() {
        return switch (this) {
            case BASIC -> ADVANCED;
            case ADVANCED -> ELITE;
            case ELITE -> ULTIMATE;
            case ULTIMATE -> firstInstalledAddonTier();
            case OVERCLOCKED -> QUANTUM;
            case QUANTUM -> DENSE;
            case DENSE -> MULTIVERSAL;
            case MULTIVERSAL -> CREATIVE;
            case CREATIVE -> null;
            case ABSOLUTE -> SUPREME;
            case SUPREME -> COSMIC;
            case COSMIC -> INFINITE;
            case INFINITE -> null;
            case ABSOLUTE_OVERCLOCKED -> SUPREME_QUANTUM;
            case SUPREME_QUANTUM -> COSMIC_DENSE;
            case COSMIC_DENSE -> INFINITE_MULTIVERSAL;
            case INFINITE_MULTIVERSAL -> null;
        };
    }

    private static MachineTier firstInstalledAddonTier() {
        if (isModLoaded("evolvedmekanism")) {
            return OVERCLOCKED;
        }
        if (isModLoaded("mekanism_extras")) {
            return ABSOLUTE;
        }
        return null;
    }

    private static boolean isModLoaded(String modId) {
        try {
            return ModList.get().isLoaded(modId);
        } catch (RuntimeException e) {
            // ModList is not ready (e.g. unit tests) - treat as absent.
            return false;
        }
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
