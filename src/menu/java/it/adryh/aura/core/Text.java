package it.adryh.aura.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/** Text drawn with the Inter font bundled with the pack. */
public final class Text {
    private static final Style REGULAR = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("auramenu", "inter"));
    private static final Style BOLD = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("auramenu", "inter_bold"));

    private Text() {
    }

    public static MutableComponent of(String text, boolean bold) {
        MutableComponent c = Component.literal(text);
        if (Theme.modernFont()) {
            return c.setStyle(bold ? BOLD : REGULAR);
        }
        return bold ? c.setStyle(Style.EMPTY.withBold(true)) : c;
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    public static float width(String text, boolean bold, float scale) {
        return font().width(of(text, bold)) * scale;
    }

    public static float width(String text) {
        return width(text, false, 1F);
    }

    public static void draw(GuiGraphics g, String text, float x, float y, int color) {
        draw(g, text, x, y, color, false, 1F);
    }

    public static void bold(GuiGraphics g, String text, float x, float y, int color) {
        draw(g, text, x, y, color, true, 1F);
    }

    public static void draw(GuiGraphics g, String text, float x, float y, int color, boolean bold, float scale) {
        // Minecraft treats an almost transparent colour as opaque, so skip it.
        if ((color >>> 24) < 8 || text.isEmpty()) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0F);
        g.pose().scale(scale, scale, 1F);
        g.drawString(font(), of(text, bold), 0, 0, color, false);
        g.pose().popPose();
    }

    public static void centered(GuiGraphics g, String text, float cx, float y, int color) {
        draw(g, text, cx - width(text) / 2F, y, color);
    }

    public static void centered(GuiGraphics g, String text, float cx, float y, int color, boolean bold, float scale) {
        draw(g, text, cx - width(text, bold, scale) / 2F, y, color, bold, scale);
    }

    public static String fit(String text, float maxWidth, boolean bold, float scale) {
        if (width(text, bold, scale) <= maxWidth) {
            return text;
        }
        String s = text;
        while (!s.isEmpty() && width(s + "...", bold, scale) > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }
}
