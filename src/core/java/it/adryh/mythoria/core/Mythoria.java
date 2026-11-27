package it.adryh.mythoria.core;

import it.adryh.mythoria.core.client.CoreClient;
import it.adryh.mythoria.core.market.MarketCatalog;
import it.adryh.mythoria.core.net.Net;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashSet;
import java.util.Set;

/** Core of the Mythoria modpack by @Adryh01: coins, wallet and the market. */
@Mod(Mythoria.ID)
public final class Mythoria {
    public static final String ID = "mythoria";

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    public static final RegistryObject<Item> CROWN = ITEMS.register("crown",
            () -> new CrownItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("mythoria", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mythoria"))
            .icon(() -> new ItemStack(CROWN.get()))
            .displayItems((params, out) -> {
                Set<Item> seen = new LinkedHashSet<>();
                MarketCatalog.all().forEach(entry -> entry.stacks().forEach(stack -> seen.add(stack.getItem())));
                seen.add(CROWN.get());
                seen.forEach(out::accept);
            })
            .build());

    public Mythoria(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        ITEMS.register(bus);
        TABS.register(bus);
        Net.init();
        MarketCommands.register(MinecraftForge.EVENT_BUS);
        Earnings.register(MinecraftForge.EVENT_BUS);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CoreClient.init(context);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }
}
