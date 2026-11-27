package it.adryh.invstyles.paint;

import static it.adryh.invstyles.paint.Paint.argb;
import static it.adryh.invstyles.paint.Paint.mix;
import static it.adryh.invstyles.paint.Paint.noise;
import static it.adryh.invstyles.paint.Paint.rgb;
import static it.adryh.invstyles.paint.Paint.shade;

/**
 * A look. The painter finds the parts of a vanilla texture (frame, background, slots, windows,
 * marks) and asks the style which colour each pixel of a part gets.
 */
public enum Style {
    VANILLA("vanilla") {
        @Override
        public int text(Accent accent) {
            return 0x404040;
        }
    },

    GLASS("glass") {
        @Override
        public boolean translucent() {
            return true;
        }

        @Override
        public int text(Accent accent) {
            return 0xECEAF8;
        }

        @Override
        int background(int x, int y, Accent accent) {
            int base = mix(argb(216, 0x1B1A2B), argb(222, 0x0F0E19), y / 220.0F);
            // a soft light of the accent colour falling from the top
            return mix(base, argb(222, accent.colour()), Math.max(0.0F, 0.16F - y * 0.0028F));
        }

        @Override
        int frame(int depth, int x, int y, Accent accent) {
            if (depth == 1) {
                return mix(rgb(accent.colour()), rgb(0x000000), 0.15F + Math.min(0.45F, y * 0.003F));
            }
            return mix(background(x, y, accent), argb(235, accent.colour()), depth == 2 ? 0.28F : 0.08F);
        }

        @Override
        int slot(int lx, int ly, int w, int h, int x, int y, Accent accent) {
            if (corner(lx, ly, w, h)) {
                return background(x, y, accent);
            }
            if (edge(lx, ly, w, h)) {
                return ly == h - 1 || lx == w - 1 ? argb(240, 0x716BA6) : argb(240, 0x4B4773);
            }
            return mix(argb(205, 0x08070F), argb(205, 0x17152A), ly / (float) h);
        }

        @Override
        int window(int x, int y, Accent accent) {
            return argb(235, 0x06050C);
        }

        @Override
        int mark(float light, int x, int y, Accent accent) {
            return light > 0.93F ? rgb(accent.colour()) : mix(rgb(0x1C1A2C), rgb(0x8E89B4), light);
        }
    },

    FANTASY("fantasy") {
        @Override
        public int text(Accent accent) {
            return 0x4A260C;
        }

        @Override
        int background(int x, int y, Accent accent) {
            float paper = noise(x / 17.0F, y / 17.0F, 3) * 0.6F + noise(x / 5.0F, y / 5.0F, 5) * 0.28F + noise(x, y, 7) * 0.14F;
            return shade(rgb(0xE3CB98), paper * 0.1F);
        }

        @Override
        int frame(int depth, int x, int y, Accent accent) {
            float grain = noise((x + y) / 2.5F, (x - y) / 9.0F, 11);
            return switch (depth) {
                case 1 -> rgb(0x241206);
                case 2 -> shade(rgb(0xE8B84A), grain * 0.25F);
                case 3 -> shade(rgb(0x6E3F1B), grain * 0.28F);
                default -> rgb(0x9C7228);
            };
        }

        @Override
        public int frameWidth() {
            return 4;
        }

        @Override
        int slot(int lx, int ly, int w, int h, int x, int y, Accent accent) {
            boolean corner = (lx < 2 || lx >= w - 2) && (ly < 2 || ly >= h - 2);
            if (corner(lx, ly, w, h)) {
                return background(x, y, accent);
            }
            if (corner) {
                return rgb(0xD9A83C);
            }
            if (edge(lx, ly, w, h)) {
                return rgb(0x5C3315);
            }
            if (lx == 1 || ly == 1) {
                return rgb(0x8E6B3E);
            }
            return shade(rgb(0xBE9C66), noise(x / 3.0F, y / 3.0F, 13) * 0.09F);
        }

        @Override
        int window(int x, int y, Accent accent) {
            return shade(rgb(0x2A160A), noise(x / 2.0F, y / 11.0F, 17) * 0.2F);
        }

        @Override
        int mark(float light, int x, int y, Accent accent) {
            return light > 0.93F ? rgb(0xFFD767) : mix(rgb(0x3C200C), rgb(0xE9D4A4), light);
        }
    },

