package it.adryh.mythoria.creatures.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;
import java.util.function.Supplier;

/** Calls a boss to the spot where it is used. */
public final class BossItem extends Item {
    private final Supplier<? extends EntityType<? extends Mob>> boss;

    public BossItem(Supplier<? extends EntityType<? extends Mob>> boss, Properties properties) {
        super(properties);
        this.boss = boss;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            context.getPlayer().displayClientMessage(Component.translatable("mythoria.boss.peaceful").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Mob mob = boss.get().spawn(level, pos, MobSpawnType.MOB_SUMMONED);
        if (mob == null) {
            return InteractionResult.FAIL;
        }
        mob.setPersistenceRequired();
        level.sendParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 1.5, mob.getZ(), 120, 1.0, 1.5, 1.0, 0.15);
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 0.5F, 1.4F);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
