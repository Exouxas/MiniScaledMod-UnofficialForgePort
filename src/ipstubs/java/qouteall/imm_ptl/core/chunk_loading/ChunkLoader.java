package qouteall.imm_ptl.core.chunk_loading;

public class ChunkLoader {
    public final DimensionalChunkPos center;
    public final int radius;

    public ChunkLoader(DimensionalChunkPos center, int radius) {
        this.center = center;
        this.radius = radius;
    }

    public void loadChunksAndDo(Runnable task) {
        if (task != null) {
            task.run();
        }
    }
}
