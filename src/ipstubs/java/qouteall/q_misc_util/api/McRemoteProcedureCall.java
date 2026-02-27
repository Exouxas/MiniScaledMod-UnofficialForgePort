package qouteall.q_misc_util.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public class McRemoteProcedureCall {
    public static void tellServerToInvoke(String callableName, Object... args) {
    }

    public static void tellClientToInvoke(ServerPlayer player, String callableName, CompoundTag tag) {
    }
}
