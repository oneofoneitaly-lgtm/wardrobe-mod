package it.adryh.invstyles.client;

import com.mojang.blaze3d.platform.NativeImage;
import it.adryh.invstyles.paint.Accent;
import it.adryh.invstyles.paint.Painter;
import it.adryh.invstyles.paint.Style;

/** Between Minecraft's images (ABGR) and the painter's pixels (ARGB). */
final class Images {
    private Images() {}

    static NativeImage paint(NativeImage original, String path, Style style, Accent accent) {
        int w = original.getWidth();
        int h = original.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixels[y * w + x] = swap(original.getPixelRGBA(x, y));
            }
        }
        int[] painted = Painter.paint(path, pixels, w, h, style, accent);
        NativeImage image = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setPixelRGBA(x, y, swap(painted[y * w + x]));
            }
        }
        return image;
    }

    /** swaps red and blue */
    private static int swap(int colour) {
        return colour & 0xFF00FF00 | (colour & 0xFF) << 16 | colour >> 16 & 0xFF;
    }
}
