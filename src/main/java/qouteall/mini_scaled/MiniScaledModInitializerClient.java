package qouteall.mini_scaled;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import qouteall.imm_ptl.core.render.PortalEntityRenderer;
import qouteall.mini_scaled.gui.ScaleBoxManagementScreen;
import qouteall.mini_scaled.item.ScaleBoxEntranceItem;

@OnlyIn(Dist.CLIENT)
public class MiniScaledModInitializerClient {
    
    public static void registerClientEvents(IEventBus modEventBus) {
        modEventBus.addListener(MiniScaledModInitializerClient::onRegisterRenderers);
        modEventBus.addListener(MiniScaledModInitializerClient::onFMLClientSetup);
        modEventBus.addListener(MiniScaledModInitializerClient::onRegisterItemColors);
        MinecraftForge.EVENT_BUS.addListener(MiniScaledModInitializerClient::onClientTick);
    }

    private static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
            (stack, tintIndex) -> ScaleBoxEntranceItem.getRenderingColor(stack),
            ScaleBoxEntranceItem.instance
        );
    }
    
    @SuppressWarnings("unchecked")
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // PortalEntityRenderer extends EntityRenderer<Portal>, but MiniScaledPortal extends Portal.
        // The cast is safe because the renderer handles all Portal subtypes.
        // Use getPortalEntityType() directly instead of MiniScaledPortal.entityType, because
        // the static field is assigned in FMLCommonSetupEvent.enqueueWork() which runs AFTER
        // EntityRenderersEvent.RegisterRenderers fires, leaving it null and causing a crash.
        event.registerEntityRenderer(
            MiniScaledRegistries.getPortalEntityType(),
            ctx -> (EntityRenderer<MiniScaledPortal>)(Object) new PortalEntityRenderer(ctx)
        );
    }
    
    private static void onFMLClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            qouteall.mini_scaled.config.MiniScaledConfigMenu.register();
            ClientScaleBoxInteractionControl.init();
            ScaleBoxManagementScreen.init_();
        });
    }
    
    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientScaleBoxInteractionControl.update();
        }
    }
}

