package dev.codeonigiri.exmekdeorum;

import com.mojang.logging.LogUtils;
import dev.codeonigiri.exmekdeorum.registry.ModMachines;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Entry point of the example addon.
 *
 * <p>Ex Deorum and Mekanism are declared as required dependencies in
 * {@code META-INF/neoforge.mods.toml}; the dev shell puts both jars on the
 * compile and runtime classpath (see flake.nix and build.gradle).</p>
 */
@Mod(ExMekanismDeorum.MOD_ID)
public final class ExMekanismDeorum {
    public static final String MOD_ID = "exmekdeorum";

    private static final Logger LOGGER = LogUtils.getLogger();

    public ExMekanismDeorum(IEventBus modEventBus, ModContainer modContainer) {
        ModMachines.register(modEventBus);
        if (ModList.get().isLoaded("mekenergistics")) {
            try {
                Class.forName("dev.codeonigiri.exmekdeorum.compat.MekEnergisticsCompat")
                        .getMethod("registerBlocks").invoke(null);
                LOGGER.info("Registered {} machines with Mek Energistics pattern automation.", ModMachines.getAll().size());
            } catch (ReflectiveOperationException e) {
                LOGGER.warn("Mek Energistics is installed but its pattern automation API could not be registered.", e);
            }
        }
        LOGGER.info("Ex Mekanism Deorum loaded; registered {} machine blocks.", ModMachines.getAll().size());
    }
}
