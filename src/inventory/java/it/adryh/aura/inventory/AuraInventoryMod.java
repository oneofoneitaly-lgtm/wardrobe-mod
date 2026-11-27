package it.adryh.aura.inventory;

import it.adryh.aura.core.Aura;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("aurainventory")
public final class AuraInventoryMod {
    public AuraInventoryMod(FMLJavaModLoadingContext context) {
        Aura.register(InventorySettings.MODULE);
        context.getModEventBus().addListener((RegisterClientReloadListenersEvent e) ->
                e.registerReloadListener((ResourceManagerReloadListener) manager -> Themer.invalidate()));
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            if (e.getNewScreen() instanceof AbstractContainerScreen<?>) {
                Themer.apply();
                // switching between creative tabs must not replay the animation
                if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)) {
                    Effects.opened();
                }
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Render.Pre e) -> Effects.before(e));
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Render.Post e) -> Effects.after(e));
        MinecraftForge.EVENT_BUS.addListener((ContainerScreenEvent.Render.Background e) -> Effects.background(e));
        MinecraftForge.EVENT_BUS.addListener((ContainerScreenEvent.Render.Foreground e) -> Effects.foreground(e));
        MinecraftForge.EVENT_BUS.addListener((RenderTooltipEvent.Color e) -> Effects.tooltip(e));
    }
}
