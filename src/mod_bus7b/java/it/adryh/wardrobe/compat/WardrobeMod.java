package it.adryh.wardrobe.compat;

import it.adryh.wardrobe.ClientHooks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("wardrobe")
public final class WardrobeMod {
    public WardrobeMod(FMLJavaModLoadingContext context) {
        KeyMapping key = Keys.create();
        RegisterKeyMappingsEvent.BUS.addListener(event -> event.register(key));
        EntityRenderersEvent.AddLayers.BUS.addListener(WardrobeMod::addLayers);
        RegisterClientCommandsEvent.BUS.addListener(event -> ClientHooks.registerCommands(event.getDispatcher()));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> ClientHooks.tick(key));
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        event.getModelTypes().forEach(type -> {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(type);
            if (renderer != null) renderer.addLayer(new WardrobeLayer(renderer));
        });
    }
}
