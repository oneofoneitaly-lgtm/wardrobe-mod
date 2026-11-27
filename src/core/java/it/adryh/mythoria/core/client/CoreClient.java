package it.adryh.mythoria.core.client;

import com.mojang.blaze3d.platform.InputConstants;
import it.adryh.mythoria.core.net.Net;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

public final class CoreClient {
    public static final KeyMapping OPEN_MARKET = new KeyMapping("key.mythoria.market", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M, "key.categories.mythoria");

    private CoreClient() {}

    public static void init(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener((RegisterKeyMappingsEvent e) -> e.register(OPEN_MARKET));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post e) -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean pressed = false;
        while (OPEN_MARKET.consumeClick()) {
            pressed = true;
        }
        if (pressed && mc.player != null && mc.screen == null) {
            Net.requestOpen();
        }
    }

    public static void onState(Net.State state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MarketScreen screen) {
            screen.update(state);
        } else if (state.result() == 0) {
            mc.setScreen(new MarketScreen(state.balance()));
        }
    }
}
