package it.adryh.mythoria.armory;

import it.adryh.mythoria.core.market.MarketCatalog;
import it.adryh.mythoria.core.market.MarketCategory;
import it.adryh.mythoria.core.market.MarketEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@Mod(Armory.ID)
public final class Armory {
    public static final String ID = "mythoria_armory";

    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);

    public static final RegistryObject<ArmorMaterial> DRAGON = MATERIALS.register("dragon", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 4);
        defense.put(ArmorItem.Type.LEGGINGS, 7);
        defense.put(ArmorItem.Type.CHESTPLATE, 9);
        defense.put(ArmorItem.Type.HELMET, 4);
        defense.put(ArmorItem.Type.BODY, 11);
        return new ArmorMaterial(defense, 18, SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> Ingredient.of(Items.NETHERITE_SCRAP),
                List.of(new ArmorMaterial.Layer(id("dragon"))), 3.5F, 0.15F);
    });

    private static final List<RegistryObject<Item>> SET = new ArrayList<>();

    static {
        piece("dragon_helmet", ArmorItem.Type.HELMET);
        piece("dragon_chestplate", ArmorItem.Type.CHESTPLATE);
        piece("dragon_leggings", ArmorItem.Type.LEGGINGS);
        piece("dragon_boots", ArmorItem.Type.BOOTS);
    }

    private static void piece(String name, ArmorItem.Type type) {
        SET.add(ITEMS.register(name, () -> new DragonArmorItem(DRAGON.getHolder().orElseThrow(), type,
                new Item.Properties().durability(type.getDurability(40)).rarity(Rarity.EPIC).fireResistant())));
    }

    public Armory(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        MATERIALS.register(bus);
        ITEMS.register(bus);
        MinecraftForge.EVENT_BUS.addListener((TickEvent.PlayerTickEvent.Post event) -> setBonus(event));

        sell("dragon_set", 0, 900, SET.stream().map(o -> o.getId()).toList(), 52.0F, 1.1F);
        int[] prices = {200, 350, 300, 180};
        for (int i = 0; i < SET.size(); i++) {
            ResourceLocation item = SET.get(i).getId();
            sell(item.getPath(), i + 1, prices[i], List.of(item), 0.0F, 0.0F);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    /** The full set is shown on an armour stand, single pieces as spinning items. */
    private static void sell(String name, int order, int price, List<ResourceLocation> items, float size, float lift) {
        boolean set = items.size() > 1;
        MarketCatalog.add(new MarketEntry(id(name), MarketCategory.ARMOR, order, price, items, 1,
                set ? ResourceLocation.withDefaultNamespace("armor_stand") : null,
                set ? size : 80.0F, set ? lift : 0.0F));
    }

    /** Wearing all four pieces protects from fire. */
    private static void setBonus(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(player.getItemBySlot(slot).getItem() instanceof DragonArmorItem)) {
                return;
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
    }
}
