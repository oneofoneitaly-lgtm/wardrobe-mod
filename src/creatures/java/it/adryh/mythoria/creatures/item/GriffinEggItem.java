package it.adryh.mythoria.creatures.item;

import it.adryh.mythoria.creatures.Creatures;
import it.adryh.mythoria.creatures.entity.GoldenGriffin;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Hatches a griffin that already belongs to whoever uses the egg. */
public final class GriffinEggItem extends Item {
    public GriffinEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        GoldenGriffin griffin = Creatures.GRIFFIN.get().spawn(level, pos, MobSpawnType.MOB_SUMMONED);
        if (griffin == null) {
            return InteractionResult.FAIL;
        }
        griffin.tame(context.getPlayer());
        griffin.setPersistenceRequired();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, griffin.getX(), griffin.getY() + 1.0, griffin.getZ(), 60, 0.6, 0.8, 0.6, 0.2);
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.NEUTRAL, 1.4F, 0.8F);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.mythoria_creatures.griffin_egg.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
