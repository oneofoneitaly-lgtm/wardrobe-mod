package it.adryh.mythoria.creatures.net;

import it.adryh.mythoria.creatures.Creatures;
import it.adryh.mythoria.creatures.entity.EmberDrake;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** The rider's ability key travels to the server, which checks what is being ridden. */
public final class CreatureNet {
    private static SimpleChannel channel;

    private CreatureNet() {}

    public record Ability() {}

    public static void init() {
        channel = ChannelBuilder.named(Creatures.id("main")).networkProtocolVersion(1).simpleChannel();
        channel.messageBuilder(Ability.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buf) -> {})
                .decoder(buf -> new Ability())
                .consumerMainThread(CreatureNet::handle)
                .add();
    }

    public static void useAbility() {
        channel.send(new Ability(), PacketDistributor.SERVER.noArg());
    }

    private static void handle(Ability message, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof EmberDrake drake) {
            drake.breathe(player);
        }
    }
}
