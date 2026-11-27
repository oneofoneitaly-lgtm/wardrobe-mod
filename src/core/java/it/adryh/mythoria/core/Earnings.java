package it.adryh.mythoria.core;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** How Crowns are earned: defeating monsters and mining. */
public final class Earnings {
    private static final String WELCOMED = "mythoria_welcomed";

    private Earnings() {}

    public static void register(IEventBus bus) {
        bus.addListener((LivingDropsEvent event) -> onDrops(event));
        bus.addListener((BlockEvent.BreakEvent event) -> onBreak(event));
        bus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> onLogin(event));
    }

    private static void onDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim instanceof Enemy) || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        var killer = event.getSource().getEntity();
        boolean byPlayer = killer instanceof Player
                || killer instanceof TamableAnimal pet && pet.getOwner() instanceof Player;
        if (!byPlayer) {
            return;
        }
        int amount = 2 + level.random.nextInt(4) + (int) (victim.getMaxHealth() / 10.0F);
        while (amount > 0) {
            int part = Math.min(amount, 64);
            event.getDrops().add(new ItemEntity(level, victim.getX(), victim.getY() + 0.4, victim.getZ(),
                    new ItemStack(Mythoria.CROWN.get(), part)));
            amount -= part;
        }
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(event.getLevel() instanceof ServerLevel level) || player == null || player.isCreative()) {
            return;
        }
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        if (!player.hasCorrectToolForDrops(state) || hasSilkTouch(player.getMainHandItem())) {
            return;
        }
        RandomSource random = level.random;
        int amount = 0;
        if (state.is(Tags.Blocks.ORES)) {
            amount = random.nextFloat() < 0.65F ? 1 + random.nextInt(4) : 0;
        } else if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)) {
            amount = random.nextFloat() < 0.04F ? 1 : 0;
        }
        if (amount > 0) {
            Block.popResource(level, pos, new ItemStack(Mythoria.CROWN.get(), amount));
        }
    }

    private static boolean hasSilkTouch(ItemStack tool) {
        ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (holder.is(Enchantments.SILK_TOUCH)) {
                return true;
            }
        }
        return false;
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.getTags().contains(WELCOMED)) {
            player.addTag(WELCOMED);
            player.sendSystemMessage(Component.translatable("mythoria.welcome").withStyle(ChatFormatting.GOLD));
        }
    }
}
