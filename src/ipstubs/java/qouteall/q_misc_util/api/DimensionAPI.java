package qouteall.q_misc_util.api;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class DimensionAPI {
    public static final SimpleEvent<BiConsumer<WorldOptions, RegistryAccess>> serverDimensionsLoadEvent = new SimpleEvent<>();

    /**
     * Registers a new dimension by adding its LevelStem to the level-stem registry
     * before Minecraft's createLevels() iterates it.
     *
     * The registry is frozen by the time this is called, so we unfreeze it via
     * reflection, add the entry, then re-freeze — mirroring what the real ImmPTL
     * DimensionAPI does under the hood.
     */
    public static void addDimension(
        Registry<LevelStem> levelStemRegistry,
        ResourceLocation dimensionId,
        Holder<DimensionType> dimensionType,
        ChunkGenerator generator
    ) {
        ResourceKey<LevelStem> stemKey = ResourceKey.create(Registries.LEVEL_STEM, dimensionId);
        if (levelStemRegistry.containsKey(stemKey)) {
            return; // idempotent – already registered
        }
        LevelStem stem = new LevelStem(dimensionType, generator);
        if (levelStemRegistry instanceof MappedRegistry<LevelStem> mapped) {
            try {
                Field frozenField = MappedRegistry.class.getDeclaredField("frozen");
                frozenField.setAccessible(true);
                frozenField.set(mapped, false);
                mapped.register(stemKey, stem, Lifecycle.stable());
                frozenField.set(mapped, true);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to register dimension " + dimensionId, e);
            }
        }
    }

    public static class SimpleEvent<T> {
        private final List<T> listeners = new ArrayList<>();

        public void register(T listener) {
            listeners.add(listener);
        }
    }
}
