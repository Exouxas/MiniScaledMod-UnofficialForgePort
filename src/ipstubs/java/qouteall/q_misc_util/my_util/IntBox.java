package qouteall.q_misc_util.my_util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.stream.Stream;

public class IntBox {
    public static IntBox fromBasePointAndSize(BlockPos basePos, BlockPos size) {
        return new IntBox();
    }

    public static IntBox getBoxByPosAndSignedSize(BlockPos basePos, BlockPos signedSize) {
        return new IntBox();
    }

    public Stream<BlockPos> stream() {
        return Stream.empty();
    }

    public Stream<BlockPos> fastStream() {
        return Stream.empty();
    }

    public boolean contains(BlockPos pos) {
        return false;
    }

    public BlockPos getSize() {
        return BlockPos.ZERO;
    }

    public BlockPos getCenter() {
        return BlockPos.ZERO;
    }

    public Vec3 getCenterVec() {
        return Vec3.ZERO;
    }

    public boolean isOnEdge(BlockPos pos) {
        return false;
    }

    public AABB toRealNumberBox() {
        return new AABB(0, 0, 0, 0, 0, 0);
    }

    public IntBox getAdjusted(int dx1, int dy1, int dz1, int dx2, int dy2, int dz2) {
        return this;
    }

    public IntBox getSurfaceLayer(Direction direction) {
        return this;
    }

    public IntBox[] get12Edges() {
        return new IntBox[0];
    }

    public BlockPos[] getEightVertices() {
        return new BlockPos[0];
    }
}
