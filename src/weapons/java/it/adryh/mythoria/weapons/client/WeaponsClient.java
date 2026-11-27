package it.adryh.mythoria.weapons.client;

import it.adryh.mythoria.weapons.Weapons;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Lets the item model of the bow follow how far it is drawn. */
public final class WeaponsClient {
    private WeaponsClient() {}

    public static void init(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener((FMLClientSetupEvent event) -> event.enqueueWork(WeaponsClient::bow));
    }

    private static void bow() {
        Item bow = Weapons.BOW.get();
        ItemPropertyFunction pull = (stack, level, entity, seed) -> entity == null || entity.getUseItem() != stack ? 0.0F
                : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
        ItemPropertyFunction pulling = (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F;
        ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pull"), pull);
        ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pulling"), pulling);
    }
}
