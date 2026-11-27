package it.adryh.invstyles.paint;

/** Small colour helpers; colours are 0xAARRGGBB. */
final class Paint {
    private Paint() {}

    static int argb(int alpha, int rgb) {
        return alpha << 24 | rgb & 0xFFFFFF;
    }

    static int rgb(int rgb) {
        return argb(255, rgb);
    }

    /** Blends two colours, alpha included. */
    static int mix(int a, int b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int x = a >>> shift & 255;
            int y = b >>> shift & 255;
            out |= Math.round(x + (y - x) * t) << shift;
        }
        return out;
    }

    static int shade(int colour, float amount) {
        int target = amount < 0 ? 0x000000 : 0xFFFFFF;
        return argb(colour >>> 24, mix(colour, target, Math.abs(amount)));
    }

    private static float hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 1442695041;
        h = (h ^ h >>> 13) * 1274126177;
        return ((h ^ h >>> 16) & 0xFFFF) / 65535.0F;
    }

    /** Smooth noise between -1 and 1, the same every time. */
    static float noise(float x, float y, int seed) {
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        float fx = x - ix;
        float fy = y - iy;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        float top = hash(ix, iy, seed) + (hash(ix + 1, iy, seed) - hash(ix, iy, seed)) * fx;
        float low = hash(ix, iy + 1, seed) + (hash(ix + 1, iy + 1, seed) - hash(ix, iy + 1, seed)) * fx;
        return (top + (low - top) * fy) * 2.0F - 1.0F;
    }
}
