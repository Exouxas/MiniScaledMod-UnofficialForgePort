package qouteall.q_misc_util.my_util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

public class IntMatrix3 {
    public final Vec3i x;
    public final Vec3i y;
    public final Vec3i z;

    public IntMatrix3(Vec3i x, Vec3i y, Vec3i z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockPos transform(Vec3i v) {
        return BlockPos.ZERO;
    }

    public IntMatrix3 multiply(IntMatrix3 other) {
        return this;
    }

    public Direction transformDirection(Direction dir) {
        return dir;
    }

    public static IntMatrix3 getIdentity() {
        return new IntMatrix3(Vec3i.ZERO, Vec3i.ZERO, Vec3i.ZERO);
    }

    public DQuaternion toQuaternion() {
        return new DQuaternion();
    }
}
