package dev.codeonigiri.exmekdeorum.compat.jei;

import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.crafting.Recipe;
import thedarkcolour.exdeorum.compat.XeiSieveRecipe;
import thedarkcolour.exdeorum.recipe.barrel.BarrelCompostRecipe;
import thedarkcolour.exdeorum.recipe.barrel.BarrelFluidMixingRecipe;
import thedarkcolour.exdeorum.recipe.barrel.BarrelMixingRecipe;
import thedarkcolour.exdeorum.recipe.crucible.CrucibleRecipe;
import thedarkcolour.exdeorum.recipe.hammer.HammerRecipe;

/**
 * The JEI recipe types exposed by Ex Deorum, created with the same namespace it uses.
 *
 * <p>The 1.20.1 Ex Deorum line always registers its JEI categories under {@code exdeorum};
 * unlike the 1.21 line it does not switch to an {@code exdeorum_emi} namespace when EMI is
 * installed.</p>
 */
public final class DeorumJeiRecipeTypes {

    private static final String EXDEORUM = "exdeorum";

    public static final RecipeType<HammerRecipe> HAMMER = create("hammer", HammerRecipe.class);
    public static final RecipeType<HammerRecipe> COMPRESSED_HAMMER = create("compressed_hammer", HammerRecipe.class);
    public static final RecipeType<XeiSieveRecipe> SIEVE = create("sieve", XeiSieveRecipe.class);
    public static final RecipeType<XeiSieveRecipe> COMPRESSED_SIEVE = create("compressed_sieve", XeiSieveRecipe.class);
    public static final RecipeType<BarrelCompostRecipe> BARREL_COMPOST = create("barrel_compost", BarrelCompostRecipe.class);
    public static final RecipeType<BarrelMixingRecipe> BARREL_MIXING = create("barrel_mixing", BarrelMixingRecipe.class);
    public static final RecipeType<BarrelFluidMixingRecipe> BARREL_FLUID_MIXING = create("barrel_fluid_mixing", BarrelFluidMixingRecipe.class);
    public static final RecipeType<CrucibleRecipe> LAVA_CRUCIBLE = create("lava_crucible", CrucibleRecipe.class);
    public static final RecipeType<CrucibleRecipe> WATER_CRUCIBLE = create("water_crucible", CrucibleRecipe.class);
    // The heat-source page uses a package-private recipe class in Ex Deorum; only the UID matters
    // for recipe lookups, so Recipe.class is a safe stand-in.
    public static final RecipeType<Recipe> CRUCIBLE_HEAT_SOURCES = create("crucible_heat_sources", Recipe.class);

    private DeorumJeiRecipeTypes() {
    }

    private static <T> RecipeType<T> create(String path, Class<? extends T> recipeClass) {
        return RecipeType.create(EXDEORUM, path, recipeClass);
    }
}
