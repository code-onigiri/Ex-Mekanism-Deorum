package dev.codeonigiri.exmekdeorum.client;

import dev.codeonigiri.exmekdeorum.ExMekanismDeorum;
import dev.codeonigiri.exmekdeorum.registry.ModContainers;
import mekanism.client.ClientRegistrationUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side registration: the shared machine container gets the shared machine screen.
 */
@EventBusSubscriber(modid = ExMekanismDeorum.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        ClientRegistrationUtil.registerScreen(event, ModContainers.DEORUM_MACHINE, GuiDeorumMachine::new);
    }
}
