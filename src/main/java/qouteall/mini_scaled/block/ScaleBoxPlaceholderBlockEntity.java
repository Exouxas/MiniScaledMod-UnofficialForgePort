package qouteall.mini_scaled.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.mini_scaled.ScaleBoxGeneration;
import qouteall.mini_scaled.ScaleBoxRecord;
import qouteall.mini_scaled.item.ScaleBoxEntranceItem;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.IntBox;

public class ScaleBoxPlaceholderBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogManager.getLogger(ScaleBoxPlaceholderBlockEntity.class);
    
    public static BlockEntityType<ScaleBoxPlaceholderBlockEntity> blockEntityType;
    
    public static void init() {
        // Registration is handled by MiniScaledRegistries via DeferredRegister.
        // blockEntityType is assigned in FMLCommonSetupEvent.
    }
    
    public ScaleBoxPlaceholderBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }
    
    public int boxId;
    public boolean isBasePos = true;
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        boxId = tag.getInt("boxId");
        if (tag.contains("isBasePos")) {
            isBasePos = tag.getBoolean("isBasePos");
        }
    }
    
    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("boxId", boxId);
        tag.putBoolean("isBasePos", isBasePos);
    }
    
    public void doTick() {
        if (level.isClientSide()) {
            return;
        }
        
        if (level.getGameTime() % 7 != 2) {
            return;
        }
        
        checkValidity();
    }
    
    public static void staticTick(Level world, BlockPos pos, BlockState state, ScaleBoxPlaceholderBlockEntity blockEntity) {
        blockEntity.doTick();
    }
    
    public void checkValidity() {
        ScaleBoxRecord.Entry entry = ScaleBoxRecord.get().getEntryById(boxId);
        
        if (entry == null) {
            LOGGER.info("invalid box with id {}", boxId);
            destroyBlockAndBlockEntity();
            return;
        }
        
        IntBox scaleBoxOuterArea = entry.getOuterAreaBox();
        
        boolean posEquals = scaleBoxOuterArea.contains(getBlockPos());
        if (!posEquals) {
            LOGGER.info("invalid box entrance position {} {} {}", boxId, getBlockPos(), entry.currentEntrancePos);
            destroyBlockAndBlockEntity();
            return;
        }
        
        boolean dimEquals = entry.currentEntranceDim == level.dimension();
        if (!dimEquals) {
            LOGGER.info("invalid box dim {} {} {}", boxId, level.dimension(), entry.currentEntranceDim);
            destroyBlockAndBlockEntity();
            return;
        }
    }
    
    private void destroyBlockAndBlockEntity() {
        Validate.isTrue(!level.isClientSide());
        LOGGER.info("destroy scale box {}", boxId);
        level.setBlockAndUpdate(getBlockPos(), Blocks.AIR.defaultBlockState());
        setRemoved();
        
        dropItemIfNecessary();
        
        // don't notifyPortalBreak()
    }
    
    public void dropItemIfNecessary() {
        if (isBasePos) {
            // Guard: the server must be running to access ScaleBoxRecord.
            // During GameTest structure initialization the server may not be
            // ready yet, so we skip the item drop rather than crashing.
            if (MiscHelper.getServer() == null) {
                isBasePos = false;
                return;
            }

            // the up-facing outer portal breaks. drop item
            ItemStack itemToDrop = ScaleBoxEntranceItem.boxIdToItem(boxId);
            if (itemToDrop != null) {
                Containers.dropItemStack(
                    level, getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5, itemToDrop
                );
            }
            
            // avoid dropping item twice
            isBasePos = false;
        }
    }
    
    private static void notifyPortalBreak(int boxId) {
        ScaleBoxRecord.Entry entry = ScaleBoxRecord.get().getEntryById(boxId);
        if (entry != null) {
            entry.generation++;
            ScaleBoxRecord.get().setDirty(true);
        }
    }
    
    /**
     * Checks whether the portal for {@code boxId} should be removed, and if so,
     * drives the full cleanup sequence.
     *
     * <p>There are three cases:</p>
     * <ol>
     *   <li><b>Entrance already unplaced</b> ({@code currentEntranceDim == null}): The box was
     *       already cleaned up by an earlier call (e.g. a sibling placeholder block fired first).
     *       Stale blocks self-clean via {@link #checkValidity()}.  We must <em>not</em> call
     *       {@link #notifyPortalBreak} here — doing so would increment the generation again and
     *       immediately invalidate the void-pointing inner portals that were just created.</li>
     *   <li><b>Entrance placed elsewhere / integrity check passes</b>: The record already points
     *       to the new location; leave it alone.  The stale blocks at the old location will clean
     *       themselves up via {@link #checkValidity()}.</li>
     *   <li><b>Entrance actually destroyed</b>: Block(s) are gone.  Increment the generation,
     *       kill all old portals, and create void-pointing inner portals.</li>
     * </ol>
     */
    public static void checkShouldRemovePortals(
        int boxId,
        ServerLevel world,
        BlockPos pos
    ) {
        // Guard: nothing to do if the server isn't ready yet.
        if (MiscHelper.getServer() == null) {
            return;
        }

        ScaleBoxRecord record = ScaleBoxRecord.get();
        ScaleBoxRecord.Entry entry = record.getEntryById(boxId);
        
        if (entry == null) {
            return;
        }
        
        ResourceKey<Level> currentEntranceDim = entry.currentEntranceDim;
        if (currentEntranceDim == null) {
            // Case 1: entrance already unplaced.
            // A previous placeholder block's onRemove already ran the full cleanup.
            // Do NOT call notifyPortalBreak() — that would kill the freshly-created
            // void-pointing inner portals by bumping the generation a second time.
            return;
        }
        
        ServerLevel entranceWorld = MiscHelper.getServer().getLevel(currentEntranceDim);
        if (entranceWorld == null) {
            // The entrance dimension no longer exists — treat as destroyed.
            LOGGER.warn("Scale box {} entrance dimension {} is gone, clearing entrance", boxId, currentEntranceDim);
            // Outer world is gone so we can't reach those portals; just clear the list.
            entry.outerPortalIds.clear();
            entry.currentEntranceDim = null;
            record.setDirty(true);
            notifyPortalBreak(boxId);
            // Kill old inner portals by UUID and create new void-pointing ones.
            ScaleBoxGeneration.createInnerPortalsPointingToVoidUnderneath(entry);
            return;
        }
        
        boolean chunkLoaded = entranceWorld.hasChunkAt(entry.currentEntrancePos);
        if (!chunkLoaded) {
            // Cannot verify block state right now.
            // Outer portals handle themselves via checkStatus() every 2 ticks.
            // If the chunk truly never loads again, the periodic reconcilePortals()
            // sweep will eventually catch and clean up any orphaned portals.
            return;
        }
        
        boolean blocksValid = entry.getOuterAreaBox().stream().allMatch(blockPos ->
            entranceWorld.getBlockState(blockPos).getBlock() == ScaleBoxPlaceholderBlock.instance
        );
        
        if (!blocksValid) {
            // Case 3: entrance actually destroyed.
            // Kill the outer portals by UUID + spatial fallback.
            ScaleBoxGeneration.killOuterPortalsForEntry(entry, entranceWorld);

            entry.currentEntranceDim = null;
            record.setDirty(true);
            // Increment generation as a secondary safety net for any portals in unloaded
            // chunks that couldn't be reached by UUID — they self-discard within 2 ticks.
            notifyPortalBreak(boxId);

            // Kill old inner portals by UUID and create new void-pointing ones.
            ScaleBoxGeneration.createInnerPortalsPointingToVoidUnderneath(entry);
        }
    }
    
    
}
