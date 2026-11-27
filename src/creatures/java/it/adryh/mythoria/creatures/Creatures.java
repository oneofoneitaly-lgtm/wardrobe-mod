package it.adryh.mythoria.creatures;

import it.adryh.mythoria.core.market.MarketCatalog;
import it.adryh.mythoria.core.market.MarketCategory;
import it.adryh.mythoria.core.market.MarketEntry;
import it.adryh.mythoria.creatures.client.CreaturesClient;
import it.adryh.mythoria.creatures.entity.CrystalGolem;
import it.adryh.mythoria.creatures.entity.GoldenGriffin;
import it.adryh.mythoria.creatures.item.GolemCoreItem;
import it.adryh.mythoria.creatures.item.GriffinEggItem;
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

    public static final RegistryObject<Item> GRIFFIN_EGG = ITEMS.register("griffin_egg",
            () -> new GriffinEggItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> GOLEM_CORE = ITEMS.register("golem_core",
            () -> new GolemCoreItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public Creatures(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) -> attributes(event));
        bus.addListener((SpawnPlacementRegisterEvent event) -> spawnRules(event));
        sell("griffin_egg", 0, 400, "golden_griffin", 31.0F, 0.95F);
        sell("golem_core", 1, 150, "crystal_golem", 25.0F, 1.55F);
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
    }

    private void spawnRules(SpawnPlacementRegisterEvent event) {
        event.register(GRIFFIN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(GOLEM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CrystalGolem::checkSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
