package it.adryh.mythoria.weapons;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A staff casts its spell on right click, with no ammunition. */
public abstract class MagicStaff extends Item {
    private final String key;
    private final int cooldown;

    protected MagicStaff(Properties properties, String key, int cooldown) {
        super(properties);
        this.key = key;
        this.cooldown = cooldown;
    }

    protected abstract void cast(ServerLevel level, Player player);

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            cast(server, player);
            player.getCooldowns().addCooldown(this, cooldown);
            stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.mythoria_weapons." + key + ".hit").withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("item.mythoria_weapons." + key + ".use").withStyle(ChatFormatting.GRAY));
    }
}
