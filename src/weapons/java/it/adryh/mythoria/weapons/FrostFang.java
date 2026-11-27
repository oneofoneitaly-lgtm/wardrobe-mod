package it.adryh.mythoria.weapons;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Tier;

/** Slows enemies on hit; right click releases a freezing nova. */
public final class FrostFang extends MythicSword {
    public FrostFang(Tier tier, Properties properties) {
        super(tier, properties, "frost_fang", 120);
    }

    @Override
    protected void onStrike(ServerLevel level, LivingEntity target, LivingEntity attacker) {
        freeze(target, 80, 1);
        level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.6), target.getZ(), 24, 0.3, 0.4, 0.3, 0.05);
    }

    @Override
    protected void power(ServerLevel level, Player player) {
        for (LivingEntity victim : victims(level, player, player.getBoundingBox().inflate(5.5, 2.0, 5.5))) {
            victim.hurt(level.damageSources().playerAttack(player), 5.0F);
            freeze(victim, 120, 3);
        }
        for (int i = 0; i < 90; i++) {
            double angle = i / 90.0 * Math.PI * 2.0;
            double radius = 1.5 + (i % 5);
            level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX() + Math.cos(angle) * radius, player.getY() + 0.4,
                    player.getZ() + Math.sin(angle) * radius, 2, 0.1, 0.25, 0.1, 0.03);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 0.6F);
    }

    private static void freeze(LivingEntity target, int ticks, int strength) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, strength));
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + ticks));
    }
}
