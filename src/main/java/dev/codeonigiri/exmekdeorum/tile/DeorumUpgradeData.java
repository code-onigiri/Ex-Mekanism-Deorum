package dev.codeonigiri.exmekdeorum.tile;

import mekanism.common.upgrade.IUpgradeData;
import net.minecraft.nbt.CompoundTag;

/**
 * Upgrade payload for {@link TileEntityDeorumMachine}.
 *
 * <p>Mekanism's tier installers (Mekanism's own, Evolved Mekanism's and Mekanism Extras' items)
 * replace the block and then call {@code parseUpgradeData}; we keep the whole machine NBT so the
 * inventory, side configuration and progress survive the upgrade.</p>
 */
public record DeorumUpgradeData(CompoundTag tag) implements IUpgradeData {
}
