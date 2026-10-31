package dev.exmekdeorum.registry;

import dev.exmekdeorum.ExMekanismDeorum;
import dev.exmekdeorum.tile.TileEntityDeorumMachine;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import net.neoforged.bus.api.IEventBus;

/**
 * Containers for the Ex Deorum machines.
 *
 * <p>A single container type is registered for the shared tile base class
 * {@link TileEntityDeorumMachine}; Mekanism resolves the tile by class, so every family and tier
 * reuses it. The container subclass supplies the player-inventory offsets of the larger GUI. The
 * screen is likewise shared (see {@code GuiDeorumMachine}).</p>
 */
public final class ModContainers {

    public static final ContainerTypeDeferredRegister CONTAINERS = new ContainerTypeDeferredRegister(ExMekanismDeorum.MOD_ID);

    public static final ContainerTypeRegistryObject<DeorumMachineContainer> DEORUM_MACHINE =
            CONTAINERS.register("deorum_machine", TileEntityDeorumMachine.class,
                    DeorumMachineContainer::new);

    private ModContainers() {
    }

    public static void register(IEventBus modBus) {
        CONTAINERS.register(modBus);
    }
}
