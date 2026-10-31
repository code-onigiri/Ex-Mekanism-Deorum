package dev.codeonigiri.exmekdeorum.machine;

import mekanism.common.block.attribute.Attribute;

/**
 * Block attribute carrying which Ex Deorum family a machine block belongs to.
 */
public record AttributeMachineFamily(DeorumMachineFamily family) implements Attribute {
}
