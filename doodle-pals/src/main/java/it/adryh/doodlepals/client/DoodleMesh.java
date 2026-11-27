package it.adryh.doodlepals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.adryh.doodlepals.DoodlePals;
import it.adryh.doodlepals.Drawing;
import it.adryh.doodlepals.Power;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a drawing into a puffy 3d body: the further a pixel is from the outline, the thicker it is.
 * Units are pixels; x is centred, y starts at the feet and the face looks towards +z.
 */
public final class DoodleMesh {
    private static final ResourceLocation WHITE = DoodlePals.id("textures/entity/white.png");
    private static final float[] DEPTH = {0.0F, 1.1F, 1.9F, 2.6F, 3.1F};

    /** group: 0 body, 1 left leg, 2 right leg */
    private record Face(float[] corners, float nx, float ny, float nz, int colour, boolean glows, int group) {}

    private record Eye(float x, float y, float z) {}

    private static final Map<Drawing, DoodleMesh> CACHE = new LinkedHashMap<>(32, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Drawing, DoodleMesh> eldest) {
            return size() > 64;
        }
    };

    private final List<Face> faces = new ArrayList<>();
    private final List<Eye> eyes = new ArrayList<>();

    public static DoodleMesh of(Drawing drawing) {
        return CACHE.computeIfAbsent(drawing, DoodleMesh::new);
    }

    public static RenderType renderType() {
        return RenderType.entityCutoutNoCull(WHITE);
    }

    private DoodleMesh(Drawing drawing) {
        int size = Drawing.SIZE;
        int[][] thick = new int[size][size];
        // distance to the outline, grown ring by ring
        for (int ring = 1; ring < DEPTH.length; ring++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (drawing.get(x, y) != 0 && thick[y][x] == ring - 1
                            && (ring == 1 || at(thick, x - 1, y) >= ring - 1 && at(thick, x + 1, y) >= ring - 1
                            && at(thick, x, y - 1) >= ring - 1 && at(thick, x, y + 1) >= ring - 1)) {
                        thick[y][x] = ring;
                    }
                }
            }
        }
        float centre = (drawing.minX() + drawing.maxX() + 1) / 2.0F;
        int bottom = drawing.maxY() + 1;
        int legRows = drawing.height() >= 8 ? Math.max(2, drawing.height() / 5) : 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Power power = Power.of(drawing.get(x, y));
                if (power == null) {
                    continue;
                }
                float d = DEPTH[thick[y][x]];
                float x0 = x - centre;
                float x1 = x0 + 1;
                float y0 = bottom - y - 1;
                float y1 = y0 + 1;
                // a faint checker so that large areas keep some texture
                int colour = shade(power.colour(), ((x * 7 + y * 13) % 5 - 2) * 0.018F);
                boolean glows = (power == Power.SPARK || power == Power.MAGIC || power == Power.FROST) && drawing.level(power) > 0;
                int group = y >= bottom - legRows ? (x + 0.5F < centre ? 1 : 2) : 0;

                add(colour, glows, group, 0, 0, 1, x0, y0, d, x1, y0, d, x1, y1, d, x0, y1, d);
                add(colour, glows, group, 0, 0, -1, x1, y0, -d, x0, y0, -d, x0, y1, -d, x1, y1, -d);
                for (int side = 0; side < 4; side++) {
                    int ox = x + (side == 0 ? -1 : side == 1 ? 1 : 0);
                    int oy = y + (side == 2 ? -1 : side == 3 ? 1 : 0);
                    float other = drawing.get(ox, oy) == 0 ? 0.0F : DEPTH[thick[oy][ox]];
                    if (other >= d) {
                        continue;
                    }
                    if (other == 0.0F) {
                        side(side, colour, glows, group, x0, x1, y0, y1, -d, d);
                    } else {
                        side(side, colour, glows, group, x0, x1, y0, y1, other, d);
                        side(side, colour, glows, group, x0, x1, y0, y1, -d, -other);
                    }
                }
            }
        }
        for (int at : drawing.eyes()) {
            int x = at % size;
            int y = at / size;
            float z = Math.max(Math.max(DEPTH[thick[y][x]], DEPTH[thick[y][x + 1]]),
                    Math.max(DEPTH[thick[y + 1][x]], DEPTH[thick[y + 1][x + 1]]));
            eyes.add(new Eye(x - centre, bottom - y - 2, z));
        }
    }

    private static int at(int[][] thick, int x, int y) {
        return x < 0 || y < 0 || x >= Drawing.SIZE || y >= Drawing.SIZE ? 0 : thick[y][x];
    }

    private void side(int side, int colour, boolean glows, int group, float x0, float x1, float y0, float y1, float z0, float z1) {
        switch (side) {
            case 0 -> add(colour, glows, group, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
            case 1 -> add(colour, glows, group, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
            case 2 -> add(colour, glows, group, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
            default -> add(colour, glows, group, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        }
    }

    private void add(int colour, boolean glows, int group, float nx, float ny, float nz, float... corners) {
        faces.add(new Face(corners, nx, ny, nz, colour, glows, group));
    }

    private static int shade(int rgb, float amount) {
        int r = Math.max(0, Math.min(255, (int) ((rgb >> 16 & 255) * (1 + amount))));
        int g = Math.max(0, Math.min(255, (int) ((rgb >> 8 & 255) * (1 + amount))));
        int b = Math.max(0, Math.min(255, (int) ((rgb & 255) * (1 + amount))));
        return r << 16 | g << 8 | b;
    }

    /**
     * @param step   phase of the walk
     * @param stride how hard it walks, 0 when still
     * @param blink  true while the eyes are shut
     */
    public void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float step, float stride, boolean blink) {
        VertexConsumer out = buffers.getBuffer(renderType());
        PoseStack.Pose last = pose.last();
        float swing = (float) Math.sin(step) * stride;
        for (Face face : faces) {
            float dz = face.group == 0 ? 0.0F : (face.group == 1 ? swing : -swing) * 1.8F;
            float dy = face.group == 0 ? 0.0F : Math.max(0.0F, face.group == 1 ? swing : -swing) * 1.1F;
            int lit = face.glows ? LightTexture.FULL_BRIGHT : light;
            for (int i = 0; i < 4; i++) {
                vertex(out, last, face.corners[i * 3], face.corners[i * 3 + 1] + dy, face.corners[i * 3 + 2] + dz,
                        face.colour, lit, overlay, face.nx, face.ny, face.nz);
            }
        }
        for (Eye eye : eyes) {
            if (blink) {
                quad(out, last, eye.x, eye.y + 0.8F, eye.x + 2, eye.y + 1.2F, eye.z + 0.12F, 0x20202A, light, overlay);
            } else {
                quad(out, last, eye.x - 0.1F, eye.y - 0.1F, eye.x + 2.1F, eye.y + 2.1F, eye.z + 0.1F, 0x20202A, light, overlay);
                quad(out, last, eye.x + 0.12F, eye.y + 0.12F, eye.x + 1.88F, eye.y + 1.88F, eye.z + 0.16F, 0xFFFFFF, light, overlay);
                quad(out, last, eye.x + 0.55F, eye.y + 0.3F, eye.x + 1.55F, eye.y + 1.4F, eye.z + 0.22F, 0x15151C, light, overlay);
                quad(out, last, eye.x + 0.65F, eye.y + 0.95F, eye.x + 1.0F, eye.y + 1.3F, eye.z + 0.28F, 0xFFFFFF, light, overlay);
            }
        }
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float z,
                             int colour, int light, int overlay) {
        vertex(out, pose, x0, y0, z, colour, light, overlay, 0, 0, 1);
        vertex(out, pose, x1, y0, z, colour, light, overlay, 0, 0, 1);
        vertex(out, pose, x1, y1, z, colour, light, overlay, 0, 0, 1);
        vertex(out, pose, x0, y1, z, colour, light, overlay, 0, 0, 1);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, int colour, int light,
                               int overlay, float nx, float ny, float nz) {
        out.addVertex(pose, x, y, z)
                .setColor(colour >> 16 & 255, colour >> 8 & 255, colour & 255, 255)
                .setUv(0.5F, 0.5F)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
