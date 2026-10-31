package dev.exmekdeorum.compat.jei;

import dev.exmekdeorum.ExMekanismDeorum;
import dev.exmekdeorum.machine.AttributeMachineFamily;
import dev.exmekdeorum.machine.DeorumMachineFamily;
import dev.exmekdeorum.registry.ModMachines;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.registration.impl.BlockRegistryObject;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI integration: lists this mod's machines as crafting catalysts for the
 * Ex Deorum recipe categories they can run.
 *
 * <p>The machines do not own recipe types themselves; they read Ex Deorum's
 * recipe caches at runtime. Registering catalysts against Ex Deorum's existing
 * {@code mezz.jei.api.recipe.RecipeType}s is therefore enough for JEI to show "this machine can
 * process that recipe" on the hammer / sieve / barrel / crucible pages, without
 * duplicating any recipe data.</p>
 *
 * <p>Ex Deorum changes its JEI category namespace to {@code exdeorum_emi} when
 * EMI is installed (so the two mods can share recipe pages), so the namespace is
 * chosen the same way here.</p>
 *
 * <p>This class is only loaded when JEI is present: {@link JeiPlugin} is
 * discovered by JEI's own plugin loader, and JEI is an optional dependency in
 * {@code neoforge.mods.toml}. The server never touches it.</p>
 */
@JeiPlugin
public final class DeorumJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ExMekanismDeorum.MOD_ID, "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        DeorumJeiHook.setRuntime(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        DeorumJeiHook.clearRuntime();
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (BlockRegistryObject<?, ?> block : ModMachines.getAll()) {
            DeorumMachineFamily family = familyOf(block);
            if (family == null) {
                continue;
            }
            ItemStack catalyst = new ItemStack(block.value());
            switch (family) {
                case HAMMER -> registration.addRecipeCatalyst(catalyst, DeorumJeiRecipeTypes.HAMMER, DeorumJeiRecipeTypes.COMPRESSED_HAMMER);
                case SIEVE -> registration.addRecipeCatalyst(catalyst, DeorumJeiRecipeTypes.SIEVE);
                case BARREL -> registration.addRecipeCatalyst(catalyst, DeorumJeiRecipeTypes.BARREL_COMPOST,
                        DeorumJeiRecipeTypes.BARREL_MIXING, DeorumJeiRecipeTypes.BARREL_FLUID_MIXING);
                case CRUCIBLE -> registration.addRecipeCatalyst(catalyst, DeorumJeiRecipeTypes.LAVA_CRUCIBLE,
                        DeorumJeiRecipeTypes.CRUCIBLE_HEAT_SOURCES);
                case WATER_CRUCIBLE -> registration.addRecipeCatalyst(catalyst, DeorumJeiRecipeTypes.WATER_CRUCIBLE);
            }
        }
    }

    private static DeorumMachineFamily familyOf(BlockRegistryObject<?, ?> block) {
        AttributeMachineFamily attribute = Attribute.get(block.value(), AttributeMachineFamily.class);
        return attribute == null ? null : attribute.family();
    }
}
