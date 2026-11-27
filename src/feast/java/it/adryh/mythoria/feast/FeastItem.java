package it.adryh.mythoria.feast;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;

import java.util.List;

/** Magical food and drink. */
public final class FeastItem extends Item {
    private final String key;
    private final boolean drink;

    public FeastItem(Properties properties, String key, boolean drink) {
        super(properties);
        this.key = key;
        this.drink = drink;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return drink ? UseAnim.DRINK : UseAnim.EAT;
    }

    @Override
    public SoundEvent getEatingSound() {
        return drink ? SoundEvents.GENERIC_DRINK : super.getEatingSound();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.mythoria_feast." + key + ".tooltip").withStyle(ChatFormatting.AQUA));
    }
}
