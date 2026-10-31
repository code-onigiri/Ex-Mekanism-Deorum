package dev.exmekdeorum.compat.jei;

import dev.exmekdeorum.machine.DeorumMachineFamily;
import net.neoforged.fml.ModList;

/**
 * Entry point used by the GUI (progress bar click) to open the matching JEI recipe page.
 *
 * <p>This class deliberately references no JEI types: it is safe to load when JEI is absent. The
 * JEI-facing code lives in {@link DeorumJeiHook}, which is only touched after the {@link ModList}
 * check succeeds.</p>
 */
public final class DeorumJeiRuntime {

    private static final String JEI = "jei";

    private DeorumJeiRuntime() {
    }

    /** Opens JEI on the recipe categories that belong to {@code family}. No-op without JEI. */
    public static void openRecipeCategories(DeorumMachineFamily family) {
        if (family != null && ModList.get().isLoaded(JEI)) {
            DeorumJeiHook.open(family);
        }
    }
}
