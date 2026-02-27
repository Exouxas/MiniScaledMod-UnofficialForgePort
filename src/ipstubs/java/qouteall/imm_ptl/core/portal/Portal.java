package qouteall.imm_ptl.core.portal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceKey;
import qouteall.q_misc_util.my_util.DQuaternion;

public class Portal extends Entity {
    public double scaling = 1.0;
    public boolean teleportChangesScale = false;
    public boolean fuseView = false;
    public boolean renderingMergable = false;
    public boolean hasCrossPortalCollision = false;
    public boolean doRenderPlayer = true;
    public String portalTag;

    private boolean interactable = true;
    private Vec3 originPos = Vec3.ZERO;

    public Portal(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return null;
    }

    public void setDestinationDimension(ResourceKey<Level> dimensionKey) {
    }

    public void setDestinationDimension(ServerLevel level) {
    }

    public void setOriginPos(Vec3 pos) {
        this.originPos = pos;
    }

    public Vec3 getOriginPos() {
        return originPos;
    }

    public void setDestination(Vec3 pos) {
    }

    public void setOrientation(Vec3 axisW, Vec3 axisH) {
    }

    public void setWidth(double width) {
    }

    public void setHeight(double height) {
    }

    public void setRotation(DQuaternion quaternion) {
    }

    public void setTeleportChangesGravity(boolean val) {
    }

    public void setInteractable(boolean interactable) {
        this.interactable = interactable;
    }

    public boolean isInteractableBy(Player player) {
        return interactable;
    }

    public double getScale() {
        return scaling;
    }

    public Vec3 getNormal() {
        return new Vec3(0, 1, 0);
    }

    public Vec3 transformVelocityRelativeToPortal(Vec3 v, Entity entity) {
        return v;
    }

    public double getDestAreaRadiusEstimation() {
        return 0;
    }

    public boolean allowOverlappedTeleport() {
        return false;
    }

    public void onCollidingWithEntity(Entity entity) {
    }

    public boolean canTeleportEntity(Entity entity) {
        return true;
    }

    public double getDistanceToNearestPointInPortal(Vec3 point) {
        return Double.POSITIVE_INFINITY;
    }
}
