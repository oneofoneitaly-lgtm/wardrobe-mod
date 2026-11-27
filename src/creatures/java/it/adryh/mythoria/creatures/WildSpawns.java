package it.adryh.mythoria.creatures;

import it.adryh.mythoria.creatures.entity.GoldenGriffin;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
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

    private WildSpawns() {}

    static void register(IEventBus bus) {
        bus.addListener((LevelEvent.PotentialSpawns event) -> golem(event));
        bus.addListener((TickEvent.PlayerTickEvent.Post event) -> griffins(event));
    }

    /** The golem joins the monsters of the overworld; its own spawn rule keeps it very rare. */
    private static void golem(LevelEvent.PotentialSpawns event) {
        if (event.getMobCategory() == MobCategory.MONSTER && event.getLevel() instanceof ServerLevel level
                && level.dimension() == Level.OVERWORLD) {
            event.addSpawnerData(new MobSpawnSettings.SpawnerData(Creatures.GOLEM.get(), 4, 1, 1));
        }
    }

    /** Now and then a wild griffin lands near a player who is exploring open country. */
    private static void griffins(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player) || player.tickCount % 1200 != 600 || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                || level.random.nextFloat() > 0.3F
                || !level.getEntitiesOfClass(GoldenGriffin.class, player.getBoundingBox().inflate(160.0)).isEmpty()) {
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
        if (level.getBiome(pos).unwrapKey().filter(GRIFFIN_HOMES::contains).isPresent()
                && Animal.checkAnimalSpawnRules(Creatures.GRIFFIN.get(), level, MobSpawnType.NATURAL, pos, level.random)) {
            Creatures.GRIFFIN.get().spawn(level, pos, MobSpawnType.NATURAL);
        }
    }
}
