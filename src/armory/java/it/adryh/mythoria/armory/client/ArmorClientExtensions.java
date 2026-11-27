package it.adryh.mythoria.armory.client;

import it.adryh.mythoria.armory.client.gen.BootsLayer;
import it.adryh.mythoria.armory.client.gen.ChestplateLayer;
import it.adryh.mythoria.armory.client.gen.HelmetLayer;
import it.adryh.mythoria.armory.client.gen.LeggingsLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.EnumMap;
import java.util.Map;

public final class ArmorClientExtensions implements IClientItemExtensions {
    private static final Map<EquipmentSlot, HumanoidModel<LivingEntity>> MODELS = new EnumMap<>(EquipmentSlot.class);

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
        HumanoidModel<LivingEntity> model = MODELS.computeIfAbsent(slot, ArmorClientExtensions::create);
        return model == null ? original : model;
    }

    private static HumanoidModel<LivingEntity> create(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> new HumanoidModel<>(HelmetLayer.create().bakeRoot());
            case CHEST -> new HumanoidModel<>(ChestplateLayer.create().bakeRoot());
            case LEGS -> new HumanoidModel<>(LeggingsLayer.create().bakeRoot());
            case FEET -> new HumanoidModel<>(BootsLayer.create().bakeRoot());
            default -> null;
        };
    }
}
