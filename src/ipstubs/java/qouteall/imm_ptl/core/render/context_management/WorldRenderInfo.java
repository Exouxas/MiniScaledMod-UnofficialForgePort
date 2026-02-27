package qouteall.imm_ptl.core.render.context_management;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class WorldRenderInfo {
    public static class Builder {
        public Builder setWorld(ClientLevel world) {return this;}

        public Builder setCameraPos(Vec3 pos) {return this;}

        public Builder setCameraTransformation(Matrix4f matrix) {return this;}

        public Builder setOverwriteCameraTransformation(boolean v) {return this;}

        public Builder setDescription(UUID id) {return this;}

        public Builder setDoRenderHand(boolean v) {return this;}

        public Builder setEnableViewBobbing(boolean v) {return this;}

        public Builder setDoRenderSky(boolean v) {return this;}

        public WorldRenderInfo build() {return new WorldRenderInfo();}
    }

    public static List<UUID> getRenderingDescription() {
        return Collections.emptyList();
    }
}
