package it.adryh.mythoria.weapons;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Tier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Sometimes calls lightning on the enemy; right click strikes where the wielder looks. */
public final class StormBlade extends MythicSword {
    public StormBlade(Tier tier, Properties properties) {
        super(tier, properties, "storm_blade", 100);
    }

    @Override
    protected void onStrike(ServerLevel level, LivingEntity target, LivingEntity attacker) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.6), target.getZ(), 20, 0.3, 0.4, 0.3, 0.2);
        if (level.random.nextFloat() < 0.25F) {
            bolt(level, target.position());
            target.hurt(level.damageSources().lightningBolt(), 6.0F);
        }
    }

    @Override
    protected void power(ServerLevel level, Player player) {
        Vec3 point = player.pick(24.0, 1.0F, false).getLocation();
        // an enemy standing near the line of sight attracts the bolt
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double best = eye.distanceTo(point) + 1.0;
        for (LivingEntity victim : victims(level, player, player.getBoundingBox().inflate(24.0))) {
            Vec3 to = victim.getBoundingBox().getCenter().subtract(eye);
            if (to.length() < best && to.normalize().dot(look) > 0.97) {
                best = to.length();
                point = victim.position();
            }
        }
        bolt(level, point);
        for (LivingEntity victim : victims(level, player, new AABB(point, point).inflate(3.0))) {
            victim.hurt(level.damageSources().lightningBolt(), 10.0F);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y + 0.5, point.z, 80, 1.2, 0.8, 1.2, 0.4);
    }

    /** Only for show: no fire, no damage to the wielder. */
    private static void bolt(ServerLevel level, Vec3 at) {
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning != null) {
            lightning.moveTo(at);
            lightning.setVisualOnly(true);
            level.addFreshEntity(lightning);
        }
    }
}
