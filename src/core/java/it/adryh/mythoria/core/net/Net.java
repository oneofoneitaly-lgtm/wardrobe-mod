package it.adryh.mythoria.core.net;

import it.adryh.mythoria.core.Mythoria;
import it.adryh.mythoria.core.Wallet;
import it.adryh.mythoria.core.client.CoreClient;
import it.adryh.mythoria.core.market.MarketCatalog;
import it.adryh.mythoria.core.market.MarketEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** The market talks to the server: only the server changes wallets and inventories. */
public final class Net {
    private static SimpleChannel channel;

    private Net() {}

    /** Client asks to open the shop. */
    public record Open() {}

    /** Client asks to buy something. */
    public record Buy(ResourceLocation id) {}

    /** Server tells the client the balance, and what happened. result: 0 open, 1 bought, 2 not enough. */
    public record State(int result, long balance, String entry) {}

    public static void init() {
        channel = ChannelBuilder.named(Mythoria.id("main")).networkProtocolVersion(1).simpleChannel();
        channel.messageBuilder(Open.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buf) -> {})
                .decoder(buf -> new Open())
                .consumerMainThread(Net::handleOpen)
                .add();
        channel.messageBuilder(Buy.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buf) -> buf.writeResourceLocation(message.id()))
                .decoder(buf -> new Buy(buf.readResourceLocation()))
                .consumerMainThread(Net::handleBuy)
                .add();
        channel.messageBuilder(State.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buf) -> {
                    buf.writeVarInt(message.result());
                    buf.writeLong(message.balance());
                    buf.writeUtf(message.entry());
                })
                .decoder(buf -> new State(buf.readVarInt(), buf.readLong(), buf.readUtf()))
                .consumerMainThread(Net::handleState)
                .add();
    }

    public static void requestOpen() {
        channel.send(new Open(), PacketDistributor.SERVER.noArg());
    }

    public static void requestBuy(ResourceLocation id) {
        channel.send(new Buy(id), PacketDistributor.SERVER.noArg());
    }

    public static void openMarket(ServerPlayer player) {
        Wallet.deposit(player);
        send(player, new State(0, Wallet.balance(player), ""));
    }

    private static void send(ServerPlayer player, State state) {
        channel.send(state, PacketDistributor.PLAYER.with(player));
    }

    private static void handleOpen(Open message, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null) {
            openMarket(player);
        }
    }

    private static void handleBuy(Buy message, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        MarketEntry entry = MarketCatalog.get(message.id());
        if (player == null || entry == null || player.isSpectator()) {
            return;
        }
        Wallet.deposit(player);
        if (Wallet.balance(player) < entry.price()) {
            player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8F, 1.0F);
            send(player, new State(2, Wallet.balance(player), entry.id().toString()));
            return;
        }
        Wallet.add(player, -entry.price());
        for (ItemStack stack : entry.stacks()) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.5F);
        send(player, new State(1, Wallet.balance(player), entry.id().toString()));
    }

    private static void handleState(State message, CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CoreClient.onState(message);
        }
    }
}
