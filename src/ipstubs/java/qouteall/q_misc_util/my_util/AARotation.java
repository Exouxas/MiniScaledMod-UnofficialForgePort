package qouteall.q_misc_util.my_util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public enum AARotation {
    IDENTITY;

    public final StubMatrix matrix = new StubMatrix();

    public BlockPos transform(BlockPos pos) {
        return pos;
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

    public static class StubMatrix {
        public DQuaternion toQuaternion() {
            return new DQuaternion();
        }
    }
}
