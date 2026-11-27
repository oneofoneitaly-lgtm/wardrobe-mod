package it.adryh.mythoria.creatures.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A dragon mount: faster and tougher than the griffin, and it breathes fire on command. */
public class EmberDrake extends GoldenGriffin {
    private static final double FIRE_RANGE = 12.0;
    private int breathCooldown;
    private int breathing;

    public EmberDrake(EntityType<? extends EmberDrake> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createDrakeAttributes() {
        return createAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.ARMOR, 6.0);
    }

    @Override
    protected String voice() {
        return "drake";
    }

    @Override
    protected float flightPower() {
        return 1.35F;
    }

    @Override
    protected double tamedHealth() {
        return 110.0;
    }

    @Override
    protected double wildHealth() {
        return 60.0;
    }

    /** The hint names the key bound to the breath, whatever the rider chose. */
    @Override
    protected Component rideHint() {
        return Component.translatable("mythoria.drake.ride", Component.keybind("key.mythoria.mount_ability")).withStyle(ChatFormatting.GOLD);
    }

    /** The rider asked for fire. */
    public void breathe(ServerPlayer rider) {
        if (breathCooldown > 0 || getControllingPassenger() != rider) {
            return;
        }
        breathCooldown = 40;
        breathing = 14;
        playSound(SoundEvents.ENDER_DRAGON_SHOOT, 0.8F, 1.1F);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (breathCooldown > 0) {
            breathCooldown--;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (breathing > 0 && level() instanceof ServerLevel server) {
            breathing--;
            if (getControllingPassenger() instanceof Player rider) {
                fire(server, rider);
            }
        }
    }

    private void fire(ServerLevel server, Player rider) {
        Vec3 look = rider.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z).normalize();
        Vec3 mouth = position().add(flat.scale(2.4)).add(0.0, 1.5, 0.0);
        for (int i = 0; i < 14; i++) {
            Vec3 spread = look.add((random.nextDouble() - 0.5) * 0.35, (random.nextDouble() - 0.5) * 0.35,
                    (random.nextDouble() - 0.5) * 0.35);
            // a count of zero turns the offsets into a velocity
            server.sendParticles(i % 5 == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z, 0,
                    spread.x, spread.y, spread.z, 0.65 + random.nextDouble() * 0.5);
        }
        if (breathing % 4 == 0) {
            for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(FIRE_RANGE),
                    e -> e != this && e != rider && e.isAlive() && !(e instanceof ArmorStand)
                            && !(e instanceof TamableAnimal pet && pet.isOwnedBy(rider))
                            && !(e instanceof Player other && !rider.canHarmPlayer(other)))) {
                Vec3 to = victim.getBoundingBox().getCenter().subtract(mouth);
                if (to.length() < FIRE_RANGE && to.normalize().dot(look) > 0.86) {
                    victim.hurt(damageSources().mobAttack(this), 5.0F);
                    victim.igniteForSeconds(5.0F);
                }
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.35F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 320;
    }

    @Override
    public float getVoicePitch() {
        return 1.25F + random.nextFloat() * 0.15F;
    }
}
