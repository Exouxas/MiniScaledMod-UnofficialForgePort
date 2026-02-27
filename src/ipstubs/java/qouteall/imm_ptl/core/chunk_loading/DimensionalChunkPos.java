package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public record DimensionalChunkPos(ResourceKey<Level> dimension, ChunkPos chunkPos) {
}
