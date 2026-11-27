package it.adryh.mythoria.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** The coin of the market. Use it to put all carried Crowns in the wallet. */
public final class CrownItem extends Item {
    public CrownItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            int amount = Wallet.deposit(server);
            server.displayClientMessage(Component.translatable("mythoria.wallet.deposit", amount, Wallet.balance(server))
                    .withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.mythoria.crown.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
