package dev.codeonigiri.exmekdeorum.machine;

/**
 * The four Ex Deorum processing families that are exposed as Mekanism machines.
 *
 * <p>{@code baseEnergyUsage} is the amount of FE/t consumed while the machine is working at
 * BASIC tier. Barrel and crucible are fluid machines: their block type additionally supports the
 * FLUID transmission type for side configuration and they own a fluid tank.</p>
 */
public enum DeorumMachineFamily {
    HAMMER("hammer", false, 20L),
    SIEVE("sieve", false, 40L),
    BARREL("barrel", true, 10L),
    CRUCIBLE("crucible", true, 20L),
    WATER_CRUCIBLE("water_crucible", true, 20L);

    /**
     * @return true for the lava crucible, which needs a heat source below the machine.
     */
    public boolean isLavaCrucible() {
        return this == CRUCIBLE;
    }

    public static final long BASE_ENERGY_STORAGE = 40_000L;

    private final String name;
    private final boolean fluidMachine;
    private final long baseEnergyUsage;

    DeorumMachineFamily(String name, boolean fluidMachine, long baseEnergyUsage) {
        this.name = name;
        this.fluidMachine = fluidMachine;
        this.baseEnergyUsage = baseEnergyUsage;
    }

    public String getName() {
        return name;
    }

    /**
     * @return true for machines that own a fluid tank (barrel, crucible).
     */
    public boolean isFluidMachine() {
        return fluidMachine;
    }

    public long getBaseEnergyUsage() {
        return baseEnergyUsage;
    }
}
