package it.adryh.mythoria.weapons;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A bolt that flies straight where the caster looks and bursts on the first thing it meets. */
public final class ArcaneStaff extends MagicStaff {
    private static final double RANGE = 28.0;

    public ArcaneStaff(Properties properties) {
        super(properties, "arcane_staff", 18);
    }

    @Override
    protected void cast(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = player.pick(RANGE, 1.0F, false).getLocation();
        double best = eye.distanceTo(end);
        LivingEntity target = null;
        for (LivingEntity victim : MythicSword.victims(level, player, player.getBoundingBox().inflate(RANGE))) {
            var hit = victim.getBoundingBox().inflate(0.35).clip(eye, end);
            if (hit.isPresent() && eye.distanceTo(hit.get()) < best) {
                best = eye.distanceTo(hit.get());
                target = victim;
            }
        }
        Vec3 point = eye.add(look.scale(best));
        for (double d = 1.2; d < best; d += 0.45) {
            Vec3 at = eye.add(look.scale(d));
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y - 0.15, at.z, 1, 0.02, 0.02, 0.02, 0.0);
            level.sendParticles(ParticleTypes.ENCHANT, at.x, at.y - 0.15, at.z, 2, 0.1, 0.1, 0.1, 0.2);
        }
        if (target != null) {
            target.hurt(level.damageSources().indirectMagic(player, player), 9.0F);
        }
        for (LivingEntity near : MythicSword.victims(level, player, new AABB(point, point).inflate(2.5))) {
            if (near != target) {
                near.hurt(level.damageSources().indirectMagic(player, player), 4.0F);
            }
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z, 40, 0.4, 0.4, 0.4, 0.12);
        level.sendParticles(ParticleTypes.FLASH, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.5F);
    }
}
