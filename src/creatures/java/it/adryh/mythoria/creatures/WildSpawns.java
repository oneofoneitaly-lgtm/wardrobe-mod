package it.adryh.mythoria.creatures;

import it.adryh.mythoria.creatures.entity.EmberDrake;
import it.adryh.mythoria.creatures.entity.GoldenGriffin;
import it.adryh.mythoria.creatures.entity.SpiritFox;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.Set;

/**
 * How the creatures appear in the world. Done in code on purpose: world generation data files
 * would make Minecraft show its "experimental settings" warning for every new world.
 */
final class WildSpawns {
    private static final Set<ResourceKey<Biome>> GRIFFIN_HOMES = Set.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW,
            Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_SAVANNA);

    private static final Set<ResourceKey<Biome>> DRAKE_HOMES = Set.of(Biomes.BADLANDS, Biomes.ERODED_BADLANDS,
            Biomes.WOODED_BADLANDS, Biomes.DESERT, Biomes.STONY_PEAKS);
    private static final Set<ResourceKey<Biome>> FOX_HOMES = Set.of(Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.GROVE,
            Biomes.TAIGA, Biomes.BIRCH_FOREST, Biomes.FLOWER_FOREST, Biomes.CHERRY_GROVE);

    private WildSpawns() {}

    static void register(IEventBus bus) {
        bus.addListener((LevelEvent.PotentialSpawns event) -> golem(event));
        bus.addListener((TickEvent.PlayerTickEvent.Post event) -> wildlife(event));
    }

    /** The golem joins the monsters of the overworld; its own spawn rule keeps it very rare. */
    private static void golem(LevelEvent.PotentialSpawns event) {
        if (event.getMobCategory() == MobCategory.MONSTER && event.getLevel() instanceof ServerLevel level
                && level.dimension() == Level.OVERWORLD) {
            event.addSpawnerData(new MobSpawnSettings.SpawnerData(Creatures.GOLEM.get(), 4, 1, 1));
        }
    }

    /** Now and then a wild creature appears near a player who is exploring its homeland. */
    private static void wildlife(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player) || player.tickCount % 1200 != 600 || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        double angle = level.random.nextDouble() * Math.PI * 2.0;
        double distance = 28.0 + level.random.nextInt(24);
        BlockPos column = BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(),
                player.getZ() + Math.sin(angle) * distance);
        if (!level.isLoaded(column)) {
            return;
        }
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        ResourceKey<Biome> biome = level.getBiome(pos).unwrapKey().orElse(null);
        if (biome == null) {
            return;
        }
        if (GRIFFIN_HOMES.contains(biome)) {
            appear(level, player, pos, Creatures.GRIFFIN.get(), GoldenGriffin.class, 0.3F);
        } else if (DRAKE_HOMES.contains(biome)) {
            appear(level, player, pos, Creatures.DRAKE.get(), EmberDrake.class, 0.2F);
        } else if (FOX_HOMES.contains(biome)) {
            appear(level, player, pos, Creatures.FOX.get(), SpiritFox.class, 0.25F);
        }
    }

    /** At most one of a kind around a player, and only where an animal could stand. */
    private static <T extends Mob> void appear(ServerLevel level, ServerPlayer player, BlockPos pos, EntityType<T> type,
                                              Class<? extends Mob> kind, float chance) {
        if (level.random.nextFloat() < chance
                && level.getEntitiesOfClass(kind, player.getBoundingBox().inflate(160.0)).isEmpty()
                && level.getFluidState(pos.below()).isEmpty()
                && Mob.checkMobSpawnRules(type, level, MobSpawnType.NATURAL, pos, level.random)) {
            type.spawn(level, pos, MobSpawnType.NATURAL);
        }
    }
}
