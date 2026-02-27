package qouteall.q_misc_util.my_util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

public enum AARotation {
    SOUTH_ROT0, SOUTH_ROT90, SOUTH_ROT180, SOUTH_ROT270,
    NORTH_ROT0, NORTH_ROT90, NORTH_ROT180, NORTH_ROT270,
    EAST_ROT0,  EAST_ROT90,  EAST_ROT180,  EAST_ROT270,
    WEST_ROT0,  WEST_ROT90,  WEST_ROT180,  WEST_ROT270,
    UP_ROT0,    UP_ROT90,    UP_ROT180,    UP_ROT270,
    DOWN_ROT0,  DOWN_ROT90,  DOWN_ROT180,  DOWN_ROT270,
    IDENTITY;

    public final IntMatrix3 matrix = IntMatrix3.getIdentity();
    public final DQuaternion quaternion = new DQuaternion();

    public BlockPos transform(Vec3i pos) {
        return BlockPos.ZERO;
    }

    public Direction transformDirection(Direction dir) {
        return dir;
    }

    public AARotation getInverse() {
        return this;
    }

    public static AARotation getAARotationFromYZ(Direction y, Direction z) {
        return IDENTITY;
    }

    public static AARotation getAARotationFromZX(Direction z, Direction x) {
        return IDENTITY;
    }

    public static AARotation getAARotationFromXY(Direction x, Direction y) {
        return IDENTITY;
    }

    public AARotation multiply(AARotation other) {
        return this;
    }

    public static Direction dirCrossProduct(Direction a, Direction b) {
        return Direction.UP;
    }

    public static Direction rotateDir90DegreesAlong(Direction dir, Direction axis) {
        return dir;
    }

    public static AARotation get90DegreesRotationAlong(Direction axis) {
        return IDENTITY;
    }
}
