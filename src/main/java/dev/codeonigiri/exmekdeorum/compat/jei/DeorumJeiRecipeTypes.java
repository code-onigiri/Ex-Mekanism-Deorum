package dev.codeonigiri.exmekdeorum.compat.jei;

import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.fml.ModList;
import thedarkcolour.exdeorum.compat.XeiSieveRecipe;
import thedarkcolour.exdeorum.recipe.barrel.BarrelCompostRecipe;
import thedarkcolour.exdeorum.recipe.barrel.BarrelFluidMixingRecipe;
import thedarkcolour.exdeorum.recipe.barrel.BarrelMixingRecipe;
import thedarkcolour.exdeorum.recipe.crucible.CrucibleRecipe;
import thedarkcolour.exdeorum.recipe.hammer.CompressedHammerRecipe;
import thedarkcolour.exdeorum.recipe.hammer.HammerRecipe;

/**
 * The JEI recipe types exposed by Ex Deorum, created with the same namespace it uses.
 *
 * <p>Ex Deorum switches its category namespace to {@code exdeorum_emi} when EMI is installed, so
 * every type has to be built lazily-ish (at class initialisation) with the same rule.</p>
 */
public final class DeorumJeiRecipeTypes {

    private static final String EMI = "emi";
    private static final String EXDEORUM = "exdeorum";
    private static final String EXDEORUM_EMI = "exdeorum_emi";

    public static final RecipeType<HammerRecipe> HAMMER = create("hammer", HammerRecipe.class);
    public static final RecipeType<CompressedHammerRecipe> COMPRESSED_HAMMER = create("compressed_hammer", CompressedHammerRecipe.class);
    public static final RecipeType<XeiSieveRecipe> SIEVE = create("sieve", XeiSieveRecipe.class);
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
        String namespace = ModList.get().isLoaded(EMI) ? EXDEORUM_EMI : EXDEORUM;
        return RecipeType.create(namespace, path, recipeClass);
    }
}
