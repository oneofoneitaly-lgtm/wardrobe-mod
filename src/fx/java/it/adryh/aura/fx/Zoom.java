package it.adryh.aura.fx;

import com.mojang.blaze3d.platform.InputConstants;
import it.adryh.aura.core.Theme;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ViewportEvent;
import org.lwjgl.glfw.GLFW;

final class Zoom {
    static final KeyMapping KEY = new KeyMapping("Zoom Aura", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, "Aura Modpack");
    private static float current = 1F;
    private static float level;
    private static boolean held;
    private static boolean wasSmooth;
    private static long last = System.currentTimeMillis();

    private Zoom() {
    }

    private static boolean pressed() {
        return FxSettings.ZOOM.on() && KEY.isDown() && Minecraft.getInstance().screen == null;
    }

    static void tick() {
        boolean now = pressed();
        if (now != held) {
            Minecraft mc = Minecraft.getInstance();
            if (now) {
                level = FxSettings.ZOOM_LEVEL.f();
                wasSmooth = mc.options.smoothCamera;
                if (FxSettings.ZOOM_CINEMA.on()) {
                    mc.options.smoothCamera = true;
                }
            } else {
                mc.options.smoothCamera = wasSmooth;
            }
            held = now;
        }
    }

    static void fov(ViewportEvent.ComputeFov event) {
        // only the world view is magnified, not the held item
        if (!event.usedConfiguredFov()) {
            return;
        }
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1F, (now - last) / 1000F);
        last = now;
        float target = held ? 1F / level : 1F;
        current = FxSettings.ZOOM_SMOOTH.on() ? Theme.approach(current, target, dt) : target;
        if (current < 0.999F) {
            event.setFOV(event.getFOV() * current);
        }
    }

    /** True when the wheel was used for the zoom, so the hotbar must not move. */
    static boolean scroll(InputEvent.MouseScrollingEvent event) {
        if (!held) {
            return false;
        }
        level = (float) Math.max(1.5, Math.min(30, level * Math.pow(1.15, event.getDeltaY())));
        return true;
    }
}
