package dev.codeonigiri.exmekdeorum.block;

import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

/**
 * Shared block class for all Ex Deorum machines.
 *
 * <p>{@link BlockTile} already handles the GUI opening, wrench behaviour, active light and the
 * facing / active block state properties through the attributes added by
 * {@link mekanism.common.content.blocktype.Machine}. Block states are therefore
 * {@code facing} (6 values) × {@code active} (2 values).</p>
 */
public class BlockDeorumMachine<TILE extends TileEntityMekanism> extends BlockTile<TILE, BlockTypeTile<TILE>> {

    public BlockDeorumMachine(BlockTypeTile<TILE> type) {
        super(type, (Properties properties) -> properties);
    }
}
