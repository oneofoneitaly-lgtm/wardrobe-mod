package it.adryh.mythoria.creatures.entity;

import it.adryh.mythoria.core.Mythoria;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Boss: casts shadow bolts, raises skeletons and slips away when cornered. */
public class ShadowLich extends Monster {
    private static final byte CAST = 100;
    private static final double BAR_RANGE = 48.0;

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
    private int boltCooldown = 40;
    private int summonCooldown = 60;
    private int blinkCooldown = 60;
    /** client animation timer */
    public int castTicks;

    public ShadowLich(EntityType<? extends ShadowLich> type, Level level) {
        super(type, level);
        xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.2);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MoveTowardsTargetGoal(this, 0.9, 32.0F));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        ServerLevel server = (ServerLevel) level();
        boolean enraged = getHealth() < getMaxHealth() / 2.0F;
        bossBar.setColor(enraged ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.GREEN);
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (--boltCooldown <= 0 && distanceToSqr(target) < 576.0 && hasLineOfSight(target)) {
            boltCooldown = enraged ? 30 : 55;
            bolt(server, target);
        }
        if (--summonCooldown <= 0) {
            summonCooldown = enraged ? 200 : 300;
            raise(server, target);
        }
        if (--blinkCooldown <= 0 && distanceToSqr(target) < 16.0) {
            blinkCooldown = 80;
            blink(server);
        }
    }

    private void bolt(ServerLevel server, LivingEntity target) {
        level().broadcastEntityEvent(this, CAST);
        Vec3 from = position().add(0.0, 2.4, 0.0);
        Vec3 to = target.getBoundingBox().getCenter();
        int steps = (int) (from.distanceTo(to) * 3.0) + 1;
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.lerp(to, i / (double) steps);
            server.sendParticles(ParticleTypes.SCULK_SOUL, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
            server.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        if (target.hurt(damageSources().indirectMagic(this, this), 8.0F)) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), this);
        }
        playSound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.7F);
    }

    private void raise(ServerLevel server, LivingEntity target) {
        if (server.getEntitiesOfClass(AbstractSkeleton.class, getBoundingBox().inflate(20.0)).size() >= 4) {
            return;
        }
        level().broadcastEntityEvent(this, CAST);
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            BlockPos pos = BlockPos.containing(getX() + Math.cos(angle) * 3.5, getY(), getZ() + Math.sin(angle) * 3.5);
            var minion = EntityType.SKELETON.spawn(server, pos, MobSpawnType.MOB_SUMMONED);
            if (minion != null) {
                // a helmet keeps it from burning in daylight
                minion.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                minion.setDropChance(EquipmentSlot.HEAD, 0.0F);
                minion.setTarget(target);
                server.sendParticles(ParticleTypes.SOUL, minion.getX(), minion.getY() + 1.0, minion.getZ(), 20, 0.4, 0.8, 0.4, 0.03);
            }
        }
        playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 2.0F, 0.8F);
    }

    private void blink(ServerLevel server) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 7.0 + random.nextDouble() * 4.0;
            server.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.2, getZ(), 30, 0.5, 1.0, 0.5, 0.2);
            if (randomTeleport(getX() + Math.cos(angle) * distance, getY() + 1.0, getZ() + Math.sin(angle) * distance, true)) {
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5F, 0.6F);
                return;
            }
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.WITHER) && super.canBeAffected(effect);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == CAST) {
            castTicks = 16;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            updateBossBar(server);
        } else {
            if (castTicks > 0) {
                castTicks--;
            }
            if (random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.SOUL, getRandomX(0.9), getY() + random.nextDouble() * 0.6, getRandomZ(0.9), 0.0, 0.02, 0.0);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide() && isDeadOrDying()) {
            spawnAtLocation(new ItemStack(Mythoria.CROWN.get(), 64));
            spawnAtLocation(new ItemStack(Mythoria.CROWN.get(), 64));
            spawnAtLocation(new ItemStack(Mythoria.CROWN.get(), 32));
            spawnAtLocation(new ItemStack(Items.EMERALD, 6 + random.nextInt(7)));
            spawnAtLocation(new ItemStack(Items.ENDER_PEARL, 2 + random.nextInt(4)));
        }
    }

    private void updateBossBar(ServerLevel server) {
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 10 != 1) {
            return;
        }
        for (ServerPlayer player : server.players()) {
            if (isAlive() && distanceToSqr(player) < BAR_RANGE * BAR_RANGE) {
                bossBar.addPlayer(player);
            } else {
                bossBar.removePlayer(player);
            }
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        bossBar.removeAllPlayers();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (hasCustomName()) {
            bossBar.setName(getDisplayName());
        }
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossBar.setName(getDisplayName());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F + random.nextFloat() * 0.1F;
    }
}
