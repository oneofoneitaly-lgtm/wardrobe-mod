package it.adryh.mythoria.armory;

import it.adryh.mythoria.core.market.MarketCatalog;
import it.adryh.mythoria.core.market.MarketCategory;
import it.adryh.mythoria.core.market.MarketEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
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
import java.util.function.Supplier;

@Mod(Armory.ID)
public final class Armory {
    public static final String ID = "mythoria_armory";

    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    private static final EnumMap<ArmorSet, List<RegistryObject<Item>>> SETS = new EnumMap<>(ArmorSet.class);

    static {
        register(ArmorSet.DRAGON, material(ArmorSet.DRAGON, 4, 9, 7, 4, 18, SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> Ingredient.of(Items.NETHERITE_SCRAP), 3.5F, 0.15F), 40);
        register(ArmorSet.ELVEN, material(ArmorSet.ELVEN, 3, 7, 6, 3, 22, SoundEvents.ARMOR_EQUIP_LEATHER,
                () -> Ingredient.of(Items.EMERALD), 2.0F, 0.0F), 32);
        register(ArmorSet.FROST, material(ArmorSet.FROST, 3, 8, 6, 3, 16, SoundEvents.ARMOR_EQUIP_DIAMOND,
                () -> Ingredient.of(Items.DIAMOND), 2.5F, 0.05F), 35);
    }

    private static RegistryObject<ArmorMaterial> material(ArmorSet set, int helmet, int chest, int legs, int boots, int enchant,
            Holder<SoundEvent> sound, Supplier<Ingredient> repair, float toughness, float knockback) {
        return MATERIALS.register(set.key(), () -> {
            EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
            defense.put(ArmorItem.Type.BOOTS, boots);
            defense.put(ArmorItem.Type.LEGGINGS, legs);
            defense.put(ArmorItem.Type.CHESTPLATE, chest);
            defense.put(ArmorItem.Type.HELMET, helmet);
            defense.put(ArmorItem.Type.BODY, chest + 2);
            return new ArmorMaterial(defense, enchant, sound, repair,
                    List.of(new ArmorMaterial.Layer(id(set.key()))), toughness, knockback);
        });
    }

    private static void register(ArmorSet set, RegistryObject<ArmorMaterial> material, int durability) {
        List<RegistryObject<Item>> pieces = new ArrayList<>();
        for (ArmorItem.Type type : new ArmorItem.Type[]{ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE,
                ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
            pieces.add(ITEMS.register(set.key() + "_" + type.getName(), () -> new MythicArmorItem(set,
                    material.getHolder().orElseThrow(), type,
                    new Item.Properties().durability(type.getDurability(durability)).rarity(Rarity.EPIC).fireResistant())));
        }
        SETS.put(set, pieces);
    }

    public Armory(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        MATERIALS.register(bus);
        ITEMS.register(bus);
        MinecraftForge.EVENT_BUS.addListener((TickEvent.PlayerTickEvent.Post event) -> setBonus(event));

        int[] setPrices = {900, 650, 700};
        int[][] piecePrices = {{200, 350, 300, 180}, {150, 250, 220, 130}, {160, 270, 240, 140}};
        for (ArmorSet set : ArmorSet.values()) {
            List<RegistryObject<Item>> pieces = SETS.get(set);
            sell(set.key() + "_set", set.ordinal(), setPrices[set.ordinal()], pieces.stream().map(o -> o.getId()).toList(), 40.0F, 1.1F);
            for (int i = 0; i < pieces.size(); i++) {
                ResourceLocation item = pieces.get(i).getId();
                sell(item.getPath(), 10 + set.ordinal() * 4 + i, piecePrices[set.ordinal()][i], List.of(item), 0.0F, 0.0F);
            }
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

    /** The set worn as a whole, or null. */
    private static ArmorSet wornSet(ServerPlayer player) {
        ArmorSet found = null;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(player.getItemBySlot(slot).getItem() instanceof MythicArmorItem piece) || found != null && piece.set() != found) {
                return null;
            }
            found = piece.set();
        }
        return found;
    }

    private static void setBonus(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        ArmorSet set = wornSet(player);
        if (set == null) {
            return;
        }
        if (set == ArmorSet.FROST && player.getTicksFrozen() > 0) {
            player.setTicksFrozen(0);
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        switch (set) {
            case DRAGON -> give(player, MobEffects.FIRE_RESISTANCE);
            case ELVEN -> {
                give(player, MobEffects.MOVEMENT_SPEED);
                give(player, MobEffects.JUMP);
            }
            case FROST -> give(player, MobEffects.WATER_BREATHING);
        }
    }

    private static void give(ServerPlayer player, Holder<MobEffect> effect) {
        player.addEffect(new MobEffectInstance(effect, 60, 0, true, false, true));
    }
}
