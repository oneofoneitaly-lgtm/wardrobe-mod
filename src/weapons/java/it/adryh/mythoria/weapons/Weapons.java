package it.adryh.mythoria.weapons;

import it.adryh.mythoria.core.market.MarketCatalog;
import it.adryh.mythoria.core.market.MarketCategory;
import it.adryh.mythoria.core.market.MarketEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.BiFunction;

@Mod(Weapons.ID)
public final class Weapons {
    public static final String ID = "mythoria_weapons";

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);

    private static final Tier MYTHIC = new ForgeTier(2200, 9.0F, 4.0F, 18, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(Items.DIAMOND), BlockTags.INCORRECT_FOR_NETHERITE_TOOL);

    public static final RegistryObject<Item> PHOENIX = sword("phoenix_blade", PhoenixBlade::new);
    public static final RegistryObject<Item> FROST = sword("frost_fang", FrostFang::new);
    public static final RegistryObject<Item> STORM = sword("storm_blade", StormBlade::new);

    private static RegistryObject<Item> sword(String name, BiFunction<Tier, Item.Properties, Item> factory) {
        return ITEMS.register(name, () -> factory.apply(MYTHIC, new Item.Properties().rarity(Rarity.EPIC)
                .fireResistant().attributes(SwordItem.createAttributes(MYTHIC, 3, -2.4F))));
    }

    public Weapons(FMLJavaModLoadingContext context) {
        ITEMS.register(context.getModEventBus());
        sell("phoenix_blade", 0, 500);
        sell("frost_fang", 1, 450);
        sell("storm_blade", 2, 650);
    }

    private static void sell(String name, int order, int price) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ID, name);
        MarketCatalog.add(new MarketEntry(id, MarketCategory.WEAPONS, order, price, List.of(id), 1, null, 50.0F, 0.25F));
    }
}
