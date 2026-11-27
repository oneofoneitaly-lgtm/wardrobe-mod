package it.adryh.mythoria.weapons;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Tier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/** Steals life on hit; right click dashes forward through enemies. */
public final class VoidReaver extends MythicSword {
    private static final double REACH = 9.0;

    public VoidReaver(Tier tier, Properties properties) {
        super(tier, properties, "void_reaver", 90);
    }

    @Override
    protected void onStrike(ServerLevel level, LivingEntity target, LivingEntity attacker) {
        attacker.heal(2.0F);
        level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY(0.6), target.getZ(), 10, 0.3, 0.4, 0.3, 0.04);
    }

    @Override
    protected void power(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 start = player.position();
        // the farthest point along the way where the wielder fits
        Vec3 end = start;
        for (double d = 0.5; d <= REACH; d += 0.5) {
            // never downwards: looking at the ground must not stop the dash
            Vec3 at = start.add(look.x * d, Math.max(0.0, look.y) * d, look.z * d);
            AABB box = player.getBoundingBox().move(at.subtract(start));
            if (!level.noCollision(player, box)) {
                break;
            }
            end = at;
        }
        Set<LivingEntity> hit = new HashSet<>();
        int steps = 18;
        for (int i = 0; i <= steps; i++) {
            Vec3 at = start.lerp(end, i / (double) steps);
            level.sendParticles(ParticleTypes.PORTAL, at.x, at.y + 1.0, at.z, 12, 0.25, 0.5, 0.25, 0.1);
            level.sendParticles(ParticleTypes.WITCH, at.x, at.y + 1.0, at.z, 3, 0.2, 0.4, 0.2, 0.0);
            hit.addAll(victims(level, player, player.getBoundingBox().move(at.subtract(start)).inflate(0.8)));
        }
        for (LivingEntity victim : hit) {
            victim.hurt(level.damageSources().playerAttack(player), 8.0F);
        }
        if (player instanceof ServerPlayer server && !player.isPassenger()) {
            server.teleportTo(end.x, end.y, end.z);
            server.resetFallDistance();
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.7F);
    }
}
