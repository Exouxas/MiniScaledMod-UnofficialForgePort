package qouteall.mini_scaled;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import qouteall.mini_scaled.block.BoxBarrierBlock;
import qouteall.mini_scaled.block.ScaleBoxPlaceholderBlock;
import qouteall.mini_scaled.block.ScaleBoxPlaceholderBlockEntity;
import qouteall.mini_scaled.item.ManipulationWandItem;
import qouteall.mini_scaled.item.ScaleBoxEntranceItem;

public class MiniScaledRegistries {
    
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, "mini_scaled");
    
    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, "mini_scaled");
    
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "mini_scaled");
    
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "mini_scaled");
    
    public static final RegistryObject<net.minecraft.world.level.block.Block> SCALE_BOX_PLACEHOLDER_BLOCK =
        BLOCKS.register("scale_box_placeholder", () -> ScaleBoxPlaceholderBlock.instance);
    
    public static final RegistryObject<net.minecraft.world.level.block.Block> BARRIER_BLOCK =
        BLOCKS.register("barrier", () -> BoxBarrierBlock.instance);
    
    public static final RegistryObject<net.minecraft.world.item.Item> SCALE_BOX_ENTRANCE_ITEM =
        ITEMS.register("scale_box_item", () -> ScaleBoxEntranceItem.instance);
    
    public static final RegistryObject<net.minecraft.world.item.Item> MANIPULATION_WAND_ITEM =
        ITEMS.register("manipulation_wand", () -> ManipulationWandItem.instance);
    
    public static final RegistryObject<BlockEntityType<ScaleBoxPlaceholderBlockEntity>> PLACEHOLDER_BE_TYPE =
        BLOCK_ENTITY_TYPES.register("placeholder_block_entity",
            () -> BlockEntityType.Builder.of(
                ScaleBoxPlaceholderBlockEntity::new,
                ScaleBoxPlaceholderBlock.instance
            ).build(null)
        );
    
    private static final RegistryObject<EntityType<?>> PORTAL_ENTITY_TYPE_RAW =
        ENTITY_TYPES.register("portal",
            () -> EntityType.Builder.of(
                MiniScaledPortal::new, MobCategory.MISC
            ).sized(1f, 1f).fireImmune().updateInterval(20).clientTrackingRange(6).build("mini_scaled:portal")
        );
    
    @SuppressWarnings("unchecked")
    public static EntityType<MiniScaledPortal> getPortalEntityType() {
        return (EntityType<MiniScaledPortal>) PORTAL_ENTITY_TYPE_RAW.get();
    }
    
    public static BlockEntityType<ScaleBoxPlaceholderBlockEntity> getPlaceholderBeType() {
        return PLACEHOLDER_BE_TYPE.get();
    }
    
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
    }
}
