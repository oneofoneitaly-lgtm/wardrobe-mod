package it.adryh.invstyles.paint;

/**
 * Repaints a vanilla gui texture. Vanilla inventories are drawn with a handful of greys, which
 * tell what every pixel is; coloured pixels are details and stay as they are.
 */
public final class Painter {
    private static final int PANEL = 0xC6C6C6;
    private static final int SLOT_DARK = 0x373737;
    private static final int SLOT_FILL = 0x8B8B8B;
    private static final int WHITE = 0xFFFFFF;
    private static final int SHADOW = 0x555555;

    private final int[] source;
    private final int w;
    private final int h;

    private Painter(int[] source, int w, int h) {
        this.source = source;
        this.w = w;
        this.h = h;
    }

    /** @param pixels 0xAARRGGBB, row by row; the result has the same layout */
    public static int[] paint(String path, int[] pixels, int w, int h, Style style, Accent accent) {
        if (style == Style.VANILLA) {
            return pixels.clone();
        }
        Painter painter = new Painter(pixels, w, h);
        return path.contains("/hud/") ? painter.hotbar(path, style, accent) : painter.container(path, style, accent);
    }

    private int alpha(int x, int y) {
        return x < 0 || y < 0 || x >= w || y >= h ? 0 : source[y * w + x] >>> 24;
    }

    /** the colour of an opaque pixel, or -1 */
    private int rgb(int x, int y) {
        return alpha(x, y) < 200 ? -1 : source[y * w + x] & 0xFFFFFF;
    }

    private static float light(int c) {
        return ((c >> 16 & 255) * 0.3F + (c >> 8 & 255) * 0.59F + (c & 255) * 0.11F) / 255.0F;
    }

