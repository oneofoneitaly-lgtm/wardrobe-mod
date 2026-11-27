package it.adryh.mythoria.armory.client;

import it.adryh.mythoria.armory.ArmorSet;
import it.adryh.mythoria.armory.MythicArmorItem;
import it.adryh.mythoria.armory.client.gen.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.HashMap;
import java.util.Map;

public final class ArmorClientExtensions implements IClientItemExtensions {
    private static final Map<String, HumanoidModel<LivingEntity>> MODELS = new HashMap<>();

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
        if (!(stack.getItem() instanceof MythicArmorItem armor)) {
            return original;
        }
        ArmorSet set = armor.set();
        HumanoidModel<LivingEntity> model = MODELS.computeIfAbsent(set.key() + slot.getName(), key -> create(set, slot));
        return model == null ? original : model;
    }

    private static HumanoidModel<LivingEntity> create(ArmorSet set, EquipmentSlot slot) {
        LayerDefinition layer = switch (set) {
            case DRAGON -> switch (slot) {
                case HEAD -> DragonHelmetLayer.create();
                case CHEST -> DragonChestplateLayer.create();
                case LEGS -> DragonLeggingsLayer.create();
                case FEET -> DragonBootsLayer.create();
                default -> null;
            };
            case ELVEN -> switch (slot) {
                case HEAD -> ElvenHelmetLayer.create();
                case CHEST -> ElvenChestplateLayer.create();
                case LEGS -> ElvenLeggingsLayer.create();
                case FEET -> ElvenBootsLayer.create();
                default -> null;
            };
            case FROST -> switch (slot) {
                case HEAD -> FrostHelmetLayer.create();
                case CHEST -> FrostChestplateLayer.create();
                case LEGS -> FrostLeggingsLayer.create();
                case FEET -> FrostBootsLayer.create();
                default -> null;
            };
        };
        return layer == null ? null : new HumanoidModel<>(layer.bakeRoot());
    }
}
