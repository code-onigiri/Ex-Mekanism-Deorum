package dev.codeonigiri.exmekdeorum.machine;

import java.util.Locale;
import net.minecraftforge.fml.ModList;

/**
 * Factory tiers.
 *
 * <p>The first four tiers mirror Mekanism's factories (4/8/12/16 parallel operations). The
 * remaining constants mirror the tiers added by the optional addons
 * <a href="https://www.curseforge.com/minecraft/mc-mods/evolved-mekanism">Evolved Mekanism</a>
 * (Overclocked..Creative) and
 * <a href="https://www.curseforge.com/minecraft/mc-mods/mekanism-extras">Mekanism Extras</a>
 * (Absolute..Infinite) and
 * <a href="https://www.curseforge.com/minecraft/mc-mods/evolved-mekanism-extras">Evolved Mekanism
 * Extras</a> (Absolute Overclocked..Infinite Multiversal). Addon tiers are only registered when
 * the corresponding addon is installed, and their recipes carry a {@code neoforge:mod_loaded}
 * condition, so an addon-less game sees only the 25 base blocks.</p>
 *
 * <p>Per-operation time and energy are the same for every tier, so a factory is exactly
 * {@code processes} machines in one block. Crucible factories use the melt multiplier instead of
 * parallelism.</p>
 */
public enum MachineTier {
    // Mekanism's own ladder.
    BASIC(4, 5),
    ADVANCED(8, 10),
    ELITE(12, 20),
    ULTIMATE(16, 30),
    // Evolved Mekanism: 11 / 13 / 15 / 17 / 19 parallel operations.
    OVERCLOCKED(20, 40),
    QUANTUM(24, 50),
    DENSE(28, 60),
    MULTIVERSAL(32, 70),
    CREATIVE(36, 80),
    // Mekanism Extras: 11 / 13 / 15 / 17 parallel operations.
    ABSOLUTE(20, 40),
    SUPREME(24, 50),
    COSMIC(28, 60),
    INFINITE(32, 70),
    // Evolved Mekanism Extras: combined Evolved + Extras tiers (12 / 14 / 16 / 18 parallel).
    ABSOLUTE_OVERCLOCKED(40, 90),
    SUPREME_QUANTUM(48, 100),
    COSMIC_DENSE(56, 110),
    INFINITE_MULTIVERSAL(64, 120);

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
     * @return whether this tier belongs to the Evolved Mekanism ladder.
     */
    public boolean isEvolvedTier() {
        return ordinal() >= OVERCLOCKED.ordinal() && ordinal() <= CREATIVE.ordinal();
    }

    /**
     * @return whether this tier belongs to the Mekanism Extras ladder.
     */
    public boolean isExtrasTier() {
        return ordinal() >= ABSOLUTE.ordinal() && ordinal() <= INFINITE.ordinal();
    }

    /**
     * @return whether this tier belongs to the Evolved Mekanism Extras ladder.
     */
    public boolean isEmExtrasTier() {
        return ordinal() >= ABSOLUTE_OVERCLOCKED.ordinal();
    }

    /**
     * @return whether the addon that provides this tier is installed. Base tiers are always
     *         available; addon tiers are only registered when their addon is present, so the
     *         blocks do not show up in an addon-less world.
     */
    public boolean isAvailable() {
        if (!isAddonTier()) {
            return true;
        }
        return isModLoaded(requiredModId());
    }

    /**
     * @return the mod id that provides this tier ({@code null} for base tiers).
     */
    public String requiredModId() {
        if (isEvolvedTier()) {
            return "evolvedmekanism";
        }
        if (isExtrasTier()) {
            return "mekanism_extras";
        }
        if (isEmExtrasTier()) {
            return "emextras";
        }
        return null;
    }

    /**
     * @return the next tier in the upgrade ladder, or {@code null} when no further tier exists.
     *
     * <p>After Ultimate the ladder continues into whichever addon is installed; when both are
     * installed Evolved Mekanism's ladder takes priority (both addons add tiers with the same
     * parallel counts, so chaining one ladder keeps the installer path unambiguous).</p>
     */
    public MachineTier next() {
        MachineTier next = switch (this) {
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
        return next != null && next.isAvailable() ? next : null;
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
