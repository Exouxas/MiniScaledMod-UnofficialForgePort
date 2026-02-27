package qouteall.q_misc_util.api;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.core.RegistryAccess;

import java.util.function.BiConsumer;

public class DimensionAPI {
    public static final SimpleEvent<BiConsumer<WorldOptions, RegistryAccess>> serverDimensionsLoadEvent = new SimpleEvent<>();

    public static void addDimension(
        Registry<LevelStem> levelStemRegistry,
        ResourceLocation dimensionId,
        Holder<DimensionType> dimensionType,
        ChunkGenerator generator
    ) {
    }

    public static class SimpleEvent<T> {
        public void register(T listener) {
        }
    }
}
