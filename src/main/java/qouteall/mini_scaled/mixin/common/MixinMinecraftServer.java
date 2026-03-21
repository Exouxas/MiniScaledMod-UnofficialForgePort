package qouteall.mini_scaled.mixin.common;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.mini_scaled.ducks.MiniScaled_MinecraftServerAccessor;
import qouteall.mini_scaled.gui.ScaleBoxGuiManager;
import qouteall.q_misc_util.forge.events.ServerDimensionsLoadEvent;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer implements MiniScaled_MinecraftServerAccessor {
    @Unique
    private ScaleBoxGuiManager miniScaled_scaleBoxGuiManager;
    
    @Override
    public ScaleBoxGuiManager miniScaled_getScaleBoxGuiManager() {
        if (miniScaled_scaleBoxGuiManager == null) {
            miniScaled_scaleBoxGuiManager = new ScaleBoxGuiManager((MinecraftServer) (Object) this);
        }
        
        return miniScaled_scaleBoxGuiManager;
    }

    /**
     * Fire ServerDimensionsLoadEvent (ImmPTL's custom event) right before Minecraft
     * iterates LevelStems to create ServerLevels. At this point data packs are fully
     * loaded so the DimensionType registry is available, and we can still add new
     * LevelStems because createLevels() hasn't iterated the registry yet.
     *
     * This replaces ImmPTL's own mixin hook when running with compile-only stubs.
     * When real ImmPTL is present it fires the event via its own mechanism, so this
     * injection is redundant but harmless (addDimension is idempotent).
     */
    @Inject(method = "createLevels", at = @At("HEAD"))
    private void mini_scaled_fireDimensionLoadEvent(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        MinecraftForge.EVENT_BUS.post(new ServerDimensionsLoadEvent(
            server.getWorldData().worldGenOptions(),
            server.registryAccess()
        ));
    }
}
