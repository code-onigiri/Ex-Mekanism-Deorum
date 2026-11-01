package dev.codeonigiri.exmekdeorum.machine;

import java.util.Locale;

/**
 * Factory tiers, following Mekanism's factory convention.
 *
 * <p>The plain machines are untiered. Only factories carry a tier, and the tier only changes how
 * many items the factory processes in parallel (Mekanism uses 3/5/7/9 for Basic/Advanced/Elite/
 * Ultimate). Per-operation time and per-operation energy cost are the same for every tier, so a
 * factory is exactly {@code processes} machines in one block.</p>
 *
 * <p>Tweak {@link #getFactoryProcesses()} here if you want a different balance.</p>
 */
public enum MachineTier {
    BASIC(3, 5),
    ADVANCED(5, 10),
    ELITE(7, 20),
    ULTIMATE(9, 30);

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
     * fluid stays the same. Basic / Advanced / Elite / Ultimate are 5x / 10x / 20x / 30x.</p>
     */
    public int getMeltMultiplier() {
        return meltMultiplier;
    }

    /**
     * @return the next tier, or {@code null} for {@link #ULTIMATE}.
     */
    public MachineTier next() {
        return switch (this) {
            case BASIC -> ADVANCED;
            case ADVANCED -> ELITE;
            case ELITE -> ULTIMATE;
            case ULTIMATE -> null;
        };
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
