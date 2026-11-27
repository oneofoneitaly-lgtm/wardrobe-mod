package it.adryh.wardrobe;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Tiny software renderer for the pictures in the wardrobe (works for any model). */
public final class Thumbnails {
    public static final int SIZE = 96;

    private Thumbnails() {}

    public static byte[] render(Outfit outfit) {
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        try {
            List<BufferedImage> textures = new ArrayList<>();
            for (byte[] png : outfit.textures) textures.add(ImageIO.read(new ByteArrayInputStream(png)));
            draw(outfit, textures, out);
        } catch (IOException | RuntimeException e) {
            WardrobeLog.warn("No picture for " + outfit.id + ": " + e);
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(out, "png", bytes);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void draw(Outfit outfit, List<BufferedImage> textures, BufferedImage out) {
        double yaw = Math.toRadians(outfit.slot == Slot.BACK ? 150 : -28);
        double pitch = Math.toRadians(14);
        List<double[]> projected = new ArrayList<>();
        double minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9;
        for (Quad q : outfit.quads) {
            double[] p = new double[12];
            for (int i = 0; i < 4; i++) {
                double x = q.pos[i * 3], y = q.pos[i * 3 + 1], z = q.pos[i * 3 + 2];
                double rx = x * Math.cos(yaw) + z * Math.sin(yaw);
                double rz = -x * Math.sin(yaw) + z * Math.cos(yaw);
                double ry = y * Math.cos(pitch) + rz * Math.sin(pitch);
                rz = -y * Math.sin(pitch) + rz * Math.cos(pitch);
                p[i * 3] = -rx;
                p[i * 3 + 1] = -ry;
                p[i * 3 + 2] = rz;
                minX = Math.min(minX, -rx);
                maxX = Math.max(maxX, -rx);
                minY = Math.min(minY, -ry);
                maxY = Math.max(maxY, -ry);
            }
            projected.add(p);
        }
        double scale = (SIZE - 6) / Math.max(1.0, Math.max(maxX - minX, maxY - minY));
        double dx = SIZE / 2.0 - (minX + maxX) / 2 * scale;
        double dy = SIZE / 2.0 - (minY + maxY) / 2 * scale;
        double[] depth = new double[SIZE * SIZE];
        Arrays.fill(depth, 1e18);
        for (int n = 0; n < projected.size(); n++) {
            double[] p = projected.get(n);
            Quad q = outfit.quads.get(n);
            BufferedImage tex = textures.get(q.texture);
            if (tex == null) continue;
            double nz = -q.nx * Math.sin(yaw) + q.nz * Math.cos(yaw);
            double shade = 0.66 + 0.34 * Math.min(1.0, Math.abs(nz) + Math.max(0, q.ny) * 0.5);
            int[][] triangles = {{0, 1, 2}, {0, 2, 3}};
            for (int[] t : triangles) {
                double x0 = p[t[0] * 3] * scale + dx, y0 = p[t[0] * 3 + 1] * scale + dy;
                double x1 = p[t[1] * 3] * scale + dx, y1 = p[t[1] * 3 + 1] * scale + dy;
                double x2 = p[t[2] * 3] * scale + dx, y2 = p[t[2] * 3 + 1] * scale + dy;
                double den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2);
                if (Math.abs(den) < 1e-9) continue;
                int left = (int) Math.max(0, Math.floor(Math.min(x0, Math.min(x1, x2))));
                int right = (int) Math.min(SIZE - 1, Math.ceil(Math.max(x0, Math.max(x1, x2))));
                int top = (int) Math.max(0, Math.floor(Math.min(y0, Math.min(y1, y2))));
                int bottom = (int) Math.min(SIZE - 1, Math.ceil(Math.max(y0, Math.max(y1, y2))));
                for (int y = top; y <= bottom; y++) {
                    for (int x = left; x <= right; x++) {
                        double l0 = ((y1 - y2) * (x + 0.5 - x2) + (x2 - x1) * (y + 0.5 - y2)) / den;
                        double l1 = ((y2 - y0) * (x + 0.5 - x2) + (x0 - x2) * (y + 0.5 - y2)) / den;
                        double l2 = 1 - l0 - l1;
                        if (l0 < -1e-6 || l1 < -1e-6 || l2 < -1e-6) continue;
                        double z = l0 * p[t[0] * 3 + 2] + l1 * p[t[1] * 3 + 2] + l2 * p[t[2] * 3 + 2];
                        if (z >= depth[y * SIZE + x]) continue;
                        double u = l0 * q.uv[t[0] * 2] + l1 * q.uv[t[1] * 2] + l2 * q.uv[t[2] * 2];
                        double v = l0 * q.uv[t[0] * 2 + 1] + l1 * q.uv[t[1] * 2 + 1] + l2 * q.uv[t[2] * 2 + 1];
                        int tx = Math.max(0, Math.min(tex.getWidth() - 1, (int) (u * tex.getWidth())));
                        // animated textures are taller than wide: use the first frame
                        int ty = Math.max(0, Math.min(tex.getHeight() - 1, (int) (v * tex.getHeight())));
                        int argb = tex.getRGB(tx, ty);
                        if ((argb >>> 24) < 128) continue;
                        depth[y * SIZE + x] = z;
                        int r = (int) (((argb >> 16) & 255) * shade);
                        int g = (int) (((argb >> 8) & 255) * shade);
                        int b = (int) ((argb & 255) * shade);
                        out.setRGB(x, y, 0xFF000000 | r << 16 | g << 8 | b);
                    }
                }
            }
        }
    }
}
