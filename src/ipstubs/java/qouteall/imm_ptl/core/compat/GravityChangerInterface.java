package qouteall.imm_ptl.core.compat;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

public interface GravityChangerInterface {
    Invoker invoker = entity -> Direction.DOWN;

    interface Invoker {
        Direction getGravityDirection(Entity entity);
    }
}
