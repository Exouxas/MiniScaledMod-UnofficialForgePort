package qouteall.q_misc_util;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class MiscHelper {
    public static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }
}
