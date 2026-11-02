package dev.codeonigiri.exmekdeorum.inventory;

import java.util.function.Predicate;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.inventory.slot.InputInventorySlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 入力スロット。並列数に応じてスタック上限を 64 × 並列数 まで引き上げる。
 * Mekanism の {@link InputInventorySlot} は {@code ItemStack} の最大スタック数 (通常 64) で
 * 上限を切ってしまうため、{@link #getLimit(ItemStack)} を上書きして大きな上限を持たせる。
 */
public class DeorumInputSlot extends InputInventorySlot {

    private final int limit;

    protected DeorumInputSlot(Predicate<ItemStack> validator, @Nullable IContentsListener listener, int x, int y, int limit) {
        super(ConstantPredicates.alwaysTrue(), validator, listener, x, y);
        this.limit = Math.max(1, limit);
    }

    public static DeorumInputSlot at(Predicate<ItemStack> validator, @Nullable IContentsListener listener, int x, int y, int limit) {
        return new DeorumInputSlot(validator, listener, x, y, limit);
    }

    @Override
    public int getLimit(ItemStack stack) {
        return limit;
    }
}
