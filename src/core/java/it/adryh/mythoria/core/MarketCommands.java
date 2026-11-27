package it.adryh.mythoria.core;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import it.adryh.mythoria.core.net.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /market opens the shop, /crowns shows the wallet, operators can /crowns give. */
public final class MarketCommands {
    private MarketCommands() {}

    static void register(IEventBus bus) {
        bus.addListener((RegisterCommandsEvent event) -> {
            event.getDispatcher().register(literal("market").executes(c -> {
                Net.openMarket(c.getSource().getPlayerOrException());
                return 1;
            }));
            event.getDispatcher().register(literal("crowns")
                    .executes(c -> balance(c.getSource()))
                    .then(literal("give").requires(s -> s.hasPermission(2))
                            .then(argument("players", EntityArgument.players())
                                    .then(argument("amount", IntegerArgumentType.integer(1, 1_000_000)).executes(c -> {
                                        int amount = IntegerArgumentType.getInteger(c, "amount");
                                        for (ServerPlayer player : EntityArgument.getPlayers(c, "players")) {
                                            Wallet.add(player, amount);
                                            player.sendSystemMessage(Component.translatable("mythoria.wallet.received", amount, Wallet.balance(player))
                                                    .withStyle(ChatFormatting.GOLD));
                                        }
                                        return amount;
                                    })))));
        });
    }

    private static int balance(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.translatable("mythoria.wallet.balance", Wallet.balance(player)).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }
}
