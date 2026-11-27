package it.adryh.aura.core;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Smooth, anti-aliased shapes drawn with plain coloured quads. */
public final class Draw {
    public static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("auramenu", "textures/gui/glow.png");

    private Draw() {
    }

    @FunctionalInterface
    private interface Paint {
        int at(float x, float y);
    }

    /** Size of one screen pixel in GUI units. */
    public static float pixel() {
        return (float) (1.0 / Math.max(1.0, Minecraft.getInstance().getWindow().getGuiScale()));
    }

    public static void rect(GuiGraphics g, float x, float y, float w, float h, float r, int color) {
        fill(g, x, y, w, h, r, (px, py) -> color);
    }

    public static void gradientV(GuiGraphics g, float x, float y, float w, float h, float r, int top, int bottom) {
        fill(g, x, y, w, h, r, (px, py) -> Colors.lerp(top, bottom, (py - y) / h));
    }

    public static void gradientH(GuiGraphics g, float x, float y, float w, float h, float r, int left, int right) {
        fill(g, x, y, w, h, r, (px, py) -> Colors.lerp(left, right, (px - x) / w));
    }

    public static void circle(GuiGraphics g, float cx, float cy, float radius, int color) {
        rect(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    public static void outline(GuiGraphics g, float x, float y, float w, float h, float r, float thickness, int color) {
        outline(g, x, y, w, h, r, thickness, (px, py) -> color);
    }

    public static void outlineH(GuiGraphics g, float x, float y, float w, float h, float r, float thickness, int left, int right) {
        outline(g, x, y, w, h, r, thickness, (px, py) -> Colors.lerp(left, right, (px - x) / w));
    }

    private static void outline(GuiGraphics g, float x, float y, float w, float h, float r, float t, Paint paint) {
        if (w <= 0 || h <= 0) {
            return;
        }
        r = Math.max(0.01F, Math.min(r, Math.min(w, h) / 2F));
        float f = pixel() / 2F;
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        ring(vc, m, x, y, w, h, r, -t - f, 0F, -t + f, 1F, paint);
        if (t > 2 * f) {
            ring(vc, m, x, y, w, h, r, -t + f, 1F, -f, 1F, paint);
        }
        ring(vc, m, x, y, w, h, r, -f, 1F, f, 0F, paint);
        g.flush();
    }

    /** Soft shadow or glow around a rounded rectangle. */
    public static void shadow(GuiGraphics g, float x, float y, float w, float h, float r, float spread, int color) {
        r = Math.max(0.01F, Math.min(r, Math.min(w, h) / 2F));
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        Paint mid = (px, py) -> Colors.fade(color, 0.35F);
        ring(vc, m, x, y, w, h, r, 0F, 1F, spread * 0.4F, 0.35F, (px, py) -> color);
        ring(vc, m, x, y, w, h, r, spread * 0.4F, 1F, spread, 0F, mid);
        g.flush();
    }

    private static void fill(GuiGraphics g, float x, float y, float w, float h, float r, Paint paint) {
        if (w <= 0 || h <= 0) {
            return;
        }
        r = Math.max(0.01F, Math.min(r, Math.min(w, h) / 2F));
        float f = pixel() / 2F;
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        float[] p = path(x, y, w, h, r, -f);
        float cx = x + w / 2F, cy = y + h / 2F;
        int n = p.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            vertex(vc, m, cx, cy, paint.at(cx, cy));
            vertex(vc, m, p[i * 2], p[i * 2 + 1], paint.at(p[i * 2], p[i * 2 + 1]));
            vertex(vc, m, p[j * 2], p[j * 2 + 1], paint.at(p[j * 2], p[j * 2 + 1]));
            vertex(vc, m, p[j * 2], p[j * 2 + 1], paint.at(p[j * 2], p[j * 2 + 1]));
        }
        ring(vc, m, x, y, w, h, r, -f, 1F, f, 0F, paint);
        g.flush();
    }

    private static void ring(VertexConsumer vc, Matrix4f m, float x, float y, float w, float h, float r,
                             float inner, float innerAlpha, float outer, float outerAlpha, Paint paint) {
        float[] a = path(x, y, w, h, r, inner);
        float[] b = path(x, y, w, h, r, outer);
        int n = a.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            vertex(vc, m, a[i * 2], a[i * 2 + 1], Colors.fade(paint.at(a[i * 2], a[i * 2 + 1]), innerAlpha));
            vertex(vc, m, b[i * 2], b[i * 2 + 1], Colors.fade(paint.at(b[i * 2], b[i * 2 + 1]), outerAlpha));
            vertex(vc, m, b[j * 2], b[j * 2 + 1], Colors.fade(paint.at(b[j * 2], b[j * 2 + 1]), outerAlpha));
            vertex(vc, m, a[j * 2], a[j * 2 + 1], Colors.fade(paint.at(a[j * 2], a[j * 2 + 1]), innerAlpha));
        }
    }

    /** Outline points, counter-clockwise on screen, starting at the top of the top-left corner. */
    private static float[] path(float x, float y, float w, float h, float r, float offset) {
        int seg = Math.max(4, Math.min(24, (int) (r / pixel() / 2F)));
        float[] out = new float[(seg + 1) * 8];
        float[][] centers = {{x + r, y + r}, {x + r, y + h - r}, {x + w - r, y + h - r}, {x + w - r, y + r}};
        float radius = Math.max(0F, r + offset);
        // Sharp corners still need to move inwards when the offset exceeds the radius.
        float extra = Math.min(0F, r + offset);
        float[][] shift = {{-1, -1}, {-1, 1}, {1, 1}, {1, -1}};
        int k = 0;
        for (int c = 0; c < 4; c++) {
            double start = Math.toRadians(270 - 90 * c);
            for (int s = 0; s <= seg; s++) {
                double angle = start - Math.toRadians(90.0 * s / seg);
                out[k++] = centers[c][0] + shift[c][0] * extra + radius * (float) Math.cos(angle);
                out[k++] = centers[c][1] + shift[c][1] * extra + radius * (float) Math.sin(angle);
            }
        }
        return out;
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, int color) {
        vc.addVertex(m, x, y, 0F).setColor(color);
    }

    /** Soft coloured light centred on a point. */
    public static void glow(GuiGraphics g, float cx, float cy, float size, int color) {
        texture(g, GLOW, cx - size / 2F, cy - size / 2F, size, size, 128, color);
    }

    /** Draws a whole square or full-screen texture tinted with a colour. */
    public static void texture(GuiGraphics g, ResourceLocation id, float x, float y, float w, float h, int size, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0F);
        g.pose().scale(w / size, h / size, 1F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(Colors.red(color), Colors.green(color), Colors.blue(color), (color >>> 24) / 255F);
        g.blit(id, 0, 0, size, size, 0F, 0F, size, size, size, size);
        g.setColor(1F, 1F, 1F, 1F);
        g.pose().popPose();
    }
}
