package it.adryh.aura.core;

import net.minecraft.client.gui.GuiGraphics;

/** The signature glass panel of the pack. */
public final class Glass {
    private Glass() {
    }

    public static void panel(GuiGraphics g, float x, float y, float w, float h) {
        panel(g, x, y, w, h, Theme.radius(), Theme.glass());
    }

    public static void panel(GuiGraphics g, float x, float y, float w, float h, float r, int fill) {
        Draw.shadow(g, x, y + 2, w, h, r, 14F, 0x66000000);
        Draw.rect(g, x, y, w, h, r, fill);
        // light falling from the top edge
        Draw.gradientV(g, x, y, w, Math.min(h, 36F), r, 0x14FFFFFF, 0x00FFFFFF);
        Draw.outlineH(g, x, y, w, h, r, 0.75F, Colors.alpha(Theme.accent(), 0.55F), Colors.alpha(Theme.second(), 0.30F));
    }
}
