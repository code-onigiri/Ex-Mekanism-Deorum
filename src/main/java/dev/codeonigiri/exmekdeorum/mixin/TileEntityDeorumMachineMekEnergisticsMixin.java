package dev.codeonigiri.exmekdeorum.mixin;

import com.beipuo.mekenergistics.api.upgrade.IMePatternAutomationHost;
import dev.codeonigiri.exmekdeorum.tile.TileEntityDeorumMachine;
import java.util.List;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.inventory.IInventorySlot;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Exposes the machines to Mek Energistics' ME Pattern Automation SPI.
 *
 * <p>Only merged when {@link DeorumMixinPlugin} sees the Mek Energistics API on the classpath;
 * the methods simply forward to {@link TileEntityDeorumMachine}'s automation accessors. Item
 * slots the machine treats as tools (hammer/mesh) are reported as persistent inputs so the
 * pattern provider keeps them stocked.</p>
 */
@Mixin(TileEntityDeorumMachine.class)
public abstract class TileEntityDeorumMachineMekEnergisticsMixin implements IMePatternAutomationHost {

    private TileEntityDeorumMachine deorum() {
        return (TileEntityDeorumMachine) (Object) this;
    }

    @Override
    public boolean meSupportsPatternAutomation() {
        return true;
    }

    @Override
    public List<IInventorySlot> mePatternItemInputs() {
        return deorum().getAutomationItemInputs();
    }

    @Override
    public List<IInventorySlot> mePatternItemOutputs() {
        return deorum().getAutomationItemOutputs();
    }

    @Override
    public List<IExtendedFluidTank> mePatternFluidInputs() {
        return deorum().getAutomationFluidInputs();
    }

    @Override
    public List<IExtendedFluidTank> mePatternFluidOutputs() {
        return deorum().getAutomationFluidOutputs();
    }

    @Override
    public List<IInventorySlot> mePersistentItemInputs() {
        IInventorySlot aux = deorum().getAutomationAuxSlot();
        return aux == null ? List.of() : List.of(aux);
    }

    @Override
    public IEnergyContainer meEnergyContainer() {
        return deorum().getAutomationEnergyContainer();
    }

    @Override
    public boolean meGroupParallelItemInputs() {
        return deorum().hasParallelOperations();
    }
}
