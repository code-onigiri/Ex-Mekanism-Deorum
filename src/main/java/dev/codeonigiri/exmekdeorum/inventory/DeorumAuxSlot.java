package dev.codeonigiri.exmekdeorum.inventory;

import java.util.function.Predicate;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Auxiliary (tool / catalyst) slot with a fixed stack limit of one.
 *
 * <p>Mekanism 10.4's {@code BasicInventorySlot.at(...)} does not take a limit, so this subclass
 * reaches the protected constructor to pin the limit.</p>
 */
public class DeorumAuxSlot extends BasicInventorySlot {

    private DeorumAuxSlot(Predicate<ItemStack> validator, @Nullable IContentsListener listener, int x, int y) {
        super(1, ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrueBi(), validator, listener, x, y);
    }

    public static DeorumAuxSlot at(Predicate<ItemStack> validator, @Nullable IContentsListener listener, int x, int y) {
        return new DeorumAuxSlot(validator, listener, x, y);
    }
}
