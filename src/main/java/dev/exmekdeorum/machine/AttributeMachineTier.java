package dev.exmekdeorum.machine;

import mekanism.common.block.attribute.Attribute;

/**
 * Block attribute carrying the Mekanism-style tier of an Ex Deorum machine.
 *
 * <p>Kept as a custom attribute so the tile can read it in {@code presetVariables()} before the
 * inventory / energy holders are built.</p>
 */
public record AttributeMachineTier(MachineTier tier) implements Attribute {
}
