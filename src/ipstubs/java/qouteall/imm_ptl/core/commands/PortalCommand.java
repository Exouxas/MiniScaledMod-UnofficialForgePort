package qouteall.imm_ptl.core.commands;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.Optional;

public class PortalCommand {
    public static Optional<Pair<Portal, Vec3>> raytracePortals(Level world, Vec3 start, Vec3 end, boolean includeGlobal) {
        return Optional.empty();
    }
}
