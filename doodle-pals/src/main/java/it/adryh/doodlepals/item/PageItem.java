package it.adryh.doodlepals.item;

import it.adryh.doodlepals.DoodlePals;
import it.adryh.doodlepals.Drawing;
import it.adryh.doodlepals.Power;
import it.adryh.doodlepals.client.PageClient;
import it.adryh.doodlepals.entity.DoodlePal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

/** A resting pal, kept as a page of the sketchbook. Use it on the ground to wake it. */
public final class PageItem extends Item {
    public PageItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(DoodlePal pal) {
        ItemStack stack = new ItemStack(DoodlePals.PAGE.get());
        CompoundTag tag = new CompoundTag();
        tag.put("Drawing", pal.drawing().save());
        if (pal.hasCustomName()) {
            tag.putString("Name", pal.getCustomName().getString());
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static Drawing drawing(ItemStack stack) {
        return Drawing.load(tag(stack).getCompound("Drawing"));
    }

    private static String name(ItemStack stack) {
        return tag(stack).getString("Name");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        DoodlePal pal = Pals.birth(player, context.getClickedPos().relative(context.getClickedFace()), drawing(stack), name(stack));
        if (pal == null) {
            return InteractionResult.FAIL;
        }
        // the page turns back into the pal, whatever the game mode
        stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public Component getName(ItemStack stack) {
        String name = name(stack);
        return name.isEmpty() ? super.getName(stack) : Component.translatable("item.doodlepals.doodle_page.named", name);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        for (Drawing.Level power : drawing(stack).powers()) {
            // shadow would be unreadable on the dark tooltip
            int colour = power.power() == Power.SHADOW ? 0x8A86B0 : power.power().colour();
            lines.add(Component.translatable(power.power().nameKey()).append(" " + "I".repeat(power.level()))
                    .withStyle(style -> style.withColor(colour)));
        }
        lines.add(Component.translatable("item.doodlepals.doodle_page.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new PageClient());
    }
}