    private static boolean grey(int c) {
        int r = c >> 16 & 255;
        int g = c >> 8 & 255;
        int b = c & 255;
        return Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) <= 10;
    }

    /** For each opaque pixel: how many steps to the nearest see-through one, capped. */
    private int[] depths(int cap) {
        int[] depth = new int[w * h];
        for (int step = 1; step <= cap; step++) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (depth[y * w + x] != 0 || alpha(x, y) < 25) {
                        continue;
                    }
                    boolean border = false;
                    for (int dy = -1; dy <= 1 && !border; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            if ((dx != 0 || dy != 0) && (step == 1 ? alpha(nx, ny) < 25
                                    : nx >= 0 && ny >= 0 && nx < w && ny < h && depth[ny * w + nx] == step - 1)) {
                                border = true;
                                break;
                            }
                        }
                    }
                    if (border) {
                        depth[y * w + x] = step;
                    }
                }
            }
        }
        return depth;
    }

    /** Marks the pixels of every slot-like box: dark top and left edges, light bottom and right. */
    private int[][] slots() {
        int[][] found = new int[w * h][];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (rgb(x, y) != SLOT_DARK || rgb(x - 1, y) == SLOT_DARK && rgb(x - 1, y + 1) != SLOT_FILL) {
                    continue;
                }
                int run = 0;
                while (rgb(x + run, y) == SLOT_DARK) {
                    run++;
                }
                int fall = 0;
                while (rgb(x, y + fall) == SLOT_DARK) {
                    fall++;
                }
                int bw = run + 1;
                int bh = fall + 1;
                if (run < 6 || fall < 4 || rgb(x + bw - 1, y + bh - 1) != WHITE
                        || rgb(x + bw / 2, y + bh - 1) != WHITE || rgb(x + bw - 1, y + bh / 2) != WHITE) {
                    continue;
                }
                int[] box = {x, y, bw, bh};
                for (int by = y; by < y + bh; by++) {
                    for (int bx = x; bx < x + bw; bx++) {
                        found[by * w + bx] = box;
                    }
                }
            }
        }
        return found;
    }

    /**
     * Large flat areas of mid grey that are not slots (the inside of the recipe book, the crafter
     * grid): every pixel of any solid 9x9 square. Arrows and other drawings are thinner.
     */
    private boolean[] wells() {
        final int side = 9;
        boolean[] well = new boolean[w * h];
        int[] run = new int[w * h];      // side of the largest one-colour square ending here
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = rgb(x, y);
                if (c < 0 || !grey(c) || light(c) < 0.35F || light(c) > 0.7F) {
                    continue;
                }
                int i = y * w + x;
                boolean same = x > 0 && y > 0 && rgb(x - 1, y) == c && rgb(x, y - 1) == c && rgb(x - 1, y - 1) == c;
                run[i] = same ? 1 + Math.min(run[i - 1], Math.min(run[i - w], run[i - w - 1])) : 1;
                if (run[i] >= side) {
                    for (int by = y - side + 1; by <= y; by++) {
                        for (int bx = x - side + 1; bx <= x; bx++) {
                            well[by * w + bx] = true;
                        }
                    }
                }
            }
        }
        return well;
    }

    /** Textures whose brownish areas are really text fields and lists: they follow the style too. */
    private static boolean hasBrownFields(String path) {
        return path.endsWith("/stonecutter.png") || path.endsWith("/loom.png") || path.contains("text_field")
                || path.endsWith("/enchantment_slot_disabled.png");
    }

    private static boolean dullBrown(int c) {
        int r = c >> 16 & 255;
        int g = c >> 8 & 255;
        int b = c & 255;
        return r >= g && g >= b && r - b > 10 && r - b <= 48 && light(c) < 0.5F;
    }

    private int[] container(String path, Style style, Accent accent) {
        boolean[] well = wells();
        boolean fields = hasBrownFields(path);
        int width = style.frameWidth();
        int[] depth = depths(width);
        int[][] boxes = slots();
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int a = source[i] >>> 24;
                if (a < 8) {
                    out[i] = source[i];
                    continue;
                }
                int c = source[i] & 0xFFFFFF;
                int d = depth[i];
                int[] box = boxes[i];
                int painted;
                if (box != null) {
                    if (!grey(c)) {
                        painted = source[i];
                    } else if (light(c) < 0.15F) {
                        painted = style.window(x, y, accent);
                    } else if (c == SLOT_FILL || c == SLOT_DARK || c == WHITE) {
                        painted = style.slot(x - box[0], y - box[1], box[2], box[3], x, y, accent);
                    } else if (well[i]) {
                        painted = style.slot(5, 5, 18, 18, x, y, accent);
                    } else {
                        painted = style.mark(light(c), x, y, accent);
                    }
                } else if (fields && dullBrown(c)) {
                    // keep the bevel of the original: lighter pixels stay lighter
                    painted = Paint.mix(style.window(x, y, accent), style.mark(0.5F, x, y, accent), (light(c) - 0.22F) * 2.0F);
                } else if (!grey(c)) {
                    painted = source[i];
                } else if (well[i]) {
                    painted = style.slot(5, 5, 18, 18, x, y, accent);
                } else if (d > 0 && d <= width && (d <= 3 && (light(c) < 0.1F || c == WHITE || c == SHADOW) || c == PANEL)
                        && w >= 20 && h >= 20) {
                    painted = style.frame(d, x, y, accent);
                } else if (c == PANEL) {
                    painted = style.background(x, y, accent);
                } else if (light(c) < 0.15F) {
                    painted = style.window(x, y, accent);
                } else {
                    painted = style.mark(light(c), x, y, accent);
                }
                // soft pixels of the original stay soft
                out[i] = a >= 250 ? painted : (painted >>> 24) * a / 255 << 24 | painted & 0xFFFFFF;
            }
        }
        return out;
    }

    /**
     * The hotbar is repainted by position rather than by colour: nine 18x18 slots, twenty pixels
     * apart, inside a frame. The off hand box has one slot, the selection ring only changes colour.
     */
    private int[] hotbar(String path, Style style, Accent accent) {
        boolean selection = path.contains("hotbar_selection");
        boolean offhand = path.contains("hotbar_offhand");
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int a = source[i] >>> 24;
                if (a < 8) {
                    out[i] = source[i];
                    continue;
                }
                float l = light(source[i] & 0xFFFFFF);
                int lx = offhand ? x - (path.contains("right") ? 9 : 2) : x < 2 ? -1 : (x - 2) % 20;
                int ly = y - (offhand ? 3 : 2);
                int painted;
                if (selection) {
                    painted = l > 0.55F ? style.mark(1.0F, x, y, accent) : style.frame(1, x, y, accent);
                } else if (lx >= 0 && lx < 18 && ly >= 0 && ly < 18) {
                    painted = style.slot(lx, ly, 18, 18, x, y, accent);
                } else {
                    boolean outline = alpha(x - 1, y) < 8 || alpha(x + 1, y) < 8 || alpha(x, y - 1) < 8 || alpha(x, y + 1) < 8;
                    painted = style.frame(outline ? 1 : l > 0.3F ? 2 : 3, x, y, accent);
                }
                out[i] = painted;
            }
        }
        return out;
    }
}
