package it.adryh.mythoria.weapons;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** A sword with a passive power on hit and an active power on right click. */
public abstract class MythicSword extends SwordItem {
    private final String key;
    private final int cooldown;

    protected MythicSword(Tier tier, Properties properties, String key, int cooldown) {
        super(tier, properties);
        this.key = key;
        this.cooldown = cooldown;
    }

    protected abstract void onStrike(ServerLevel level, LivingEntity target, LivingEntity attacker);

    protected abstract void power(ServerLevel level, Player player);

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel level) {
            onStrike(level, target, attacker);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            power(server, player);
            player.getCooldowns().addCooldown(this, cooldown);
            stack.hurtAndBreak(2, player, LivingEntity.getSlotForHand(hand));
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Creatures a power may hurt around a point: never the wielder, their pets or protected players. */
    protected static List<LivingEntity> victims(ServerLevel level, Player player, AABB box) {
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive()
                && !(e instanceof ArmorStand)
                && !(e instanceof TamableAnimal pet && pet.isOwnedBy(player))
                && !(e instanceof Player other && !player.canHarmPlayer(other)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.mythoria_weapons." + key + ".hit").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.mythoria_weapons." + key + ".use").withStyle(ChatFormatting.AQUA));
    }
}
