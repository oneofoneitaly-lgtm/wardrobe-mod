package it.adryh.mythoria.feast;

import it.adryh.mythoria.core.market.MarketCatalog;
import it.adryh.mythoria.core.market.MarketCategory;
import it.adryh.mythoria.core.market.MarketEntry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Mod(Feast.ID)
public final class Feast {
    public static final String ID = "mythoria_feast";

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);

    static {
        food("moon_apple", false, 6, 0.8F, new Effect(MobEffects.NIGHT_VISION, 3600, 0), new Effect(MobEffects.ABSORPTION, 1200, 0));
        food("fairy_cake", false, 5, 0.6F, new Effect(MobEffects.JUMP, 1200, 1), new Effect(MobEffects.SLOW_FALLING, 1200, 0));
        food("dragon_drumstick", false, 12, 1.0F, new Effect(MobEffects.DAMAGE_BOOST, 1200, 0), new Effect(MobEffects.FIRE_RESISTANCE, 2400, 0));
        food("elven_mead", true, 3, 0.4F, new Effect(MobEffects.MOVEMENT_SPEED, 1200, 1), new Effect(MobEffects.REGENERATION, 200, 1));
        food("star_fruit", false, 4, 0.8F, new Effect(MobEffects.DIG_SPEED, 2400, 1), new Effect(MobEffects.LUCK, 2400, 0));
        food("frost_sorbet", false, 4, 0.5F, new Effect(MobEffects.WATER_BREATHING, 3600, 0), new Effect(MobEffects.DOLPHINS_GRACE, 1800, 0));
        food("honey_pie", false, 10, 0.9F, new Effect(MobEffects.DAMAGE_RESISTANCE, 1200, 0), new Effect(MobEffects.HEALTH_BOOST, 2400, 1));
        food("mana_potion", true, 2, 0.4F, new Effect(MobEffects.INVISIBILITY, 900, 0), new Effect(MobEffects.MOVEMENT_SPEED, 900, 1));
    }

    private record Effect(Holder<MobEffect> type, int ticks, int level) {}

    private static void food(String name, boolean drink, int nutrition, float saturation, Effect... effects) {
        ITEMS.register(name, () -> {
            FoodProperties.Builder food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).alwaysEdible();
            for (Effect effect : effects) {
                food.effect(new MobEffectInstance(effect.type(), effect.ticks(), effect.level()), 1.0F);
            }
            Item.Properties properties = new Item.Properties().food(food.build()).rarity(Rarity.RARE);
            return new FeastItem(properties, name, drink);
        });
    }

    public Feast(FMLJavaModLoadingContext context) {
        ITEMS.register(context.getModEventBus());
        sell("moon_apple", 0, 40, 4);
        sell("fairy_cake", 1, 35, 4);
        sell("dragon_drumstick", 2, 60, 4);
        sell("elven_mead", 3, 45, 4);
        sell("star_fruit", 4, 50, 4);
        sell("frost_sorbet", 5, 40, 4);
        sell("honey_pie", 6, 65, 4);
        sell("mana_potion", 7, 70, 4);
    }

    private static void sell(String name, int order, int price, int count) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ID, name);
        MarketCatalog.add(new MarketEntry(id, MarketCategory.FEAST, order, price, List.of(id), count, null, 82.0F, 0.0F));
    }
}
