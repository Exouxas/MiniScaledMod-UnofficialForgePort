package qouteall.imm_ptl.core.render;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import qouteall.imm_ptl.core.portal.Portal;

/**
 * Stub matching the Forge ImmersivePortals 3.0.7 API.
 * PortalRenderer lives at qouteall.imm_ptl.core.render.PortalRenderer.
 * Portal rendering can be blocked by listening to DoRenderPortalEvent on MinecraftForge.EVENT_BUS
 * and calling event.setCanceled(true).
 */
public abstract class PortalRenderer {

    /**
     * Fired on MinecraftForge.EVENT_BUS before a portal is rendered.
     * Cancel this event to suppress rendering of the portal.
     */
    @Cancelable
    public static final class DoRenderPortalEvent extends Event {
        public final Portal portal;

        public DoRenderPortalEvent(Portal portal) {
            this.portal = portal;
        }
    }
}
