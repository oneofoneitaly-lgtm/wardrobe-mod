package it.adryh.mythoria.weapons;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Tier;
import net.minecraft.world.phys.Vec3;

/** Sets enemies on fire and throws a wave of flames. */
public final class PhoenixBlade extends MythicSword {
    public PhoenixBlade(Tier tier, Properties properties) {
        super(tier, properties, "phoenix_blade", 80);
    }

    @Override
    protected void onStrike(ServerLevel level, LivingEntity target, LivingEntity attacker) {
        target.igniteForSeconds(6.0F);
        level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.6), target.getZ(), 24, 0.3, 0.4, 0.3, 0.06);
    }

    @Override
    protected void power(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        for (LivingEntity victim : victims(level, player, player.getBoundingBox().inflate(8.0))) {
            Vec3 to = victim.getBoundingBox().getCenter().subtract(eye);
            if (to.length() < 8.0 && to.normalize().dot(look) > 0.75 && player.hasLineOfSight(victim)) {
                victim.hurt(level.damageSources().playerAttack(player), 7.0F);
                victim.igniteForSeconds(5.0F);
            }
        }
        Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize();
        for (int i = 2; i <= 14; i++) {
            double distance = i * 0.55;
            for (int j = -3; j <= 3; j++) {
                Vec3 at = eye.add(look.scale(distance)).add(side.scale(j * distance * 0.13));
                level.sendParticles(ParticleTypes.FLAME, at.x, at.y - 0.4, at.z, 2, 0.1, 0.15, 0.1, 0.02);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
    }
}
