package dev.codeonigiri.exmekdeorum.compat;

import com.beipuo.mekenergistics.api.upgrade.MePatternAutomation;
import dev.codeonigiri.exmekdeorum.registry.ModMachines;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Registers this mod's block entities with Mek Energistics' pattern-automation SPI.
 *
 * <p>Loaded reflectively from {@code ExMekanismDeorum} only when the mod is present, so neither
 * this class nor the SPI is touched (or required) otherwise.</p>
 */
public final class MekEnergisticsCompat {

    private MekEnergisticsCompat() {
    }

    public static void registerBlocks() {
        for (var block : ModMachines.getAll()) {
            MePatternAutomation.registerBlock(BuiltInRegistries.BLOCK.getKey(block.value()));
        }
    }
}
