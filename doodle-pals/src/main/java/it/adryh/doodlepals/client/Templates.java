package it.adryh.doodlepals.client;

import it.adryh.doodlepals.Drawing;
import it.adryh.doodlepals.Power;

import java.util.ArrayList;
import java.util.List;

/** Starting ideas for those who face a blank page. */
final class Templates {
    private Templates() {}

    static Drawing get(int index) {
        return switch (Math.floorMod(index, 4)) {
            case 0 -> dragon();
            case 1 -> slime();
            case 2 -> cat();
            default -> ghost();
        };
    }

    static int count() {
        return 4;
    }

    /** Paints shapes one over the other. */
    private static final class Canvas {
        private final byte[] pixels = new byte[Drawing.CELLS];

        Canvas dot(Power power, int x, int y) {
            if (x >= 0 && y >= 0 && x < Drawing.SIZE && y < Drawing.SIZE) {
                pixels[y * Drawing.SIZE + x] = (byte) power.code();
            }
            return this;
        }

        Canvas box(Power power, int x0, int y0, int x1, int y1) {
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    dot(power, x, y);
                }
            }
            return this;
        }

        Canvas oval(Power power, float cx, float cy, float rx, float ry) {
            for (int y = 0; y < Drawing.SIZE; y++) {
                for (int x = 0; x < Drawing.SIZE; x++) {
                    float dx = (x + 0.5F - cx) / rx;
                    float dy = (y + 0.5F - cy) / ry;
                    if (dx * dx + dy * dy <= 1.0F) {
                        dot(power, x, y);
                    }
                }
            }
            return this;
        }

        /** the same on both sides of the middle */
        Canvas pair(Power power, int x0, int y0, int x1, int y1) {
            return box(power, x0, y0, x1, y1).box(power, Drawing.SIZE - 1 - x1, y0, Drawing.SIZE - 1 - x0, y1);
        }

        Drawing done(int... eyes) {
            List<Integer> at = new ArrayList<>();
            for (int i = 0; i + 1 < eyes.length; i += 2) {
                at.add(eyes[i + 1] * Drawing.SIZE + eyes[i]);
            }
            return new Drawing(pixels, at.stream().mapToInt(Integer::intValue).toArray());
        }
    }

    private static Drawing dragon() {
        Canvas c = new Canvas();
        // wings
        c.pair(Power.BRAVE, 2, 8, 6, 12).pair(Power.BRAVE, 1, 7, 4, 8).pair(Power.BRAVE, 1, 5, 2, 7).pair(Power.BRAVE, 3, 13, 5, 13);
        // body and head
        c.oval(Power.FIRE, 12, 16, 5.5F, 5.5F).oval(Power.FIRE, 12, 8.5F, 6, 5);
        c.pair(Power.SPARK, 7, 2, 7, 4).pair(Power.SPARK, 8, 3, 8, 4);
        c.oval(Power.SPARK, 12, 17, 3, 4);
        // feet
        c.pair(Power.FIRE, 7, 20, 9, 23);
        return c.done(8, 7, 14, 7);
    }

    private static Drawing slime() {
        Canvas c = new Canvas();
        c.oval(Power.NATURE, 12, 18, 9, 6.5F).oval(Power.NATURE, 12, 15, 6.5F, 5).box(Power.NATURE, 4, 19, 19, 22);
        c.box(Power.TIDE, 11, 8, 12, 10).dot(Power.TIDE, 13, 7).dot(Power.TIDE, 14, 7);
        c.pair(Power.HEART, 6, 18, 7, 18);
        return c.done(8, 15, 14, 15);
    }

    private static Drawing cat() {
        Canvas c = new Canvas();
        c.pair(Power.IRON, 6, 3, 7, 6).pair(Power.IRON, 8, 5, 8, 6).pair(Power.HEART, 7, 5, 7, 6);
        c.oval(Power.AIR, 12, 10, 6.5F, 5).oval(Power.AIR, 12, 17, 4.5F, 5);
        c.pair(Power.AIR, 8, 20, 9, 23);
        c.box(Power.HEART, 11, 12, 12, 12);
        c.box(Power.IRON, 16, 18, 18, 19).box(Power.IRON, 18, 13, 19, 18);
        c.box(Power.SPARK, 9, 15, 14, 15).box(Power.SPARK, 11, 16, 12, 16);
        return c.done(8, 8, 14, 8);
    }

    private static Drawing ghost() {
        Canvas c = new Canvas();
        c.oval(Power.MAGIC, 12, 11, 7, 7).box(Power.MAGIC, 5, 11, 18, 19);
        c.box(Power.MAGIC, 5, 20, 6, 22).box(Power.MAGIC, 9, 20, 10, 21).box(Power.MAGIC, 13, 20, 14, 21).box(Power.MAGIC, 17, 20, 18, 22);
        c.oval(Power.FROST, 12, 17, 4, 3.5F);
        c.pair(Power.FROST, 3, 13, 4, 15);
        return c.done(8, 9, 14, 9);
    }
}
