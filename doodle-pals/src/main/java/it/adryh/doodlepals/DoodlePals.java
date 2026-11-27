package it.adryh.doodlepals;

import it.adryh.doodlepals.client.ClientSetup;
import it.adryh.doodlepals.entity.DoodlePal;
import it.adryh.doodlepals.item.PageItem;
import it.adryh.doodlepals.item.SketchbookItem;
import it.adryh.doodlepals.net.Net;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static net.minecraft.commands.Commands.literal;

@Mod(DoodlePals.ID)
public final class DoodlePals {
    public static final String ID = "doodlepals";
    /** How many living doodles one player may have around. */
    public static final int LIMIT = 3;

    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);

    public static final RegistryObject<EntityType<DoodlePal>> PAL = ENTITIES.register("pal",
            () -> EntityType.Builder.of(DoodlePal::new, MobCategory.CREATURE)
                    .sized(0.8F, 1.0F).clientTrackingRange(10).build("pal"));

    public static final RegistryObject<Item> SKETCHBOOK = ITEMS.register("sketchbook",
            () -> new SketchbookItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> PAGE = ITEMS.register("doodle_page",
            () -> new PageItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));

    public DoodlePals(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(PAL.get(), DoodlePal.createAttributes().build()));
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                event.accept(SKETCHBOOK.get());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> event.getDispatcher().register(literal("doodle")
                .executes(source -> {
                    Net.openSketchbook(source.getSource().getPlayerOrException());
                    return 1;
                })
                .then(literal("call").executes(source -> {
                    ServerPlayer player = source.getSource().getPlayerOrException();
                    return DoodlePal.callAll(player);
                }))));
        Net.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(context);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }
}
