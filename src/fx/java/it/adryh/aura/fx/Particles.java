package it.adryh.aura.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

final class Particles {
    private static int ticks;

    private Particles() {
    }

    private static SimpleParticleType type(int index) {
        return switch (index) {
            case 1 -> ParticleTypes.HEART;
            case 2 -> ParticleTypes.SOUL_FIRE_FLAME;
            case 3 -> ParticleTypes.ENCHANTED_HIT;
            case 4 -> ParticleTypes.FLAME;
            case 5 -> ParticleTypes.CHERRY_LEAVES;
            case 6 -> ParticleTypes.NOTE;
            default -> ParticleTypes.END_ROD;
        };
    }

    static void hit(AttackEntityEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!FxSettings.HIT.on() || mc.level == null || event.getEntity() != mc.player) {
            return;
        }
        Entity target = event.getTarget();
        RandomSource random = mc.level.random;
        SimpleParticleType type = type(FxSettings.HIT_STYLE.index());
        for (int i = 0; i < FxSettings.HIT_AMOUNT.i(); i++) {
            mc.level.addParticle(type,
                    target.getRandomX(0.6), target.getY() + random.nextDouble() * target.getBbHeight(), target.getRandomZ(0.6),
                    (random.nextDouble() - 0.5) * 0.25, random.nextDouble() * 0.15, (random.nextDouble() - 0.5) * 0.25);
        }
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (!FxSettings.TRAIL.on() || p == null || mc.level == null || mc.isPaused() || ++ticks % 2 != 0) {
            return;
        }
        double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
        if (dx * dx + dz * dz > 0.002) {
            mc.level.addParticle(type(FxSettings.TRAIL_STYLE.index()),
                    p.getRandomX(0.5) - dx * 3, p.getY() + 0.1 + mc.level.random.nextDouble() * 0.4, p.getRandomZ(0.5) - dz * 3,
                    0, 0.01, 0);
        }
    }
}
