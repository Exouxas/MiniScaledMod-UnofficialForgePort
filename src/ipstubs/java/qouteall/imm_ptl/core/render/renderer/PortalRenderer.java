package qouteall.imm_ptl.core.render.renderer;

import net.minecraftforge.eventbus.api.Event;
import qouteall.imm_ptl.core.portal.Portal;

/**
 * Stub for iPortalTeam/ImmersivePortalsModForNeo.
 * The real class lives at qouteall.imm_ptl.core.render.renderer.PortalRenderer.
 *
 * Portal rendering filtering is done via PortalRenderingPredicateEvent, posted on
 * the NeoForge/Forge event bus. Handlers can call event.setCanRender(false) to skip
 * rendering a specific portal.
 */
public abstract class PortalRenderer {

    /**
     * Fired (cancellably) before each portal is rendered.
     * All listeners' results are ANDed — a single setCanRender(false) suppresses rendering.
     */
    public static class PortalRenderingPredicateEvent extends Event {
        public final Portal portal;
        private boolean canRender = true;

        public PortalRenderingPredicateEvent(Portal portal) {
            this.portal = portal;
        }

        public void setCanRender(boolean canRender) {
            this.canRender = this.canRender && canRender;
        }

        public boolean canRender() {
            return canRender;
        }
    }
}
