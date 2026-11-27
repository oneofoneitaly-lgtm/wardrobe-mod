package it.adryh.mythoria.armory;

import it.adryh.mythoria.armory.client.ArmorClientExtensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

/** Armour with its own 3d model: horns, wings, antlers and crystals. */
public final class MythicArmorItem extends ArmorItem {
    private final ArmorSet set;

    public MythicArmorItem(ArmorSet set, Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
        this.set = set;
    }

    public ArmorSet set() {
        return set;
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean inner) {
        return Armory.id("textures/armor/" + set.key() + "_" + getType().getName() + ".png");
    }

    /** Frost boots walk on powder snow, like leather ones. */
    @Override
    public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer) {
        return set == ArmorSet.FROST && getType() == Type.BOOTS;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        // called from the Item constructor, before "set" is assigned: the extensions look it up later
        consumer.accept(new ArmorClientExtensions());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.mythoria_armory." + set.key() + "_set_bonus").withStyle(ChatFormatting.GOLD));
    }
}
