package it.adryh.mythoria.creatures.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** A little companion: it brings drops to its owner and lets them see in the dark. */
public class SpiritFox extends TamableAnimal {
    private static final ResourceLocation CAKE = ResourceLocation.fromNamespaceAndPath("mythoria_feast", "fairy_cake");

    public SpiritFox(EntityType<? extends SpiritFox> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.2, 5.0F, 2.0F));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SWEET_BERRIES) || stack.is(Items.GLOW_BERRIES) || CAKE.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean client = level().isClientSide();
        if (!isTame()) {
            if (!client) {
                if (isFood(stack)) {
                    stack.consume(1, player);
                    if (random.nextInt(3) == 0) {
                        tame(player);
                        navigation.stop();
                        level().broadcastEntityEvent(this, (byte) 7);
                        say(player, "tamed");
                    } else {
                        level().broadcastEntityEvent(this, (byte) 6);
                    }
                } else {
                    say(player, "wild");
                }
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (isOwnedBy(player)) {
            if (!client) {
                if (isFood(stack) && getHealth() < getMaxHealth()) {
                    stack.consume(1, player);
                    heal(8.0F);
                } else {
                    setOrderedToSit(!isOrderedToSit());
                    navigation.stop();
                    say(player, isOrderedToSit() ? "sit" : "follow");
                }
            }
            return InteractionResult.sidedSuccess(client);
        }
        return super.mobInteract(player, hand);
    }

    private static void say(Player player, String what) {
        player.displayClientMessage(Component.translatable("mythoria.fox." + what).withStyle(ChatFormatting.AQUA), true);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity owner = getOwner();
        if (tickCount % 10 != 0 || !(owner instanceof Player player) || isOrderedToSit() || distanceToSqr(owner) > 256.0) {
            return;
        }
        // light: refreshed well before the effect starts to blink
        if (level().getMaxLocalRawBrightness(player.blockPosition()) < 7) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, true));
        }
        // fetch: nearby drops drift towards the owner
        for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(8.0, 3.0, 8.0),
                item -> !item.hasPickUpDelay() && item.distanceToSqr(player) > 1.5)) {
            Vec3 to = player.position().add(0.0, 0.4, 0.0).subtract(item.position());
            if (to.lengthSqr() < 144.0) {
                item.setDeltaMovement(to.normalize().scale(0.55).add(0.0, 0.12, 0.0));
                item.hurtMarked = true;
                ((ServerLevel) level()).sendParticles(ParticleTypes.END_ROD, item.getX(), item.getY() + 0.2, item.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && random.nextInt(6) == 0) {
            level().addParticle(ParticleTypes.END_ROD, getRandomX(0.9), getRandomY(), getRandomZ(0.9), 0.0, 0.01, 0.0);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, net.minecraft.core.BlockPos pos) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.3F + random.nextFloat() * 0.1F;
    }
}
