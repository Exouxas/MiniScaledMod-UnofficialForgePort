package qouteall.imm_ptl.core.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import qouteall.imm_ptl.core.portal.Portal;

public class PortalEntityRenderer extends EntityRenderer<Portal> {
    public PortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Portal entity) {
        return null;
    }
}
