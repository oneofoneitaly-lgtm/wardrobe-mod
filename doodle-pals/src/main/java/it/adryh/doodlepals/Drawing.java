package it.adryh.doodlepals;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * A doodle: a square grid of palette codes (row 0 at the top) and up to four eyes,
 * each covering two by two pixels. Immutable.
 */
public final class Drawing {
    public static final int SIZE = 24;
    public static final int CELLS = SIZE * SIZE;
    public static final int MAX_EYES = 4;
    public static final int MIN_PIXELS = 12;

    public static final Drawing EMPTY = new Drawing(new byte[CELLS], new int[0]);

    /** A power and how strong it is, from 1 to 3. */
    public record Level(Power power, int level, float share) {}

    private final byte[] pixels;
    private final int[] eyes;
    private final int painted;
    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;
    private final List<Level> powers;

    /** Anything invalid is dropped: unknown colours, eyes outside the paint. */
    public Drawing(byte[] source, int[] eyeSource) {
        pixels = new byte[CELLS];
        int[] counts = new int[Power.values().length];
        int total = 0;
        int left = SIZE;
        int right = -1;
        int top = SIZE;
        int bottom = -1;
        for (int i = 0; i < CELLS && i < source.length; i++) {
            Power power = Power.of(source[i]);
            if (power == null) {
                continue;
            }
            pixels[i] = source[i];
            counts[power.ordinal()]++;
            total++;
            left = Math.min(left, i % SIZE);
            right = Math.max(right, i % SIZE);
            top = Math.min(top, i / SIZE);
            bottom = Math.max(bottom, i / SIZE);
        }
        painted = total;
        minX = total == 0 ? 0 : left;
        maxX = total == 0 ? 0 : right;
        minY = total == 0 ? 0 : top;
        maxY = total == 0 ? 0 : bottom;

        List<Integer> kept = new ArrayList<>();
        for (int eye : eyeSource) {
            if (kept.size() < MAX_EYES && fitsEye(pixels, eye) && !kept.contains(eye)) {
                kept.add(eye);
            }
        }
        eyes = kept.stream().mapToInt(Integer::intValue).toArray();

        List<Level> found = new ArrayList<>();
        for (Power power : Power.values()) {
            float share = total == 0 ? 0.0F : counts[power.ordinal()] / (float) total;
            if (share >= 0.12F) {
                found.add(new Level(power, share >= 0.65F ? 3 : share >= 0.35F ? 2 : 1, share));
            }
        }
        found.sort(Comparator.comparingDouble((Level l) -> -l.share()));
        powers = List.copyOf(found.subList(0, Math.min(3, found.size())));
    }

    /** An eye needs all four pixels under it painted. */
    public static boolean fitsEye(byte[] pixels, int at) {
        if (at < 0 || at >= CELLS || at % SIZE == SIZE - 1 || at / SIZE == SIZE - 1) {
            return false;
        }
        return pixels[at] != 0 && pixels[at + 1] != 0 && pixels[at + SIZE] != 0 && pixels[at + SIZE + 1] != 0;
    }

    public int get(int x, int y) {
        return x < 0 || y < 0 || x >= SIZE || y >= SIZE ? 0 : pixels[y * SIZE + x];
    }

    public byte[] pixels() {
        return pixels.clone();
    }

    public int[] eyes() {
        return eyes.clone();
    }

    public int painted() {
        return painted;
    }

    public boolean isAlive() {
        return painted >= MIN_PIXELS;
    }

    public int minX() {
        return minX;
    }

    public int maxX() {
        return maxX;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public int width() {
        return painted == 0 ? 0 : maxX - minX + 1;
    }

    public int height() {
        return painted == 0 ? 0 : maxY - minY + 1;
    }

    public List<Level> powers() {
        return powers;
    }

    /** 0 when the drawing lacks that power. */
    public int level(Power power) {
        for (Level level : powers) {
            if (level.power() == power) {
                return level.level();
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ body: big doodles are tough, small ones quick
    public double health() {
        double health = 10.0 + painted * 0.12 + level(Power.ROCK) * 8.0;
        return Math.round(Math.min(80.0, health));
    }

    public double speed() {
        return Math.max(0.22, 0.36 - painted * 0.0003) + level(Power.AIR) * 0.015;
    }

    public double attack() {
        return Math.round(2.0 + painted / 70.0 + level(Power.BRAVE) * 2.5);
    }

    public double armour() {
        return level(Power.IRON) * 4.0;
    }

    // ------------------------------------------------------------------ storage
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putByteArray("Pixels", pixels.clone());
        tag.putIntArray("Eyes", eyes.clone());
        return tag;
    }

    public static Drawing load(CompoundTag tag) {
        return new Drawing(tag.getByteArray("Pixels"), tag.getIntArray("Eyes"));
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Drawing d && Arrays.equals(pixels, d.pixels) && Arrays.equals(eyes, d.eyes);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(pixels) + Arrays.hashCode(eyes);
    }
}
