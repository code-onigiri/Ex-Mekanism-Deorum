package dev.exmekdeorum.registry;

import dev.exmekdeorum.tile.TileEntityDeorumMachine;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Container shared by every Ex Deorum machine.
 *
 * <p>Only the player-inventory offsets are customised: the machine GUI is wider and taller than
 * Mekanism's default 176x166 layout because hammer/sieve expose twenty output slots (the same
 * count as Ex Deorum's mechanical sieve). Slot indices are assigned by each family's
 * {@code initInventory} implementation:</p>
 *
 * <pre>
 *   hammer / sieve   0 input, 1..20 outputs (5x4), 21 energy, 22 auxiliary (hammer / mesh)
 *   barrel           0 input A, 1 input B, 2 output, 3 energy
 *   crucible         0 input, 1 energy
 * </pre>
 */
public class DeorumMachineContainer extends MekanismTileContainer<TileEntityDeorumMachine> {

    /** Horizontal offset of the 9x3 player inventory inside the 200 pixel wide GUI. */
    public static final int INVENTORY_X = 19;
    /** Vertical offset of the player inventory; the hotbar follows 58 pixels below. */
    public static final int INVENTORY_Y = 104;

    public DeorumMachineContainer(int id, @NotNull Inventory inv, @NotNull TileEntityDeorumMachine tile) {
        super(ModContainers.DEORUM_MACHINE, id, inv, tile);
    }

    @Override
    protected int getInventoryXOffset() {
        return INVENTORY_X;
    }

    @Override
    protected int getInventoryYOffset() {
        return INVENTORY_Y;
    }
}
