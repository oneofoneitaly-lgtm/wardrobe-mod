package it.adryh.aura.fx;

import it.adryh.aura.core.Aura;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("aurafx")
public final class AuraFxMod {
    public AuraFxMod(FMLJavaModLoadingContext context) {
        Aura.register(FxSettings.MODULE);
        context.getModEventBus().addListener((RegisterKeyMappingsEvent e) -> e.register(Zoom.KEY));
        context.getModEventBus().addListener(AuraFxMod::layers);
        MinecraftForge.EVENT_BUS.addListener((ViewportEvent.ComputeFov e) -> Zoom.fov(e));
        MinecraftForge.EVENT_BUS.addListener((InputEvent.MouseScrollingEvent e) -> {
            if (Zoom.scroll(e)) {
                e.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post e) -> {
            Zoom.tick();
            Particles.tick();
        });
        MinecraftForge.EVENT_BUS.addListener((AttackEntityEvent e) -> Particles.hit(e));
        MinecraftForge.EVENT_BUS.addListener(AuraFxMod::highlight);
    }

    private static void highlight(RenderHighlightEvent.Block event) {
        if (FxSettings.OUTLINE.on() && BlockOutline.render(event)) {
            event.setCanceled(true); // hides the vanilla black outline
        }
    }

    private static void layers(AddGuiOverlayLayersEvent event) {
        ForgeLayeredDraw draw = event.getLayeredDraw();
        ResourceLocation stack = ForgeLayeredDraw.PRE_SLEEP_STACK;
        draw.addBelow(stack, id("vignette"), ForgeLayeredDraw.CROSSHAIR, Vignette::render);
        draw.addAbove(stack, id("crosshair"), ForgeLayeredDraw.CROSSHAIR, Crosshair::render);
        draw.addConditionTo(stack, ForgeLayeredDraw.CROSSHAIR, () -> !Crosshair.active());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("aurafx", path);
    }
}
