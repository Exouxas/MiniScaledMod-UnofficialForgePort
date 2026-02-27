package qouteall.imm_ptl.core.portal;

public class PortalExtension {
    public boolean adjustPositionAfterTeleport = false;

    public static PortalExtension get(Portal portal) {
        return new PortalExtension();
    }
}
