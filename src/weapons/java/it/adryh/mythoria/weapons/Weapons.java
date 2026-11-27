package it.adryh.mythoria.weapons;

import it.adryh.mythoria.core.market.MarketCatalog;
import it.adryh.mythoria.core.market.MarketCategory;
import it.adryh.mythoria.core.market.MarketEntry;
import it.adryh.mythoria.weapons.client.WeaponsClient;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
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

    public static final RegistryObject<Item> VOID = sword("void_reaver", VoidReaver::new);
    public static final RegistryObject<Item> BOW = ITEMS.register("elven_bow",
            () -> new ElvenBow(new Item.Properties().durability(900).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> ARCANE = ITEMS.register("arcane_staff",
            () -> new ArcaneStaff(new Item.Properties().durability(700).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> LIFE = ITEMS.register("life_staff",
            () -> new LifeStaff(new Item.Properties().durability(300).rarity(Rarity.EPIC)));

    private static RegistryObject<Item> sword(String name, BiFunction<Tier, Item.Properties, Item> factory) {
        return ITEMS.register(name, () -> factory.apply(MYTHIC, new Item.Properties().rarity(Rarity.EPIC)
                .fireResistant().attributes(SwordItem.createAttributes(MYTHIC, 3, -2.4F))));
    }

    public Weapons(FMLJavaModLoadingContext context) {
        ITEMS.register(context.getModEventBus());
        sell("phoenix_blade", 0, 500);
        sell("frost_fang", 1, 450);
        sell("storm_blade", 2, 650);
        sell("void_reaver", 3, 600);
        sell("elven_bow", 4, 480);
        sell("arcane_staff", 5, 550);
        sell("life_staff", 6, 420);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            WeaponsClient.init(context);
        }
    }

    private static void sell(String name, int order, int price) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ID, name);
        MarketCatalog.add(new MarketEntry(id, MarketCategory.WEAPONS, order, price, List.of(id), 1, null, 40.0F, 0.25F));
    }
}
