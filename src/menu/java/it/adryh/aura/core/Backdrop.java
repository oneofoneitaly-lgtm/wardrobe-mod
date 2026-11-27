package it.adryh.aura.core;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/** Animated background used by the title screen and the out-of-game menus. */
public final class Backdrop {
    private static final int COUNT = 70;
    private static final float[][] DOTS = new float[COUNT][4];

    static {
        Random r = new Random(7);
        for (float[] d : DOTS) {
            d[0] = r.nextFloat();
            d[1] = r.nextFloat();
            d[2] = 0.3F + r.nextFloat();
            d[3] = r.nextFloat() * 6.283F;
        }
    }

    private Backdrop() {
    }

    public static void render(GuiGraphics g, int w, int h) {
        float t = Theme.time();
        int a = Theme.accent();
        int b = Theme.second();
        Draw.gradientV(g, -2, -2, w + 4, h + 4, 0, Colors.lerp(0xFF0B0D1C, a, 0.07F), 0xFF05060C);
        float size = Math.max(w, h);
        if (Theme.BACKGROUND.index() == 1) {
            waves(g, w, h, t, a, b);
        } else {
            Draw.glow(g, w * 0.18F + sin(t * 0.17F) * w * 0.10F, h * 0.22F + cos(t * 0.21F) * h * 0.12F, size * 1.05F, Colors.alpha(a, 0.50F));
            Draw.glow(g, w * 0.85F + cos(t * 0.13F) * w * 0.10F, h * 0.75F + sin(t * 0.19F) * h * 0.14F, size * 0.95F, Colors.alpha(b, 0.34F));
            Draw.glow(g, w * 0.55F + sin(t * 0.11F + 2F) * w * 0.25F, h * 1.02F, size * 0.80F, Colors.alpha(Colors.lerp(a, b, 0.5F), 0.30F));
        }
        if (Theme.PARTICLES.on()) {
            for (float[] d : DOTS) {
                float y = (d[1] - t * 0.012F * d[2]) % 1F;
                if (y < 0) {
                    y += 1F;
                }
                float x = d[0] * w + sin(t * 0.3F + d[3]) * 8F;
                float alpha = (0.18F + 0.30F * (0.5F + 0.5F * sin(t * 1.4F * d[2] + d[3]))) * Math.min(1F, y * 4F);
                Draw.circle(g, x, y * h, 0.5F + d[2] * 0.7F, Colors.alpha(Colors.lerp(0xFFFFFFFF, b, d[0]), alpha));
            }
        }
        Draw.gradientV(g, -2, h * 0.65F, w + 4, h * 0.35F + 2, 0, 0x00000000, 0x80000000);
    }

    private static void waves(GuiGraphics g, int w, int h, float t, int a, int b) {
        Draw.glow(g, w * 0.5F, h * 0.1F, Math.max(w, h), Colors.alpha(a, 0.25F));
        int steps = 64;
        for (int layer = 0; layer < 4; layer++) {
            int color = Colors.alpha(Colors.lerp(a, b, layer / 3F), 0.13F + layer * 0.03F);
            float step = w / (float) steps;
            for (int i = 0; i < steps; i++) {
                float x = i * step;
                float y = h * (0.56F + layer * 0.10F) + sin(x * 0.012F + t * (0.5F + layer * 0.12F) + layer) * h * 0.06F
                        + sin(x * 0.031F - t * 0.35F) * h * 0.02F;
                g.pose().pushPose();
                g.pose().translate(x, 0, 0);
                g.pose().scale(step / 16F, 1F, 1F);
                g.fillGradient(0, (int) y, 16, h, color, Colors.fade(color, 0.1F));
                g.pose().popPose();
                g.fill((int) x, (int) y, (int) Math.ceil(x + step), (int) y + 1, Colors.fade(color, 2.2F));
            }
        }
    }

    private static float sin(float v) {
        return (float) Math.sin(v);
    }

    private static float cos(float v) {
        return (float) Math.cos(v);
    }
}
