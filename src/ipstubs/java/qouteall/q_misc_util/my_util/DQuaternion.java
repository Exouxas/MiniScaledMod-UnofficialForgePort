package qouteall.q_misc_util.my_util;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class DQuaternion {
    public static DQuaternion getCameraRotation(double pitchDegrees, double yawDegrees) {
        return new DQuaternion();
    }

    public Quaternionf toMcQuaternion() {
        return new Quaternionf();
    }

    public DQuaternion getConjugated() {
        return new DQuaternion();
    }

    public Vec3 rotate(Vec3 vec) {
        return vec;
    }
}
