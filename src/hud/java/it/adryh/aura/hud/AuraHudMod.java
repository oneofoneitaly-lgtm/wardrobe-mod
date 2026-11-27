package it.adryh.aura.hud;

import it.adryh.aura.core.Aura;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

@Mod("aurahud")
public final class AuraHudMod {
    public AuraHudMod(FMLJavaModLoadingContext context) {
        Aura.register(HudSettings.MODULE);
        context.getModEventBus().addListener(AuraHudMod::layers);
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post e) -> Widgets.tick());
        MinecraftForge.EVENT_BUS.addListener((InputEvent.MouseButton.Pre e) -> {
            if (e.getAction() == GLFW.GLFW_PRESS) {
                Widgets.click(e.getButton());
            }
        });
    }

    private static void layers(AddGuiOverlayLayersEvent event) {
        ForgeLayeredDraw draw = event.getLayeredDraw();
        ResourceLocation stack = ForgeLayeredDraw.PRE_SLEEP_STACK;
        draw.addAbove(stack, id("hotbar"), ForgeLayeredDraw.HOTBAR, Hotbar::render);
        draw.addConditionTo(stack, ForgeLayeredDraw.HOTBAR, () -> !Hotbar.active());
        draw.addConditionTo(stack, ForgeLayeredDraw.EXPERIENCE, () -> !Hotbar.active());
        draw.addConditionTo(stack, ForgeLayeredDraw.POTION_EFFECTS, () -> !HudSettings.POTIONS.on());
        draw.add(stack, id("widgets"), Widgets::render);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("aurahud", path);
    }
}
