package it.adryh.aura.core;

/** ARGB helpers. */
public final class Colors {
    private Colors() {
    }

    public static int alpha(int color, float alpha) {
        int a = Math.round(Math.max(0F, Math.min(1F, alpha)) * 255F);
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static int fade(int color, float factor) {
        return alpha(color, ((color >>> 24) / 255F) * factor);
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0F, Math.min(1F, t));
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
                | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
    }

    /** Opaque RGB from hue, saturation and value, all in 0..1. */
    public static int hsv(float h, float s, float v) {
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(h, clamp(s), clamp(v)) & 0xFFFFFF);
    }

    public static float[] toHsv(int color) {
        return java.awt.Color.RGBtoHSB((color >> 16) & 255, (color >> 8) & 255, color & 255, null);
    }

    public static float red(int c) {
        return ((c >> 16) & 255) / 255F;
    }

    public static float green(int c) {
        return ((c >> 8) & 255) / 255F;
    }

    public static float blue(int c) {
        return (c & 255) / 255F;
    }

    private static float clamp(float v) {
        return Math.max(0F, Math.min(1F, v));
    }
}
