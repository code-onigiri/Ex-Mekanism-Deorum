package dev.codeonigiri.exmekdeorum.machine;

import mekanism.common.block.attribute.Attribute;

/**
 * Block attribute telling whether a machine is the factory (parallel processing) variant.
 */
public record AttributeMachineFactory(boolean factory) implements Attribute {
}
