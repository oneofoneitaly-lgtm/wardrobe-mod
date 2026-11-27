package it.adryh.aura.menu;

import com.mojang.blaze3d.platform.InputConstants;
import it.adryh.aura.core.Aura;
import it.adryh.aura.core.Theme;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

@Mod("auramenu")
public final class AuraMenuMod {
    public static final KeyMapping OPEN = new KeyMapping("Apri Aura Studio", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT, "Aura Modpack");
    private static boolean openRequested;

    public AuraMenuMod(FMLJavaModLoadingContext context) {
        Aura.register(Theme.MODULE);
        context.getModEventBus().addListener((RegisterKeyMappingsEvent e) -> e.register(OPEN));
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent e) -> e.getDispatcher()
                .register(Commands.literal("aura").executes(c -> {
                    // the chat screen closes after the command, so open on the next tick
                    openRequested = true;
                    return 1;
                })));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post e) -> tick());
        MenuHooks.register();
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean pressed = false;
        while (OPEN.consumeClick()) {
            pressed = true;
        }
        if ((pressed || openRequested) && mc.screen == null) {
            openRequested = false;
            mc.setScreen(new HubScreen(null));
        }
    }
}
