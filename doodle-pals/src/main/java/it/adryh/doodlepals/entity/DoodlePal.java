package it.adryh.doodlepals.entity;

import it.adryh.doodlepals.DoodlePals;
import it.adryh.doodlepals.Drawing;
import it.adryh.doodlepals.Power;
import it.adryh.doodlepals.item.PageItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/** A drawing that came to life. Its body, strength and powers all come from the picture. */
public class DoodlePal extends TamableAnimal {
    private static final EntityDataAccessor<CompoundTag> DRAWING =
            SynchedEntityData.defineId(DoodlePal.class, EntityDataSerializers.COMPOUND_TAG);

    private Drawing drawing = Drawing.EMPTY;
    private int boltCooldown;
    private int heartCooldown;
    /** client animation: counts down after the pal strikes */
    public int lunge;

    public DoodlePal(EntityType<? extends DoodlePal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DRAWING, new CompoundTag());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DRAWING.equals(key)) {
            drawing = Drawing.load(entityData.get(DRAWING));
            refreshDimensions();
        }
    }

    public Drawing drawing() {
        return drawing;
    }

    /** Gives the pal its picture, and with it its body. */
    public void setDrawing(Drawing picture) {
        entityData.set(DRAWING, picture.save());
        drawing = picture;
        refreshDimensions();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(picture.health());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(picture.speed());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(picture.attack());
        getAttribute(Attributes.ARMOR).setBaseValue(picture.armour());
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(picture.level(Power.IRON) * 0.25);
    }

    /** The box follows the painted area: one pixel is a sixteenth of a block. */
    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        if (drawing == null || drawing.painted() == 0) {
            return super.getDefaultDimensions(pose);
        }
        float width = Math.max(0.4F, Math.min(1.3F, drawing.width() / 16.0F * 0.85F));
        float height = Math.max(0.35F, drawing.height() / 16.0F);
        return EntityDimensions.scalable(width, height).withEyeHeight(height * 0.75F);
    }

    // ------------------------------------------------------------------ behaviour
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.2, 6.0F, 2.5F));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof Creeper || target instanceof Ghast) {
            return false;
        }
        return !(target instanceof TamableAnimal pet && pet.getOwner() == owner);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        LivingEntity owner = getOwner();
        if (target == owner || target instanceof TamableAnimal pet && owner != null && pet.getOwner() == owner) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            level().broadcastEntityEvent(this, (byte) 4);
            if (target instanceof LivingEntity living) {
                touch(living);
            }
        }
        return hit;
    }

    /** What the colours add to every hit, melee or magic. */
    private void touch(LivingEntity victim) {
        int fire = drawing.level(Power.FIRE);
        if (fire > 0) {
            victim.igniteForSeconds(2.0F + fire * 2.0F);
        }
        int frost = drawing.level(Power.FROST);
        if (frost > 0) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + frost * 30, frost - 1), this);
            victim.setTicksFrozen(victim.getTicksFrozen() + 60 * frost);
        }
        if (drawing.level(Power.SHADOW) > 0) {
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), this);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (boltCooldown > 0) {
            boltCooldown--;
        }
        if (heartCooldown > 0) {
            heartCooldown--;
        }
        ServerLevel server = (ServerLevel) level();
        magic(server);
        if (tickCount % 20 == 0 && getOwner() instanceof ServerPlayer owner && distanceToSqr(owner) < 1024.0) {
            help(server, owner);
        }
    }

    private void magic(ServerLevel server) {
        int level = drawing.level(Power.MAGIC);
        LivingEntity target = getTarget();
        if (level == 0 || boltCooldown > 0 || target == null || !target.isAlive() || distanceToSqr(target) > 196.0
                || !hasLineOfSight(target)) {
            return;
        }
        boltCooldown = 70 - level * 12;
        Vec3 from = position().add(0.0, getBbHeight() * 0.7, 0.0);
        Vec3 to = target.getBoundingBox().getCenter();
        int steps = (int) (from.distanceTo(to) * 4.0) + 1;
        DustParticleOptions dust = dust(Power.MAGIC, 1.1F);
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.lerp(to, i / (double) steps);
            server.sendParticles(dust, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.0);
        }
        if (target.hurt(damageSources().mobAttack(this), 2.0F + level * 1.5F)) {
            touch(target);
        }
        level().broadcastEntityEvent(this, (byte) 4);
        playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 1.6F);
    }

    /** The helping colours, once a second while the owner is near. */
    private void help(ServerLevel server, ServerPlayer owner) {
        if (drawing.level(Power.SPARK) > 0) {
            give(owner, MobEffects.MOVEMENT_SPEED, drawing.level(Power.SPARK) >= 3 ? 1 : 0);
        }
        if (drawing.level(Power.TIDE) > 0 && owner.isInWater()) {
            give(owner, MobEffects.WATER_BREATHING, 0);
            give(owner, MobEffects.DOLPHINS_GRACE, 0);
        }
        if (drawing.level(Power.AIR) > 0 && owner.fallDistance > 2.0F) {
            give(owner, MobEffects.SLOW_FALLING, 0);
        }
        if (drawing.level(Power.SHADOW) > 0 && owner.isShiftKeyDown()) {
            give(owner, MobEffects.INVISIBILITY, 0);
        }
        if (drawing.level(Power.ROCK) > 0) {
            give(owner, MobEffects.DAMAGE_RESISTANCE, drawing.level(Power.ROCK) >= 3 ? 1 : 0);
        }
        if (drawing.level(Power.IRON) > 0) {
            give(owner, MobEffects.DIG_SPEED, drawing.level(Power.IRON) >= 3 ? 1 : 0);
        }
        int heart = drawing.level(Power.HEART);
        if (heart > 0 && heartCooldown == 0 && owner.getHealth() < owner.getMaxHealth() * 0.5F) {
            heartCooldown = 600 - heart * 100;
            owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80 + heart * 20, 1));
            server.sendParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + 2.1, owner.getZ(), 8, 0.4, 0.2, 0.4, 0.0);
        }
        int nature = drawing.level(Power.NATURE);
        if (nature > 0 && tickCount % (200 - nature * 40) < 20) {
            grow(server, 2 + nature);
        }
    }

    private static void give(ServerPlayer owner, net.minecraft.core.Holder<MobEffect> effect, int amplifier) {
        owner.addEffect(new MobEffectInstance(effect, 60, amplifier, true, false, true));
    }

    private void grow(ServerLevel server, int radius) {
        BlockPos centre = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -1, -radius), centre.offset(radius, 1, radius))) {
            BlockState state = server.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop && random.nextInt(4) == 0
                    && ((BonemealableBlock) crop).isValidBonemealTarget(server, pos, state)) {
                ((BonemealableBlock) crop).performBonemeal(server, random, pos.immutable(), state);
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }

    @Override
    public boolean fireImmune() {
        return drawing != null && drawing.level(Power.FIRE) > 0 || super.fireImmune();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return drawing != null && drawing.level(Power.TIDE) > 0 && source.is(DamageTypes.DROWN) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return drawing.level(Power.AIR) == 0 && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        Vec3 motion = getDeltaMovement();
        if (drawing.level(Power.AIR) > 0 && !onGround() && motion.y < 0.0) {
            setDeltaMovement(motion.multiply(1.0, 0.6, 1.0));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (lunge > 0) {
                lunge--;
            }
            List<Drawing.Level> powers = drawing.powers();
            if (!powers.isEmpty() && random.nextInt(5) == 0) {
                Drawing.Level power = powers.get(random.nextInt(powers.size()));
                level().addParticle(dust(power.power(), 0.5F + power.level() * 0.15F),
                        getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            lunge = 8;
        } else {
            super.handleEntityEvent(id);
        }
    }

    public static DustParticleOptions dust(Power power, float size) {
        int c = power.colour();
        return new DustParticleOptions(new Vector3f((c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F), size);
    }

    // ------------------------------------------------------------------ the owner's hands
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isOwnedBy(player) || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean client = level().isClientSide();
        if (isFood(stack) && getHealth() < getMaxHealth()) {
            if (!client) {
                stack.consume(1, player);
                heal(getMaxHealth() / 2.0F);
                ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY(1.0) + 0.2, getZ(), 5, 0.3, 0.2, 0.3, 0.0);
            }
        } else if (player.isSecondaryUseActive()) {
            if (!client) {
                ItemStack page = PageItem.of(this);
                if (!player.getInventory().add(page)) {
                    player.drop(page, false);
                }
                // a button held a moment too long must not wake the pal straight away
                player.getCooldowns().addCooldown(DoodlePals.PAGE.get(), 20);
                playSound(SoundEvents.BOOK_PAGE_TURN, 1.5F, 1.0F);
                ((ServerLevel) level()).sendParticles(ParticleTypes.POOF, getX(), getY(0.5), getZ(), 20, 0.3, 0.3, 0.3, 0.02);
                discard();
            }
        } else if (!client) {
            setOrderedToSit(!isOrderedToSit());
            navigation.stop();
            setTarget(null);
            player.displayClientMessage(Component.translatable(isOrderedToSit() ? "doodlepals.sit" : "doodlepals.follow",
                    getDisplayName()).withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(client);
    }

    /** Doodles are fed with ink. */
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.INK_SAC) || stack.is(Items.GLOW_INK_SAC);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    /** A pal that falls leaves its page behind, so the drawing is never lost. */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide() && isTame()) {
            spawnAtLocation(PageItem.of(this));
        }
        super.die(source);
    }

    public static int countOf(ServerPlayer player) {
        return player.serverLevel().getEntities(DoodlePals.PAL.get(), pal -> pal.isAlive() && pal.isOwnedBy(player)).size();
    }

    /** Brings the pals of this dimension back to their owner. */
    public static int callAll(ServerPlayer player) {
        List<? extends DoodlePal> pals = player.serverLevel().getEntities(DoodlePals.PAL.get(),
                pal -> pal.isAlive() && pal.isOwnedBy(player));
        for (DoodlePal pal : pals) {
            pal.setOrderedToSit(false);
            pal.setInSittingPose(false);
            pal.teleportTo(player.getX(), player.getY(), player.getZ());
        }
        player.displayClientMessage(Component.translatable("doodlepals.called", pals.size()).withStyle(ChatFormatting.AQUA), true);
        return pals.size();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Drawing", drawing.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Drawing", Tag.TAG_COMPOUND)) {
            float health = getHealth();
            setDrawing(Drawing.load(tag.getCompound("Drawing")));
            if (tag.contains("Health")) {
                health = tag.getFloat("Health");
            }
            setHealth(health);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BOOK_PAGE_TURN;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 280;
    }

    @Override
    public float getVoicePitch() {
        // small doodles squeak, big ones rumble
        return 1.6F - Math.min(1.0F, drawing.painted() / 400.0F) + random.nextFloat() * 0.1F;
    }
}
