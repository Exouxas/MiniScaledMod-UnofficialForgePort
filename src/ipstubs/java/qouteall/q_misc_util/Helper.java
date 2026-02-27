package qouteall.q_misc_util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.stream.Stream;

public class Helper {
    public static void log(Object msg) {
        System.out.println(msg);
    }

    public static Direction[] getAnotherFourDirections(Direction.Axis axis) {
        return new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    }

    public static Tuple<Direction, Direction> getPerpendicularDirections(Direction dir) {
        return new Tuple<>(Direction.NORTH, Direction.EAST);
    }

    public static AABB getBoxSurface(AABB box, Direction dir) {
        return box;
    }

    public static Vec3 getBoxSize(AABB box) {
        return new Vec3(box.getXsize(), box.getYsize(), box.getZsize());
    }

    public static int getCoordinate(Vec3i pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    public static double getCoordinate(Vec3 vec, Direction.Axis axis) {
        return switch (axis) {
            case X -> vec.x;
            case Y -> vec.y;
            case Z -> vec.z;
        };
    }

    public static BlockPos putCoordinate(Vec3i pos, Direction.Axis axis, int value) {
        return switch (axis) {
            case X -> new BlockPos(value, pos.getY(), pos.getZ());
            case Y -> new BlockPos(pos.getX(), value, pos.getZ());
            case Z -> new BlockPos(pos.getX(), pos.getY(), value);
        };
    }

    public static BlockPos getVec3i(CompoundTag tag, String key) {
        return BlockPos.ZERO;
    }

    public static void putVec3i(CompoundTag tag, String key, Vec3i pos) {
    }

    public static long secondToNano(double seconds) {
        return (long) (seconds * 1_000_000_000L);
    }
}
