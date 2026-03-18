package qouteall.mini_scaled;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.mini_scaled.block.ScaleBoxPlaceholderBlock;
import qouteall.mini_scaled.block.ScaleBoxPlaceholderBlockEntity;
import qouteall.mini_scaled.config.MiniScaledConfig;
import qouteall.mini_scaled.item.ManipulationWandItem;
import qouteall.mini_scaled.item.ScaleBoxEntranceItem;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.forge.events.ServerDimensionsLoadEvent;

@Mod("mini_scaled")
public class MiniScaledModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiniScaledModInitializer.class);
    
    public MiniScaledModInitializer() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register deferred registries
        MiniScaledRegistries.register(modEventBus);
        
        // Mod lifecycle events (fire during mod loading)
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onBuildCreativeTab);
        
        // Game events (fire during gameplay)
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerDimensionsLoad);
        // Also register via the BiConsumer API so the void dimension is created
        // regardless of which mechanism the real ImmPTL uses to signal dimension loading.
        qouteall.q_misc_util.api.DimensionAPI.serverDimensionsLoadEvent.register(
            (worldOptions, registryAccess) -> VoidDimension.initializeVoidDimension(registryAccess)
        );
        
        // Register config
        MSGlobal.config = AutoConfig.register(MiniScaledConfig.class, GsonConfigSerializer::new);
        
        // Config save listener
        AutoConfig.getConfigHolder(MiniScaledConfig.class).registerSaveListener((configHolder, config) -> {
            if (MiscHelper.getServer() != null) {
                applyConfigServerSide(config);
            }
            applyConfigClientSide(config);
            return InteractionResult.PASS;
        });
        
        // Register client-side events (only on client)
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MiniScaledModInitializerClient.registerClientEvents(modEventBus);
        }
        
        LOGGER.info("MiniScaled Mod Initializing");
    }
    
    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Assign entity/block entity types from deferred register after registration
            MiniScaledPortal.entityType = MiniScaledRegistries.getPortalEntityType();
            ScaleBoxPlaceholderBlockEntity.blockEntityType = MiniScaledRegistries.getPlaceholderBeType();
            
            IPGlobal.enableDepthClampForPortalRendering = true;
        });
    }
    
    private void onServerDimensionsLoad(ServerDimensionsLoadEvent event) {
        VoidDimension.initializeVoidDimension(event.registryManager);
    }
    
    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                FallenEntityTeleportaion.teleportFallenEntities(server);
            }
        }
    }
    
    private void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level world = event.getLevel();
        BlockHitResult hitResult = event.getHitVec();
        
        // Handle scale box placeholder block interactions
        Block block = world.getBlockState(hitResult.getBlockPos()).getBlock();
        if (block == ScaleBoxPlaceholderBlock.instance) {
            InteractionResult result = ScaleBoxManipulation.onHandRightClickEntrance(
                player, world, event.getHand(), hitResult
            );
            if (result != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(result);
                return;
            }
        }
        
        // Handle scale box creation using the configured creation item
        InteractionResult result = ScaleBoxEntranceCreation.onRightClickBlock(
            player, world, event.getHand(), hitResult
        );
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
    
    private void onRegisterCommands(RegisterCommandsEvent event) {
        MiniScaledCommand.register(event.getDispatcher());
    }
    
    private void onServerStarted(ServerStartedEvent event) {
        MiniScaledConfig config = AutoConfig.getConfigHolder(MiniScaledConfig.class).getConfig();
        applyConfigServerSide(config);
    }
    
    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            ManipulationWandItem.registerCreativeInventory(event::accept);
            ScaleBoxEntranceItem.registerCreativeInventory(event::accept);
        }
    }
    
    public static void applyConfigServerSide(MiniScaledConfig miniScaledConfig) {
        try {
            ResourceLocation identifier = new ResourceLocation(miniScaledConfig.creationItem);
            
            Item creationItem = BuiltInRegistries.ITEM.get(identifier);
            
            if (creationItem != Items.AIR) {
                ScaleBoxEntranceCreation.creationItem = creationItem;
            }
            else {
                LOGGER.error("Invalid scale box creation item {}", identifier);
                ScaleBoxEntranceCreation.creationItem = Items.NETHERITE_INGOT;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void applyConfigClientSide(MiniScaledConfig miniScaledConfig) {
    
    }
}

