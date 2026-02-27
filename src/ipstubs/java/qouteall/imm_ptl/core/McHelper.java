package qouteall.imm_ptl.core;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class McHelper {
    public static ServerLevel getServerWorld(ResourceKey<Level> key) {
        return null;
    }

    public static ServerLevel getServerWorld(net.minecraft.resources.ResourceLocation key) {
        return null;
    }

    public static ServerLevel getOverWorldOnServer() {
        return null;
    }

    public static MinecraftServer getServer() {
        return null;
    }

    public static void spawnServerEntity(Entity entity) {
    }

    public static void updateBoundingBox(Entity entity) {
    }

    public static Component getDimensionName(ResourceKey<Level> key) {
        return Component.literal(String.valueOf(key));
    }

    public static int getRenderDistanceOnServer() {
        return 8;
    }
}
