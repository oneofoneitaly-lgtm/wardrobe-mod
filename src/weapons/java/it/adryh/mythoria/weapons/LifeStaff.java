package it.adryh.mythoria.weapons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Heals the caster, nearby players and their pets, and makes crops grow. */
public final class LifeStaff extends MagicStaff {
    public LifeStaff(Properties properties) {
        super(properties, "life_staff", 200);
    }

    @Override
    protected void cast(ServerLevel level, Player player) {
        for (LivingEntity friend : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(7.0, 3.0, 7.0),
                e -> e == player || e instanceof Player || e instanceof TamableAnimal pet && pet.isTame())) {
            friend.heal(8.0F);
            friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
            level.sendParticles(ParticleTypes.HEART, friend.getX(), friend.getY(1.0) + 0.3, friend.getZ(), 5, 0.4, 0.2, 0.4, 0.0);
        }
        BlockPos centre = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-5, -1, -5), centre.offset(5, 2, 5))) {
            BlockState state = level.getBlockState(pos);
            var block = state.getBlock();
            if ((block instanceof CropBlock || block instanceof StemBlock || block instanceof SaplingBlock || block instanceof SweetBerryBushBlock)
                    && block instanceof BonemealableBlock plant && plant.isValidBonemealTarget(level, pos, state)
                    && plant.isBonemealSuccess(level, level.random, pos, state)) {
                plant.performBonemeal(level, level.random, pos.immutable(), state);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
            }
        }
        for (int i = 0; i < 80; i++) {
            double angle = i / 80.0 * Math.PI * 2.0;
            double radius = 1.5 + (i % 5);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX() + Math.cos(angle) * radius, player.getY() + 0.3,
                    player.getZ() + Math.sin(angle) * radius, 1, 0.1, 0.2, 0.1, 0.0);
        }
        level.playSound(null, centre, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 1.2F);
    }
}
