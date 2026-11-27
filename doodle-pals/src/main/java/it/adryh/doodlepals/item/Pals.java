package it.adryh.doodlepals.item;

import it.adryh.doodlepals.DoodlePals;
import it.adryh.doodlepals.Drawing;
import it.adryh.doodlepals.entity.DoodlePal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;

/** The one place where a drawing turns into a living pal. */
public final class Pals {
    private Pals() {}

    /** Returns null, after telling the player why, when the pal cannot be born. */
    public static DoodlePal birth(ServerPlayer player, BlockPos pos, Drawing drawing, String name) {
        if (!drawing.isAlive()) {
            tell(player, Component.translatable("doodlepals.too_small", Drawing.MIN_PIXELS));
            return null;
        }
        if (DoodlePal.countOf(player) >= DoodlePals.LIMIT) {
            tell(player, Component.translatable("doodlepals.too_many", DoodlePals.LIMIT));
            return null;
        }
        ServerLevel level = player.serverLevel();
        DoodlePal pal = DoodlePals.PAL.get().create(level);
        if (pal == null) {
            return null;
        }
        pal.setDrawing(drawing);
        pal.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180.0F, 0.0F);
        if (!level.noCollision(pal)) {
            pal.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot() + 180.0F, 0.0F);
        }
        pal.yBodyRot = pal.getYRot();
        pal.yHeadRot = pal.getYRot();
        pal.finalizeSpawn(level, level.getCurrentDifficultyAt(pal.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        pal.tame(player);
        pal.setHealth(pal.getMaxHealth());
        pal.setPersistenceRequired();
        if (!name.isBlank()) {
            pal.setCustomName(Component.literal(name.strip()));
        }
        level.addFreshEntity(pal);
        for (Drawing.Level power : drawing.powers()) {
            level.sendParticles(DoodlePal.dust(power.power(), 1.4F), pal.getX(), pal.getY() + pal.getBbHeight() / 2.0, pal.getZ(),
                    40, 0.5, 0.5, 0.5, 0.0);
        }
        level.playSound(null, pal.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.NEUTRAL, 1.2F, 1.3F);
        return pal;
    }

    private static void tell(ServerPlayer player, Component message) {
        player.displayClientMessage(message.copy().withStyle(ChatFormatting.RED), true);
    }
}
