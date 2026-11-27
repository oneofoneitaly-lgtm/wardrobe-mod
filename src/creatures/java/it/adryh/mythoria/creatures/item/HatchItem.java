package it.adryh.mythoria.creatures.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;
import java.util.function.Supplier;

/** Eggs and charms: the creature appears already belonging to whoever uses the item. */
public final class HatchItem extends Item {
    private final Supplier<? extends EntityType<? extends TamableAnimal>> creature;

    public HatchItem(Supplier<? extends EntityType<? extends TamableAnimal>> creature, Properties properties) {
        super(properties);
        this.creature = creature;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        TamableAnimal born = creature.get().spawn(level, pos, MobSpawnType.MOB_SUMMONED);
        if (born == null) {
            return InteractionResult.FAIL;
        }
        born.tame(context.getPlayer());
        born.setPersistenceRequired();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, born.getX(), born.getY() + 1.0, born.getZ(), 60, 0.6, 0.8, 0.6, 0.2);
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.NEUTRAL, 1.4F, 0.8F);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
