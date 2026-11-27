package it.adryh.mythoria.creatures;

import it.adryh.mythoria.core.market.MarketCatalog;
import it.adryh.mythoria.core.market.MarketCategory;
import it.adryh.mythoria.core.market.MarketEntry;
import it.adryh.mythoria.creatures.client.CreaturesClient;
import it.adryh.mythoria.creatures.entity.CrystalGolem;
import it.adryh.mythoria.creatures.entity.GoldenGriffin;
import it.adryh.mythoria.creatures.entity.EmberDrake;
import it.adryh.mythoria.creatures.entity.ShadowLich;
import it.adryh.mythoria.creatures.entity.SpiritFox;
import it.adryh.mythoria.creatures.item.BossItem;
import it.adryh.mythoria.creatures.item.HatchItem;
import it.adryh.mythoria.creatures.net.CreatureNet;
import net.minecraft.world.entity.Mob;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

@Mod(Creatures.ID)
public final class Creatures {
    public static final String ID = "mythoria_creatures";

    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);

    public static final RegistryObject<EntityType<GoldenGriffin>> GRIFFIN = ENTITIES.register("golden_griffin",
            () -> EntityType.Builder.of(GoldenGriffin::new, MobCategory.CREATURE)
                    .sized(1.3F, 1.75F).eyeHeight(1.65F).passengerAttachments(new Vec3(0.0, 1.42, -0.1))
                    .clientTrackingRange(10).build("golden_griffin"));

    public static final RegistryObject<EntityType<CrystalGolem>> GOLEM = ENTITIES.register("crystal_golem",
            () -> EntityType.Builder.of(CrystalGolem::new, MobCategory.MONSTER)
                    .sized(1.9F, 3.0F).eyeHeight(2.6F).fireImmune()
                    .clientTrackingRange(10).build("crystal_golem"));

    public static final RegistryObject<EntityType<EmberDrake>> DRAKE = ENTITIES.register("ember_drake",
            () -> EntityType.Builder.of(EmberDrake::new, MobCategory.CREATURE)
                    .sized(1.6F, 2.1F).eyeHeight(2.0F).passengerAttachments(new Vec3(0.0, 1.72, -0.1))
                    .fireImmune().clientTrackingRange(10).build("ember_drake"));

    public static final RegistryObject<EntityType<SpiritFox>> FOX = ENTITIES.register("spirit_fox",
            () -> EntityType.Builder.of(SpiritFox::new, MobCategory.CREATURE)
                    .sized(0.65F, 0.85F).eyeHeight(0.7F).clientTrackingRange(10).build("spirit_fox"));

    public static final RegistryObject<EntityType<ShadowLich>> LICH = ENTITIES.register("shadow_lich",
            () -> EntityType.Builder.of(ShadowLich::new, MobCategory.MONSTER)
                    .sized(1.1F, 2.8F).eyeHeight(2.35F).fireImmune().clientTrackingRange(10).build("shadow_lich"));

    public static final RegistryObject<Item> GRIFFIN_EGG = ITEMS.register("griffin_egg",
            () -> new HatchItem(GRIFFIN, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> GOLEM_CORE = ITEMS.register("golem_core",
            () -> new BossItem(GOLEM, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> DRAKE_EGG = ITEMS.register("drake_egg",
            () -> new HatchItem(DRAKE, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> SPIRIT_CHARM = ITEMS.register("spirit_charm",
            () -> new HatchItem(FOX, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> PHYLACTERY = ITEMS.register("lich_phylactery",
            () -> new BossItem(LICH, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public Creatures(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) -> attributes(event));
        bus.addListener((SpawnPlacementRegisterEvent event) -> spawnRules(event));
        WildSpawns.register(MinecraftForge.EVENT_BUS);
        sell("griffin_egg", 0, 400, "golden_griffin", 31.0F, 0.95F);
        sell("golem_core", 3, 150, "crystal_golem", 25.0F, 1.55F);
        sell("drake_egg", 1, 900, "ember_drake", 23.0F, 1.0F);
        sell("spirit_charm", 2, 300, "spirit_fox", 58.0F, 0.5F);
        sell("lich_phylactery", 4, 250, "shadow_lich", 26.0F, 1.45F);
        CreatureNet.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CreaturesClient.init(context);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    private static void sell(String item, int order, int price, String entity, float size, float lift) {
        MarketCatalog.add(new MarketEntry(id(item), MarketCategory.CREATURES, order, price,
                List.of(id(item)), 1, id(entity), size, lift));
    }

    private void attributes(EntityAttributeCreationEvent event) {
        event.put(GRIFFIN.get(), GoldenGriffin.createAttributes().build());
        event.put(GOLEM.get(), CrystalGolem.createAttributes().build());
        event.put(DRAKE.get(), EmberDrake.createDrakeAttributes().build());
        event.put(FOX.get(), SpiritFox.createAttributes().build());
        event.put(LICH.get(), ShadowLich.createAttributes().build());
    }

    private void spawnRules(SpawnPlacementRegisterEvent event) {
        event.register(GRIFFIN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(GOLEM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CrystalGolem::checkSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(DRAKE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(FOX.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
