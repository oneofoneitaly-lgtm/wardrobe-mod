package it.adryh.doodlepals.net;

import it.adryh.doodlepals.DoodlePals;
import it.adryh.doodlepals.Drawing;
import it.adryh.doodlepals.client.ClientSetup;
import it.adryh.doodlepals.entity.DoodlePal;
import it.adryh.doodlepals.item.Pals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** The client only sends the picture; the server checks it, takes the ink and creates the pal. */
public final class Net {
    private static final int NAME_LENGTH = 24;
    private static SimpleChannel channel;

    private Net() {}

    /** Server tells the client to open the drawing screen. */
    public record Open(boolean free) {}

    /** Client asks to bring a drawing to life. */
    public record Create(byte[] pixels, int[] eyes, String name) {}

    public static void init() {
        channel = ChannelBuilder.named(DoodlePals.id("main")).networkProtocolVersion(1).simpleChannel();
        channel.messageBuilder(Open.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buf) -> buf.writeBoolean(message.free()))
                .decoder(buf -> new Open(buf.readBoolean()))
                .consumerMainThread(Net::handleOpen)
                .add();
        channel.messageBuilder(Create.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buf) -> {
                    buf.writeByteArray(message.pixels());
                    buf.writeVarIntArray(message.eyes());
                    buf.writeUtf(message.name(), NAME_LENGTH);
                })
                .decoder(buf -> new Create(buf.readByteArray(Drawing.CELLS), buf.readVarIntArray(Drawing.MAX_EYES),
                        buf.readUtf(NAME_LENGTH)))
                .consumerMainThread(Net::handleCreate)
                .add();
    }

    public static void openSketchbook(ServerPlayer player) {
        channel.send(new Open(player.getAbilities().instabuild), PacketDistributor.PLAYER.with(player));
    }

    public static void create(Drawing drawing, String name) {
        channel.send(new Create(drawing.pixels(), drawing.eyes(), name), PacketDistributor.SERVER.noArg());
    }

    private static void handleOpen(Open message, CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.open(message.free());
        }
    }

    private static void handleCreate(Create message, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || player.isSpectator() || !player.isAlive()) {
            return;
        }
        boolean free = player.getAbilities().instabuild;
        int ink = free ? -1 : findInk(player);
        if (!free && ink < 0) {
            player.displayClientMessage(Component.translatable("doodlepals.no_ink").withStyle(ChatFormatting.RED), true);
            return;
        }
        Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(2.2);
        BlockPos pos = BlockPos.containing(player.position().add(look).add(0.0, 0.1, 0.0));
        String name = message.name().replaceAll("[\\p{Cntrl}§]", "");
        DoodlePal pal = Pals.birth(player, pos, new Drawing(message.pixels(), message.eyes()), name);
        if (pal != null && !free) {
            player.getInventory().removeItem(ink, 1);
        }
    }

    private static int findInk(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.INK_SAC) || stack.is(Items.GLOW_INK_SAC)) {
                return i;
            }
        }
        return -1;
    }
}
