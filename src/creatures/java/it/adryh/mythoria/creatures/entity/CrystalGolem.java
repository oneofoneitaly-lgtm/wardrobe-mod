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
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/** Boss: smashes with its fists, shakes the ground and becomes faster when badly damaged. */
public class CrystalGolem extends Monster {
    private static final byte PUNCH = 100;
    private static final byte SLAM = 101;
    private static final int SLAM_WINDUP = 18;

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int slamCooldown = 80;
    private int slamDelay;
    /** client animation timers */
    public int punchTicks;
    public int slamTicks;

    public CrystalGolem(EntityType<? extends CrystalGolem> type, Level level) {
        super(type, level);
        xpReward = 80;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 250.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.2);
    }

    /** Very rare in the wild: only in the dark, and not near another golem. */
    public static boolean checkSpawnRules(EntityType<CrystalGolem> type, ServerLevelAccessor level, MobSpawnType reason,
                                          BlockPos pos, RandomSource random) {
        return random.nextInt(25) == 0
                && Monster.checkMonsterSpawnRules(type, level, reason, pos, random)
                && level.getEntitiesOfClass(CrystalGolem.class, new net.minecraft.world.phys.AABB(pos).inflate(128.0)).isEmpty();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        level().broadcastEntityEvent(this, PUNCH);
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.35, 0.0));
            target.hurtMarked = true;
        }
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        boolean enraged = getHealth() < getMaxHealth() / 2.0F;
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(enraged ? 0.31 : 0.24);
        bossBar.setColor(enraged ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.BLUE);
        bossBar.setProgress(getHealth() / getMaxHealth());

        if (slamDelay > 0) {
            if (--slamDelay == 0) {
                shakeGround();
            }
        } else if (--slamCooldown <= 0) {
            LivingEntity target = getTarget();
            if (target != null && distanceToSqr(target) < 49.0) {
                slamDelay = SLAM_WINDUP;
                slamCooldown = enraged ? 70 : 130;
                level().broadcastEntityEvent(this, SLAM);
                playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0F, 0.5F);
            }
        }
    }

    private void shakeGround() {
        ServerLevel server = (ServerLevel) level();
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6.0, 2.0, 6.0),
                e -> e != this && e.isAlive() && !(e instanceof CrystalGolem))) {
            if (victim.hurt(damageSources().mobAttack(this), 9.0F)) {
                victim.knockback(1.4, getX() - victim.getX(), getZ() - victim.getZ());
                victim.setDeltaMovement(victim.getDeltaMovement().add(0.0, 0.45, 0.0));
                victim.hurtMarked = true;
            }
        }
        for (int i = 0; i < 64; i++) {
            double angle = i / 64.0 * Math.PI * 2.0;
            double radius = 2.0 + (i % 4);
            server.sendParticles(ParticleTypes.END_ROD, getX() + Math.cos(angle) * radius, getY() + 0.2,
                    getZ() + Math.sin(angle) * radius, 1, 0.0, 0.1, 0.0, 0.05);
        }
        server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 6, 2.0, 0.1, 2.0, 0.0);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 0.7F);
        playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 3.0F, 0.6F);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == PUNCH) {
            punchTicks = 12;
        } else if (id == SLAM) {
            slamTicks = SLAM_WINDUP + 8;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (punchTicks > 0) {
                punchTicks--;
            }
            if (slamTicks > 0) {
                slamTicks--;
            }
            if (random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.END_ROD, getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0.0, 0.02, 0.0);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide() && isDeadOrDying()) {
            spawnAtLocation(new ItemStack(Mythoria.CROWN.get(), 64));
            spawnAtLocation(new ItemStack(Mythoria.CROWN.get(), 64));
            spawnAtLocation(new ItemStack(Items.DIAMOND, 3 + random.nextInt(4)));
            spawnAtLocation(new ItemStack(Items.AMETHYST_SHARD, 8 + random.nextInt(9)));
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
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
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 0.7F);
    }
}
