package it.adryh.aura.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.adryh.aura.core.Colors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Random;

/** Draws the gradient, aurora and stars right after the vanilla sky, behind the world. */
final class SkyRenderer {
    private static final int SEGMENTS = 48;
    private static final int MAX_STARS = 1500;
    private static final float[][] STARS = new float[MAX_STARS][5];

    static {
        Random r = new Random(20250101L);
        for (float[] star : STARS) {
            float azimuth = r.nextFloat() * 6.2831855F;
            float height = 0.03F + r.nextFloat() * 0.97F;
            float flat = (float) Math.sqrt(1 - height * height);
            star[0] = (float) Math.cos(azimuth) * flat;
            star[1] = height;
            star[2] = (float) Math.sin(azimuth) * flat;
            star[3] = 0.08F + r.nextFloat() * r.nextFloat() * 0.30F;
            star[4] = r.nextFloat() * 6.2831855F;
        }
    }

    private SkyRenderer() {
    }

    static boolean hasSky(ClientLevel level) {
        return level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL;
    }

    /** How bright the world currently is, used to darken the colours at night. */
    static float daylight(ClientLevel level) {
        return 1F - night(level) * 0.78F;
    }

    /** The horizon colour, warmed up while the sun rises or sets. */
    static int horizon(ClientLevel level) {
        int base = Colors.lerp(0xFF000000, SkySettings.horizon(), daylight(level));
        float[] sun = level.effects().getSunriseColor(level.getTimeOfDay(1F), 1F);
        if (sun == null) {
            return base;
        }
        int warm = 0xFF000000 | (Math.round(sun[0] * 255F) << 16) | (Math.round(sun[1] * 255F) << 8) | Math.round(sun[2] * 255F);
        return Colors.lerp(base, warm, sun[3] * 0.9F);
    }

    private static float night(ClientLevel level) {
        return Math.min(1F, level.getStarBrightness(1F) * 2F);
    }

