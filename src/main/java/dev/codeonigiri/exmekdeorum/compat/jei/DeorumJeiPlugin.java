package dev.codeonigiri.exmekdeorum.compat.jei;

import dev.codeonigiri.exmekdeorum.ExMekanismDeorum;
import dev.codeonigiri.exmekdeorum.machine.AttributeMachineFamily;
import dev.codeonigiri.exmekdeorum.machine.DeorumMachineFamily;
import dev.codeonigiri.exmekdeorum.registry.ModMachines;
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
 * <p>The machines do not own recipe types themselves; they look Ex Deorum's
 * recipes up at runtime. Registering catalysts against Ex Deorum's existing
 * {@code mezz.jei.api.recipe.RecipeType}s is therefore enough for JEI to show "this machine can
 * process that recipe" on the hammer / sieve / barrel / crucible pages, without
 * duplicating any recipe data.</p>
 *
 * <p>The 1.20.1 Ex Deorum line always registers its JEI categories under
 * {@code exdeorum}; unlike the 1.21 line it does not switch to an
 * {@code exdeorum_emi} namespace when EMI is installed.</p>
 *
 * <p>This class is only loaded when JEI is present: {@link JeiPlugin} is
 * discovered by JEI's own plugin loader, and JEI is an optional dependency in
 * {@code mods.toml}. The server never touches it.</p>
 */
@JeiPlugin
public final class DeorumJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(ExMekanismDeorum.MOD_ID, "jei_plugin");
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
            ItemStack catalyst = new ItemStack(block.asItem());
            switch (family) {
                case HAMMER -> registration.addRecipeCatalyst(catalyst, DeorumJeiRecipeTypes.HAMMER, DeorumJeiRecipeTypes.COMPRESSED_HAMMER);
                case SIEVE -> registration.addRecipeCatalyst(catalyst,
                        DeorumJeiRecipeTypes.SIEVE, DeorumJeiRecipeTypes.COMPRESSED_SIEVE);
                case BARREL -> registration.addRecipeCatalyst(catalyst, DeorumJeiRecipeTypes.BARREL_COMPOST,
                        DeorumJeiRecipeTypes.BARREL_MIXING, DeorumJeiRecipeTypes.BARREL_FLUID_MIXING);
                case CRUCIBLE -> registration.addRecipeCatalyst(catalyst, DeorumJeiRecipeTypes.LAVA_CRUCIBLE,
                        DeorumJeiRecipeTypes.CRUCIBLE_HEAT_SOURCES);
                case WATER_CRUCIBLE -> registration.addRecipeCatalyst(catalyst, DeorumJeiRecipeTypes.WATER_CRUCIBLE);
            }
        }
    }

    private static DeorumMachineFamily familyOf(BlockRegistryObject<?, ?> block) {
        AttributeMachineFamily attribute = Attribute.get(block.getBlock(), AttributeMachineFamily.class);
        return attribute == null ? null : attribute.family();
    }
}
