package it.adryh.mythoria.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Crown balance, saved with the player and kept after death. */
public final class Wallet {
    private static final String KEY = "mythoria_crowns";

    private Wallet() {}

    private static CompoundTag data(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(ServerPlayer.PERSISTED_NBT_TAG)) {
            root.put(ServerPlayer.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(ServerPlayer.PERSISTED_NBT_TAG);
    }

    public static long balance(Player player) {
        return data(player).getLong(KEY);
    }

    public static void set(Player player, long amount) {
        data(player).putLong(KEY, Math.max(0, amount));
    }

    public static void add(Player player, long amount) {
        set(player, balance(player) + amount);
    }

    /** Moves every Crown carried in the inventory into the wallet and returns how many. */
    public static int deposit(ServerPlayer player) {
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Mythoria.CROWN.get())) {
                found += stack.getCount();
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        if (found > 0) {
            add(player, found);
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
        return found;
    }
}
