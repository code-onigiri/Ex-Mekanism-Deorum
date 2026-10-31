package dev.exmekdeorum.compat.jei;

import dev.exmekdeorum.machine.DeorumMachineFamily;
import java.util.List;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;

/**
 * JEI-facing bridge. Only loaded when JEI is installed (guarded by {@link DeorumJeiRuntime}).
 */
final class DeorumJeiHook {

    private static IJeiRuntime runtime;

    private DeorumJeiHook() {
    }

    static void setRuntime(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    static void clearRuntime() {
        runtime = null;
    }

    static void open(DeorumMachineFamily family) {
        if (runtime == null) {
            return;
        }
        List<RecipeType<?>> categories = switch (family) {
            case HAMMER -> List.of(DeorumJeiRecipeTypes.HAMMER, DeorumJeiRecipeTypes.COMPRESSED_HAMMER);
            case SIEVE -> List.of(DeorumJeiRecipeTypes.SIEVE);
            case BARREL -> List.of(DeorumJeiRecipeTypes.BARREL_COMPOST, DeorumJeiRecipeTypes.BARREL_MIXING,
                    DeorumJeiRecipeTypes.BARREL_FLUID_MIXING);
            case CRUCIBLE -> List.of(DeorumJeiRecipeTypes.LAVA_CRUCIBLE, DeorumJeiRecipeTypes.CRUCIBLE_HEAT_SOURCES);
            case WATER_CRUCIBLE -> List.of(DeorumJeiRecipeTypes.WATER_CRUCIBLE);
        };
        // Ex Deorum registers some categories with a package-private recipe class (heat sources), so
        // resolve each type by UID first; a type built with a stand-in class must not be passed to JEI.
        List<RecipeType<?>> resolved = new java.util.ArrayList<>(categories.size());
        for (RecipeType<?> candidate : categories) {
            runtime.getRecipeManager().getRecipeType(candidate.getUid()).ifPresent(resolved::add);
        }
        if (!resolved.isEmpty()) {
            runtime.getRecipesGui().showTypes(resolved);
        }
    }
}
