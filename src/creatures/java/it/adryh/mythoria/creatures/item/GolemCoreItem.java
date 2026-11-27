package it.adryh.mythoria.creatures.item;

import it.adryh.mythoria.creatures.Creatures;
import it.adryh.mythoria.creatures.entity.CrystalGolem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Wakes up the Crystal Golem. */
public final class GolemCoreItem extends Item {
    public GolemCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            context.getPlayer().displayClientMessage(Component.translatable("mythoria.golem.peaceful").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        CrystalGolem golem = Creatures.GOLEM.get().spawn(level, pos, MobSpawnType.MOB_SUMMONED);
        if (golem == null) {
            return InteractionResult.FAIL;
        }
        golem.setPersistenceRequired();
        level.sendParticles(ParticleTypes.END_ROD, golem.getX(), golem.getY() + 1.5, golem.getZ(), 120, 1.0, 1.5, 1.0, 0.15);
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 0.5F, 1.4F);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.mythoria_creatures.golem_core.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
