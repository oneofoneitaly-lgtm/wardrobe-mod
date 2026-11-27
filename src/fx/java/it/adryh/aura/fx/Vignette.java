package it.adryh.aura.fx;

import it.adryh.aura.core.Colors;
import it.adryh.aura.core.Draw;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

final class Vignette {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("aurafx", "textures/gui/vignette.png");

    private Vignette() {
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) {
            return;
        }
        float base = FxSettings.VIGNETTE.f() / 100F;
        if (base > 0.01F) {
            draw(g, Colors.alpha(FxSettings.VIGNETTE_COLOR.get(), base));
        }
        if (p.isCreative() || p.isSpectator()) {
            return;
        }
        if (FxSettings.DAMAGE.on() && p.hurtTime > 0 && p.hurtDuration > 0) {
            float k = (p.hurtTime - delta.getGameTimeDeltaPartialTick(false)) / p.hurtDuration;
            draw(g, Colors.alpha(0xFF1744, Math.max(0F, k) * 0.85F));
        }
        if (FxSettings.LOW_HEALTH.on() && p.isAlive() && p.getHealth() <= p.getMaxHealth() * 0.3F) {
            double beat = Math.pow(Math.abs(Math.sin(System.currentTimeMillis() / 260.0)), 3);
            draw(g, Colors.alpha(0xE11D48, (float) (0.25 + 0.55 * beat)));
        }
    }

    private static void draw(GuiGraphics g, int color) {
        Draw.texture(g, TEXTURE, 0, 0, g.guiWidth(), g.guiHeight(), 256, color);
    }
}
