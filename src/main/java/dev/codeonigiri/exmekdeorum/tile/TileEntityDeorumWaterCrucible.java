package dev.codeonigiri.exmekdeorum.tile;

import mekanism.api.providers.IBlockProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.exdeorum.recipe.RecipeUtil;
import thedarkcolour.exdeorum.recipe.crucible.CrucibleRecipe;

/**
 * Machine version of Ex Deorum's water crucible.
 *
 * <p>Only {@code WaterCrucibleRecipe}s are accepted; items melt into water without a heat source at
 * a fixed conversion rate of 4 per 10 ticks. Factories increase the melting/conversion rate by
 * 5x / 10x / 20x / 30x and spend the same multiple of FE/t; see
 * {@link TileEntityDeorumCrucibleBase}.</p>
 */
public class TileEntityDeorumWaterCrucible extends TileEntityDeorumCrucibleBase {

    public TileEntityDeorumWaterCrucible(IBlockProvider blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
    }

    @Override
    protected boolean isLava() {
        return false;
    }

    @Nullable
    @Override
    protected CrucibleRecipe recipeFor(ItemStack stack) {
        return RecipeUtil.getWaterCrucibleRecipe(stack);
    }
}
