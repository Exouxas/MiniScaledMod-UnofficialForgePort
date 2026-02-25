package qouteall.mini_scaled;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import qouteall.imm_ptl.core.render.PortalEntityRenderer;
import qouteall.mini_scaled.gui.ScaleBoxManagementScreen;

@OnlyIn(Dist.CLIENT)
public class MiniScaledModInitializerClient {
    
    public static void registerClientEvents(IEventBus modEventBus) {
        modEventBus.addListener(MiniScaledModInitializerClient::onRegisterRenderers);
        modEventBus.addListener(MiniScaledModInitializerClient::onFMLClientSetup);
        MinecraftForge.EVENT_BUS.addListener(MiniScaledModInitializerClient::onClientTick);
    }
    
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
            MiniScaledPortal.entityType,
            PortalEntityRenderer::new
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

