package qouteall.dimlib.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.function.Consumer;

public class DimensionAPI {
    public static final SimpleEvent<Consumer<MinecraftServer>> SERVER_DIMENSIONS_LOAD_EVENT = new SimpleEvent<>();

    public static void addDimension(
        MinecraftServer server,
        ResourceLocation dimensionId,
        LevelStem levelStem
    ) {
    }

    public static void addDimensionIfNotExists(
        MinecraftServer server,
        ResourceLocation dimensionId,
        java.util.function.Supplier<LevelStem> levelStemSupplier
    ) {
    }

    public static class SimpleEvent<T> {
        public void register(T listener) {
        }
    }
}