    NEON("neon") {
        @Override
        public int text(Accent accent) {
            return mix(rgb(accent.colour()), rgb(0xFFFFFF), 0.6F) & 0xFFFFFF;
        }

        @Override
        int background(int x, int y, Accent accent) {
            boolean grid = x % 9 == 7 || y % 9 == 7;
            return grid ? mix(rgb(0x07070E), rgb(accent.colour()), 0.07F) : rgb(0x07070E);
        }

        @Override
        int frame(int depth, int x, int y, Accent accent) {
            int glow = rgb(accent.colour());
            return switch (depth) {
                case 1 -> mix(glow, rgb(0x000000), 0.55F);
                case 2 -> mix(glow, rgb(0xFFFFFF), (x + y) % 23 < 3 ? 0.65F : 0.15F);
                default -> mix(rgb(0x07070E), glow, 0.32F);
            };
        }

        @Override
        int slot(int lx, int ly, int w, int h, int x, int y, Accent accent) {
            int glow = rgb(accent.colour());
            if (corner(lx, ly, w, h)) {
                return background(x, y, accent);
            }
            boolean bracket = (lx < 4 || lx >= w - 4) && (ly < 4 || ly >= h - 4);
            if (edge(lx, ly, w, h)) {
                return bracket ? glow : mix(rgb(0x07070E), glow, 0.34F);
            }
            if (lx == 1 || ly == 1 || lx == w - 2 || ly == h - 2) {
                return mix(rgb(0x0A0A15), glow, 0.12F);
            }
            return rgb(0x0A0A15);
        }

        @Override
        int window(int x, int y, Accent accent) {
            return y % 3 == 0 ? mix(rgb(0x020206), rgb(accent.colour()), 0.08F) : rgb(0x020206);
        }

        @Override
        int mark(float light, int x, int y, Accent accent) {
            int glow = rgb(accent.colour());
            return light > 0.93F ? mix(glow, rgb(0xFFFFFF), 0.35F) : mix(rgb(0x0E0E1C), glow, light * 0.75F);
        }
    },

    PASTEL("pastel") {
        @Override
        public int text(Accent accent) {
            return 0x5B416F;
        }

        private int tint(Accent accent, float amount) {
            return mix(rgb(0xFFFFFF), rgb(accent.colour()), amount);
        }

        @Override
        int background(int x, int y, Accent accent) {
            int base = mix(tint(accent, 0.13F), rgb(0xEAF4FF), (x + y) / 360.0F);
            // little dots, staggered
            int row = y / 12;
            int dx = (x + (row % 2) * 6) % 12 - 5;
            int dy = y % 12 - 5;
            return dx * dx + dy * dy <= 2 ? mix(base, rgb(0xFFFFFF), 0.8F) : base;
        }

        @Override
        int frame(int depth, int x, int y, Accent accent) {
            return switch (depth) {
                case 1 -> mix(tint(accent, 0.75F), rgb(0x7B5E99), 0.5F);
                case 2 -> tint(accent, 0.45F);
                default -> rgb(0xFFFFFF);
            };
        }

        @Override
        int slot(int lx, int ly, int w, int h, int x, int y, Accent accent) {
            boolean round = (lx <= 1 && ly == 0) || (lx == 0 && ly == 1) || (lx >= w - 2 && ly == 0) || (lx == w - 1 && ly == 1)
                    || (lx <= 1 && ly == h - 1) || (lx == 0 && ly == h - 2) || (lx >= w - 2 && ly == h - 1) || (lx == w - 1 && ly == h - 2);
            if (round) {
                return background(x, y, accent);
            }
            if (edge(lx, ly, w, h) || (lx == 1 || lx == w - 2) && (ly == 1 || ly == h - 2)) {
                return mix(rgb(accent.colour()), rgb(0x5A4A6A), 0.45F);
            }
            // deep enough for the white item counts to stay readable
            int light = tint(accent, 0.55F);
            int deep = mix(rgb(accent.colour()), rgb(0x5A4A6A), 0.25F);
            int fill = mix(light, deep, 0.5F + 0.2F * ly / (float) h);
            return ly == 1 ? mix(fill, rgb(0xFFFFFF), 0.35F) : fill;
        }

        @Override
        int window(int x, int y, Accent accent) {
            return mix(rgb(0x55426B), rgb(0x3E3054), y / 200.0F);
        }

        @Override
        int mark(float light, int x, int y, Accent accent) {
            return light > 0.93F ? tint(accent, 0.9F) : mix(rgb(0x70548C), rgb(0xF6EEFF), light);
        }
    };

    private final String id;

    Style(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean translucent() {
        return false;
    }

    /** Colour (0xRRGGBB) of the titles written on the inventory. */
    public abstract int text(Accent accent);

    /** How many pixels from the outline belong to the frame. */
    public int frameWidth() {
        return 3;
    }

    int background(int x, int y, Accent accent) {
        throw new IllegalStateException("vanilla is not painted");
    }

    int frame(int depth, int x, int y, Accent accent) {
        return background(x, y, accent);
    }

    int slot(int lx, int ly, int w, int h, int x, int y, Accent accent) {
        return background(x, y, accent);
    }

    /** the dark openings: the player's portrait, text fields */
    int window(int x, int y, Accent accent) {
        return background(x, y, accent);
    }

    /** arrows, lines and other grey drawings; light is the original brightness */
    int mark(float light, int x, int y, Accent accent) {
        return background(x, y, accent);
    }

    private static boolean edge(int lx, int ly, int w, int h) {
        return lx == 0 || ly == 0 || lx == w - 1 || ly == h - 1;
    }

    private static boolean corner(int lx, int ly, int w, int h) {
        return (lx == 0 || lx == w - 1) && (ly == 0 || ly == h - 1);
    }
}
