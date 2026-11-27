package it.adryh.mythoria.armory;

import it.adryh.mythoria.armory.client.ArmorClientExtensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

/** Armour with its own 3d model: horns, wings, spikes and claws. */
public final class DragonArmorItem extends ArmorItem {
    public DragonArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean inner) {
        return Armory.id("textures/armor/dragon_" + getType().getName() + ".png");
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new ArmorClientExtensions());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.mythoria_armory.set_bonus").withStyle(ChatFormatting.GOLD));
    }
}
