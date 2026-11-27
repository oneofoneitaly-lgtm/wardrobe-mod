package it.adryh.aura.fx;

import it.adryh.aura.core.Colors;
import it.adryh.aura.core.Draw;
import it.adryh.aura.core.Theme;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;

final class Crosshair {
    private static float spread;
    private static long last = System.currentTimeMillis();

    private Crosshair() {
    }

    /** The vanilla crosshair stays for spectators and for the F3 axis indicator. */
    static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return FxSettings.CROSS.on() && mc.gameMode != null && mc.gameMode.getPlayerMode() != GameType.SPECTATOR
                && !mc.getDebugOverlay().showDebugScreen();
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (!active() || p == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1F, (now - last) / 1000F);
        last = now;
        float charge = p.getAttackStrengthScale(0F);
        float target = 0;
        if (FxSettings.DYNAMIC.on()) {
            double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
            target = (float) Math.min(4, Math.sqrt(dx * dx + dz * dz) * 12) + (1F - charge) * 3F;
        }
        spread = Theme.approach(spread, target, dt);

        float cx = g.guiWidth() / 2F, cy = g.guiHeight() / 2F;
        float size = FxSettings.SIZE.f(), t = FxSettings.THICK.f(), gap = FxSettings.GAP.f() + spread;
        int color = FxSettings.CROSS_COLOR.get();
        boolean border = FxSettings.BORDER.on();
        switch (FxSettings.STYLE.index()) {
            case 0 -> {
                bar(g, cx - t / 2F, cy - gap - size, t, size, color, border);
                bar(g, cx - t / 2F, cy + gap, t, size, color, border);
                bar(g, cx - gap - size, cy - t / 2F, size, t, color, border);
                bar(g, cx + gap, cy - t / 2F, size, t, color, border);
            }
            case 1 -> dot(g, cx, cy, t + size * 0.15F + spread * 0.3F, color, border);
            case 2 -> circle(g, cx, cy, size + spread, t, color, border);
            case 3 -> {
                circle(g, cx, cy, size + spread, t, color, border);
                dot(g, cx, cy, t * 0.9F, color, border);
            }
            case 4 -> chevron(g, cx, cy + spread, size, t, color, border);
            default -> {
                float r = size + spread * 0.6F;
                if (border) {
                    Draw.outline(g, cx - r - 0.5F, cy - r - 0.5F, r * 2 + 1, r * 2 + 1, 1.5F, t + 1F, 0x90000000);
                }
                Draw.outline(g, cx - r, cy - r, r * 2, r * 2, 1F, t, color);
            }
        }
        // attack recharge, replacing the vanilla indicator
        if (charge < 0.999F && mc.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR) {
            float y = cy + Math.max(size + gap, size) + 5;
            Draw.rect(g, cx - 9, y, 18, 2.4F, 1.2F, 0x90000000);
            Draw.rect(g, cx - 9, y, Math.max(2.4F, 18 * charge), 2.4F, 1.2F, color);
        }
    }

    private static void bar(GuiGraphics g, float x, float y, float w, float h, int color, boolean border) {
        if (border) {
            Draw.rect(g, x - 0.5F, y - 0.5F, w + 1, h + 1, 0.8F, 0xA0000000);
        }
        Draw.rect(g, x, y, w, h, 0.4F, color);
    }

    private static void dot(GuiGraphics g, float cx, float cy, float r, int color, boolean border) {
        if (border) {
            Draw.circle(g, cx, cy, r + 0.6F, 0xA0000000);
        }
        Draw.circle(g, cx, cy, r, color);
    }

    private static void circle(GuiGraphics g, float cx, float cy, float r, float t, int color, boolean border) {
        if (border) {
            float o = r + 0.5F;
            Draw.outline(g, cx - o, cy - o, o * 2, o * 2, o, t + 1F, 0x90000000);
        }
        Draw.outline(g, cx - r, cy - r, r * 2, r * 2, r, t, color);
    }

    private static void chevron(GuiGraphics g, float cx, float cy, float size, float t, int color, boolean border) {
        for (int side = -1; side <= 1; side += 2) {
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0F);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(side * -45F));
            bar(g, -t / 2F, 0, t, size * 1.25F, color, border);
            g.pose().popPose();
        }
        Draw.circle(g, cx, cy, t * 0.55F, Colors.alpha(color, 1F));
    }
}
