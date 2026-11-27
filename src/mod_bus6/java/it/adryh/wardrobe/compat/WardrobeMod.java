package it.adryh.wardrobe.compat;

import it.adryh.wardrobe.ClientHooks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("wardrobe")
public final class WardrobeMod {
    public WardrobeMod(FMLJavaModLoadingContext context) {
        KeyMapping key = Keys.create();
        IEventBus modBus = context.getModEventBus();
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(key));
        modBus.addListener((EntityRenderersEvent.AddLayers event) -> addLayers(event));
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> ClientHooks.registerCommands(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post event) -> ClientHooks.tick(key));
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        event.getSkins().forEach(skin -> {
            PlayerRenderer renderer = event.getPlayerSkin(skin);
            if (renderer != null) renderer.addLayer(new WardrobeLayer(renderer));
        });
    }
}
