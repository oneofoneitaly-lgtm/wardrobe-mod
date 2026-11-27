package it.adryh.mythoria.creatures.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.BooleanSupplier;

/** Wild it roams peacefully; tamed it follows, fights for its owner and carries them through the sky. */
public class GoldenGriffin extends TamableAnimal {
    /** Set by the client: is the local player holding the jump key? */
    public static BooleanSupplier jumpHeld = () -> false;

    private static final ResourceLocation DRUMSTICK = ResourceLocation.fromNamespaceAndPath("mythoria_feast", "dragon_drumstick");

    private float fly;
    private float flyOld;
    private float wing;
    private float wingOld;

    public GoldenGriffin(EntityType<? extends GoldenGriffin> type, Level level) {
        super(type, level);
    }

    /** Middle part of the translation keys of this mount. */
    protected String voice() {
        return "griffin";
    }

    /** How fast this mount flies compared to the griffin. */
    protected float flightPower() {
        return 1.0F;
    }

    protected double tamedHealth() {
        return 70.0;
    }

    protected double wildHealth() {
        return 40.0;
    }

    private Component say(String what, ChatFormatting colour) {
        return Component.translatable("mythoria." + voice() + "." + what).withStyle(colour);
    }

    /** The hint shown when the rider gets on. */
    protected Component rideHint() {
        return say("ride", ChatFormatting.GOLD);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, true));
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.25, 8.0F, 3.0F));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
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
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.BEEF) || stack.is(Items.COOKED_BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.COOKED_PORKCHOP)
                || stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN) || stack.is(Items.MUTTON) || stack.is(Items.COOKED_MUTTON)
                || DRUMSTICK.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    protected void applyTamingSideEffects() {
        double health = isTame() ? tamedHealth() : wildHealth();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        if (isTame()) {
            setHealth((float) health);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean client = level().isClientSide();
        if (!isTame()) {
            if (isFood(stack)) {
                if (!client) {
                    stack.consume(1, player);
                    if (random.nextInt(3) == 0) {
                        tame(player);
                        navigation.stop();
                        setTarget(null);
                        level().broadcastEntityEvent(this, (byte) 7);
                        player.displayClientMessage(say("tamed", ChatFormatting.GOLD), true);
                    } else {
                        level().broadcastEntityEvent(this, (byte) 6);
                    }
                }
                return InteractionResult.sidedSuccess(client);
            }
            if (!client) {
                player.displayClientMessage(say("wild", ChatFormatting.YELLOW), true);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (isOwnedBy(player)) {
            if (isFood(stack) && getHealth() < getMaxHealth()) {
                if (!client) {
                    stack.consume(1, player);
                    heal(10.0F);
                }
                return InteractionResult.sidedSuccess(client);
            }
            if (player.isSecondaryUseActive()) {
                if (!client) {
                    setOrderedToSit(!isOrderedToSit());
                    navigation.stop();
                    setTarget(null);
                    player.displayClientMessage(say(isOrderedToSit() ? "sit" : "follow", ChatFormatting.GOLD), true);
                }
                return InteractionResult.sidedSuccess(client);
            }
            if (!isVehicle()) {
                if (!client) {
                    setOrderedToSit(false);
                    setInSittingPose(false);
                    player.startRiding(this);
                    player.displayClientMessage(rideHint(), true);
                }
                return InteractionResult.sidedSuccess(client);
            }
        }
        return super.mobInteract(player, hand);
    }

    // ------------------------------------------------------------------ riding and flying
    /** No gravity while ridden: the flight is steered by hand, and servers would otherwise kick the rider for flying. */
    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!level().isClientSide() && passenger instanceof Player player && isOwnedBy(player)) {
            setNoGravity(true);
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && passenger instanceof Player player) {
            setNoGravity(false);
            // jumping off high in the sky: float down safely
            if (level().noCollision(this, getBoundingBox().expandTowards(0.0, -4.0, 0.0))) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 300));
            }
        }
    }

    @Override
    public LivingEntity getControllingPassenger() {
        if (getFirstPassenger() instanceof Player player && isOwnedBy(player)) {
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        setRot(player.getYRot(), player.getXRot() * 0.4F);
        yRotO = getYRot();
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        float forward = player.zza < 0 ? player.zza * 0.35F : player.zza;
        return new Vec3(player.xxa * 0.5F, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED) * (player.isSprinting() ? 1.4F : 1.0F);
    }

    @Override
    public void travel(Vec3 input) {
        if (isVehicle() && getControllingPassenger() instanceof Player player && !isInWater()) {
            boolean up = level().isClientSide() && jumpHeld.getAsBoolean();
            if (up || !onGround()) {
                moveRelative((player.isSprinting() ? 0.085F : 0.055F) * flightPower(), input);
                Vec3 motion = getDeltaMovement();
                double vertical = up ? 0.34 : -0.07;
                float pitch = player.getXRot();   // positive: looking down
                if (!up && input.z > 0.1 && Math.abs(pitch) > 12.0F) {
                    vertical = -pitch / 90.0F * 0.65;
                }
                motion = new Vec3(motion.x, motion.y + (vertical - motion.y) * 0.3, motion.z);
                setDeltaMovement(motion);
                move(MoverType.SELF, getDeltaMovement());
                setDeltaMovement(getDeltaMovement().multiply(0.91, 0.98, 0.91));
                calculateEntityAnimation(false);
                return;
            }
        }
        super.travel(input);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // wings slow any fall
        Vec3 motion = getDeltaMovement();
        if (!isVehicle() && !onGround() && motion.y < 0.0) {
            setDeltaMovement(motion.multiply(1.0, 0.65, 1.0));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            // the ground check also covers a resting griffin whose "on ground" state has not reached the client yet
            boolean airborne = !onGround() && !isInWater() && !isPassenger()
                    && level().noCollision(this, getBoundingBox().move(0.0, -0.6, 0.0));
            flyOld = fly;
            fly = Mth.clamp(fly + (airborne ? 0.12F : -0.15F), 0.0F, 1.0F);
            wingOld = wing;
            wing += airborne ? (getDeltaMovement().y < -0.02 ? 0.22F : 0.42F) : 0.0F;
            if (airborne && isVehicle() && tickCount % 14 == 0) {
                level().playLocalSound(getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_FLAP, getSoundSource(), 0.35F, 1.4F, false);
            }
            if (isTame() && random.nextInt(14) == 0) {
                level().addParticle(ParticleTypes.WAX_ON, getRandomX(0.9), getRandomY(), getRandomZ(0.9), 0.0, 0.01, 0.0);
            }
        }
    }

    public float flyAmount(float partial) {
        return Mth.lerp(partial, flyOld, fly);
    }

    public float wingTime(float partial) {
        return Mth.lerp(partial, wingOld, wing);
    }

    // ------------------------------------------------------------------ sounds
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F + random.nextFloat() * 0.1F;
    }
}
