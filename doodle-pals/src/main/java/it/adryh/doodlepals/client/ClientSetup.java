package it.adryh.doodlepals.client;

import com.mojang.blaze3d.platform.InputConstants;
import it.adryh.doodlepals.DoodlePals;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

public final class ClientSetup {
    private static final KeyMapping DRAW = new KeyMapping("key.doodlepals.draw", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J, "key.categories.doodlepals");

    private ClientSetup() {}

    public static void init(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(DoodlePals.PAL.get(), PalRenderer::new));
        context.getModEventBus().addListener((RegisterKeyMappingsEvent event) -> event.register(DRAW));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post event) -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean pressed = false;
        while (DRAW.consumeClick()) {
            pressed = true;
        }
        if (pressed && mc.player != null && mc.screen == null) {
            open(mc.player.getAbilities().instabuild);
        }
    }

    public static void open(boolean free) {
        Minecraft.getInstance().setScreen(new SketchScreen(free));
    }
}
