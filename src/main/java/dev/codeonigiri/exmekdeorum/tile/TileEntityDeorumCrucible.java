package dev.codeonigiri.exmekdeorum.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.exdeorum.recipe.RecipeCaches;
import thedarkcolour.exdeorum.recipe.crucible.CrucibleRecipe;

/**
 * Machine version of Ex Deorum's lava crucible.
 *
 * <p>Only {@code LavaCrucibleRecipe}s are accepted. The machine needs a heat source block below it
 * (the same blocks Ex Deorum accepts); the heat value is the conversion rate per 10 ticks. Without
 * a heat source nothing melts and the item stays in the input slot, so no energy is wasted.</p>
 *
 * <p>Factories increase the melting/conversion rate by 5x / 10x / 20x / 30x and spend the same
 * multiple of FE/t; see {@link TileEntityDeorumCrucibleBase}.</p>
 */
public class TileEntityDeorumCrucible extends TileEntityDeorumCrucibleBase {

    public TileEntityDeorumCrucible(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
    }

    @Override
    protected boolean isLava() {
        return true;
    }

    @Nullable
    @Override
    protected CrucibleRecipe recipeFor(RecipeCaches caches, ItemStack stack) {
        return caches.getLavaCrucibleRecipe(stack);
    }
}
