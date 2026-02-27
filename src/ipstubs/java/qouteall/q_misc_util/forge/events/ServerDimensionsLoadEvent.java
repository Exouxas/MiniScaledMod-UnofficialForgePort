package qouteall.q_misc_util.forge.events;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraftforge.eventbus.api.Event;

public class ServerDimensionsLoadEvent extends Event {
    public WorldOptions generatorOptions;
    public RegistryAccess registryManager;

    public ServerDimensionsLoadEvent(WorldOptions generatorOptions, RegistryAccess registryManager) {
        this.generatorOptions = generatorOptions;
        this.registryManager = registryManager;
    }
}