    static void render(Matrix4f matrix, Camera camera) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !hasSky(level) || camera.getFluidInCamera() != FogType.NONE
                || camera.getEntity() instanceof LivingEntity e && (e.hasEffect(MobEffects.BLINDNESS) || e.hasEffect(MobEffects.DARKNESS))) {
            return;
        }
        float time = (System.currentTimeMillis() % 3_600_000L) / 1000F;
        float clear = 1F - level.getRainLevel(1F);
        float night = SkySettings.NIGHT_ONLY.on() ? night(level) : 1F;
        float visible = night * clear;

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        if (SkySettings.SKY.on()) {
            RenderSystem.defaultBlendFunc();
            dome(matrix, scale(SkySettings.zenith(), daylight(level)), horizon(level),
                    SkySettings.INTENSITY.f() / 100F * (0.35F + 0.65F * clear));
        }
        // light is added on top of the sky
        RenderSystem.blendFunc(770, 1);
        if (SkySettings.STARS.on() && visible > 0.02F) {
            stars(matrix, time, visible);
        }
        if (SkySettings.SHOOTING.on() && visible > 0.2F) {
            shooting(matrix, time, visible);
        }
        if (SkySettings.AURORA.on() && visible > 0.02F) {
            aurora(matrix, time, visible * SkySettings.AURORA_POWER.f() / 100F);
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static BufferBuilder begin() {
        return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    private static void end(BufferBuilder buffer) {
        MeshData mesh = buffer.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }

    private static void vertex(BufferBuilder b, Matrix4f m, float azimuth, float elevation, float radius, int color) {
        float flat = (float) Math.cos(elevation) * radius;
        b.addVertex(m, (float) Math.cos(azimuth) * flat, (float) Math.sin(elevation) * radius, (float) Math.sin(azimuth) * flat)
                .setColor(color);
    }

    private static void dome(Matrix4f m, int zenith, int horizon, float intensity) {
        // elevation in degrees, mix towards the zenith, opacity
        float[][] bands = {{-14, 0F, 0F}, {-3, 0F, 0.75F}, {6, 0.08F, 0.95F}, {20, 0.45F, 0.90F}, {45, 0.85F, 0.85F}, {90, 1F, 0.85F}};
        BufferBuilder b = begin();
        for (int band = 0; band < bands.length - 1; band++) {
            float[] low = bands[band], high = bands[band + 1];
            int lowColor = Colors.alpha(Colors.lerp(horizon, zenith, low[1]), low[2] * intensity);
            int highColor = Colors.alpha(Colors.lerp(horizon, zenith, high[1]), high[2] * intensity);
            float e0 = (float) Math.toRadians(low[0]), e1 = (float) Math.toRadians(high[0]);
            for (int i = 0; i < SEGMENTS; i++) {
                float a0 = i * 6.2831855F / SEGMENTS, a1 = (i + 1) * 6.2831855F / SEGMENTS;
                vertex(b, m, a0, e0, 100F, lowColor);
                vertex(b, m, a1, e0, 100F, lowColor);
                vertex(b, m, a1, e1, 100F, highColor);
                vertex(b, m, a0, e1, 100F, highColor);
            }
        }
        end(b);
    }

    private static int scale(int color, float k) {
        return 0xFF000000 | (Math.round(((color >> 16) & 255) * k) << 16) | (Math.round(((color >> 8) & 255) * k) << 8)
                | Math.round((color & 255) * k);
    }

    private static void stars(Matrix4f m, float time, float visible) {
        BufferBuilder b = begin();
        int count = SkySettings.STAR_COUNT.i();
        float turn = time * 0.004F;
        float cos = (float) Math.cos(turn), sin = (float) Math.sin(turn);
        Vector3f dir = new Vector3f(), side = new Vector3f(), up = new Vector3f();
        for (int i = 0; i < count; i++) {
            float[] s = STARS[i];
            dir.set(s[0] * cos - s[2] * sin, s[1], s[0] * sin + s[2] * cos);
            side.set(0F, 1F, 0F).cross(dir);
            if (side.lengthSquared() < 1.0E-4F) {
                side.set(1F, 0F, 0F);
            }
            side.normalize();
            up.set(dir).cross(side).normalize();
            float twinkle = 0.55F + 0.45F * (float) Math.sin(time * (1.2F + s[3] * 6F) + s[4]);
            float size = s[3] * (0.8F + 0.3F * twinkle);
            int tint = i % 7 == 0 ? 0xFFD6A5 : i % 5 == 0 ? 0xA5C8FF : 0xFFFFFF;
            int color = Colors.alpha(tint, visible * twinkle);
            float x = dir.x * 90F, y = dir.y * 90F, z = dir.z * 90F;
            // a diamond looks like a point of light
            b.addVertex(m, x + side.x * size, y + side.y * size, z + side.z * size).setColor(color);
            b.addVertex(m, x + up.x * size, y + up.y * size, z + up.z * size).setColor(color);
            b.addVertex(m, x - side.x * size, y - side.y * size, z - side.z * size).setColor(color);
            b.addVertex(m, x - up.x * size, y - up.y * size, z - up.z * size).setColor(color);
        }
        end(b);
    }

    private static void shooting(Matrix4f m, float time, float visible) {
        BufferBuilder b = begin();
        boolean any = false;
        for (int n = 0; n < 3; n++) {
            float period = 5.5F + n * 3.1F;
            float t = (time + n * 2.3F) % period;
            if (t > 1.1F) {
                continue;
            }
            Random r = new Random((long) ((time + n * 2.3F) / period) * 31 + n);
            float azimuth = r.nextFloat() * 6.2831855F;
            float elevation = 0.55F + r.nextFloat() * 0.6F;
            float drift = (r.nextBoolean() ? 1 : -1) * (0.35F + r.nextFloat() * 0.25F);
            float head = t / 1.1F;
            float tail = Math.max(0F, head - 0.28F);
            float alpha = visible * (float) Math.sin(head * Math.PI);
            int bright = Colors.alpha(0xFFFFFF, alpha);
            float ha = azimuth + drift * head, he = elevation - 0.28F * head;
            float ta = azimuth + drift * tail, te = elevation - 0.28F * tail;
            vertex(b, m, ta, te, 88F, 0x00FFFFFF);
            vertex(b, m, ha, he - 0.004F, 88F, bright);
            vertex(b, m, ha, he + 0.004F, 88F, bright);
            vertex(b, m, ta, te + 0.0005F, 88F, 0x00FFFFFF);
            any = true;
        }
        if (any) {
            end(b);
        } else {
            b.build();
        }
    }

    private static void aurora(Matrix4f m, float time, float power) {
        int low = SkySettings.AURORA_LOW.get();
        int high = SkySettings.AURORA_HIGH.get();
        BufferBuilder b = begin();
        int steps = 96;
        for (int ribbon = 0; ribbon < 3; ribbon++) {
            float start = ribbon * 2.2F + 0.4F;
            float length = 3.4F - ribbon * 0.4F;
            for (int i = 0; i < steps; i++) {
                float[] left = ribbonPoint(ribbon, start, length, i / (float) steps, time);
                float[] right = ribbonPoint(ribbon, start, length, (i + 1) / (float) steps, time);
                // two stacked strips: a bright lower edge fading up into the second colour
                int l0 = Colors.alpha(low, left[2] * power), r0 = Colors.alpha(low, right[2] * power);
                int l1 = Colors.alpha(Colors.lerp(low, high, 0.55F), left[2] * power * 0.45F);
                int r1 = Colors.alpha(Colors.lerp(low, high, 0.55F), right[2] * power * 0.45F);
                int clear = Colors.alpha(high, 0F);
                float lm = left[1] + left[3] * 0.35F, rm = right[1] + right[3] * 0.35F;
                quad(b, m, left[0], right[0], left[1] - 0.025F, right[1] - 0.025F, left[1], right[1], clear, clear, r0, l0);
                quad(b, m, left[0], right[0], left[1], right[1], lm, rm, l0, r0, r1, l1);
                quad(b, m, left[0], right[0], lm, rm, left[1] + left[3], right[1] + right[3], l1, r1, clear, clear);
            }
        }
        end(b);
    }

    /** azimuth, base elevation, brightness, height */
    private static float[] ribbonPoint(int ribbon, float start, float length, float k, float time) {
        float azimuth = start + k * length;
        float elevation = 0.30F + ribbon * 0.14F
                + 0.10F * (float) Math.sin(azimuth * 2.7F + time * 0.22F + ribbon)
                + 0.045F * (float) Math.sin(azimuth * 7.1F - time * 0.41F);
        float rays = 0.62F + 0.38F * (float) (Math.sin(azimuth * 29F + time * 0.9F) * Math.sin(azimuth * 11F - time * 0.5F + ribbon));
        float fade = (float) Math.sin(Math.PI * k);
        float height = 0.30F + 0.14F * (float) Math.sin(azimuth * 5F + time * 0.3F + ribbon * 2F);
        return new float[]{azimuth, elevation, Math.max(0F, rays * fade * 0.8F), height};
    }

    private static void quad(BufferBuilder b, Matrix4f m, float a0, float a1, float low0, float low1, float high0, float high1,
                             int c00, int c10, int c11, int c01) {
        vertex(b, m, a0, low0, 95F, c00);
        vertex(b, m, a1, low1, 95F, c10);
        vertex(b, m, a1, high1, 95F, c11);
        vertex(b, m, a0, high0, 95F, c01);
    }
}
