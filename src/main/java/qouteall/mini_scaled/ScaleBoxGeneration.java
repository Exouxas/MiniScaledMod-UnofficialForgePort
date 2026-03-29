package qouteall.mini_scaled;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.chunk_loading.ChunkLoader;
import qouteall.imm_ptl.core.chunk_loading.DimensionalChunkPos;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.mini_scaled.MiniScaledPortal;
import qouteall.mini_scaled.block.BoxBarrierBlock;
import qouteall.mini_scaled.block.ScaleBoxPlaceholderBlock;
import qouteall.mini_scaled.block.ScaleBoxPlaceholderBlockEntity;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.AARotation;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.IntBox;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ScaleBoxGeneration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScaleBoxGeneration.class);
    
    public static final int[] supportedScales = {4, 8, 16, 32};
    
    public static void putScaleBoxIntoWorld(
        ScaleBoxRecord.Entry entry,
        ServerLevel world, BlockPos outerBoxBasePos,
        AARotation rotation,
        ServerPlayer player
    ) {
        if (entry.accessControl) {
            if (!Objects.equals(entry.ownerId, player.getUUID())) {
                ScaleBoxManipulation.showScaleBoxAccessDeniedMessage(player);
                return;
            }
        }
        
        // Kill all registered portals before creating new ones.
        // Do this before updating the entry so we still know the old entrance dimension.
        ServerLevel voidWorld = VoidDimension.getVoidServerWorld();
        ResourceKey<Level> oldDim = entry.currentEntranceDim;
        killPortalsByIds(entry.outerPortalIds, oldDim != null ? McHelper.getServerWorld(oldDim) : null);
        killPortalsByIds(entry.innerPortalIds, voidWorld);

        entry.currentEntranceDim = world.dimension();
        entry.currentEntrancePos = outerBoxBasePos;
        entry.entranceRotation = rotation;
        entry.generation++;

        ScaleBoxRecord.get().setDirty(true);

        if (voidWorld == null) {
            LOGGER.error("Void world is not loaded yet, cannot place scale box portals for entry {}", entry.id);
            return;
        }

        createScaleBoxPortals(voidWorld, world, entry);
        
        entry.getOuterAreaBox().stream().forEach(outerPos -> {
            world.setBlockAndUpdate(outerPos, ScaleBoxPlaceholderBlock.instance.defaultBlockState());
            
            BlockEntity blockEntity = world.getBlockEntity(outerPos);
            if (blockEntity == null) {
                LOGGER.info("cannot find block entity for scale box");
            }
            else {
                ScaleBoxPlaceholderBlockEntity be = (ScaleBoxPlaceholderBlockEntity) blockEntity;
                be.boxId = entry.id;
                be.isBasePos = outerPos.equals(entry.currentEntrancePos);
            }
        });
    }
    
    static void createScaleBoxPortals(
        ServerLevel innerWorld,
        ServerLevel outerWorld,
        ScaleBoxRecord.Entry entry
    ) {
        AARotation entranceRotation = entry.getEntranceRotation();
        AARotation toInnerRotation = entranceRotation.getInverse();
        AABB outerAreaBox = entry.getOuterAreaBox().toRealNumberBox();
        AABB innerAreaBox = entry.getInnerAreaBox().toRealNumberBox();
        BlockPos outerAreaBoxSize = entry.getOuterAreaBox().getSize();
        int scale = entry.scale;
        DQuaternion quaternion = toInnerRotation.matrix.toQuaternion();
        int boxId = entry.id;
        int generation = entry.generation;
        entry.outerPortalIds.clear();
        entry.innerPortalIds.clear();
        
        for (Direction outerDirection : Direction.values()) {
            MiniScaledPortal portal = MiniScaledPortal.entityType.create(outerWorld);
            Validate.notNull(portal);
            
            portal.setDestinationDimension(innerWorld.dimension());
            
            Direction innerDirection = toInnerRotation.transformDirection(outerDirection);
            
            portal.setOriginPos(Helper.getBoxSurface(outerAreaBox, outerDirection).getCenter());
            portal.setDestination(Helper.getBoxSurface(innerAreaBox, innerDirection).getCenter());
            
            Tuple<Direction, Direction> perpendicularDirections = Helper.getPerpendicularDirections(outerDirection);
            Direction pd1 = perpendicularDirections.getA();
            Direction pd2 = perpendicularDirections.getB();
            
            portal.setOrientation(Vec3.atLowerCornerOf(pd1.getNormal()), Vec3.atLowerCornerOf(pd2.getNormal()));
            portal.setWidth(Helper.getCoordinate(outerAreaBoxSize, pd1.getAxis()));
            portal.setHeight(Helper.getCoordinate(outerAreaBoxSize, pd2.getAxis()));
            
            portal.setRotation(quaternion);
            
            portal.scaling = scale;
            portal.teleportChangesScale = entry.teleportChangesScale;
            portal.setTeleportChangesGravity(entry.teleportChangesGravity);
            portal.fuseView = true;
            portal.renderingMergable = true;
            portal.hasCrossPortalCollision = true;
            portal.portalTag = "mini_scaled:scaled_box";
            PortalExtension.get(portal).adjustPositionAfterTeleport = true;
            portal.setInteractable(true);
            portal.boxId = boxId;
            portal.generation = generation;
            portal.recordEntry = entry;
            
            McHelper.spawnServerEntity(portal);
            entry.outerPortalIds.add(portal.getUUID());
            
            MiniScaledPortal reversePortal =
                PortalManipulation.createReversePortal(portal, MiniScaledPortal.entityType);
            
            if (reversePortal == null) {
                LOGGER.error("createReversePortal returned null for direction {}, skipping reverse portal", outerDirection);
                continue;
            }
            
            reversePortal.fuseView = false;
            reversePortal.renderingMergable = true;
            reversePortal.hasCrossPortalCollision = true;
            reversePortal.setInteractable(true);
            reversePortal.boxId = boxId;
            reversePortal.generation = generation;
            reversePortal.recordEntry = entry;
            
            // When used with Iris, it renders normal portal instead of fuse view,
            // then when the player touches the portal, it wrongly renders the player.
            // It's a workaround to avoid this.
            reversePortal.doRenderPlayer = false;
            
            McHelper.spawnServerEntity(reversePortal);
            entry.innerPortalIds.add(reversePortal.getUUID());
        }
        ScaleBoxRecord.get().setDirty(true);
    }
    
    
    public static ScaleBoxRecord.Entry getOrCreateEntry(
        UUID playerId, String playerName, int scale, DyeColor color, ScaleBoxRecord record
    ) {
        Validate.notNull(playerId);
        
        ScaleBoxRecord.Entry entry = record.getEntriesByOwner(playerId).stream().filter(
            e -> e.color == color && e.scale == scale
        ).findFirst().orElse(null);
        
        if (entry == null) {
            int newId = record.allocateId();
            ScaleBoxRecord.Entry newEntry = new ScaleBoxRecord.Entry();
            newEntry.id = newId;
            newEntry.color = color;
            newEntry.ownerId = playerId;
            newEntry.ownerNameCache = playerName;
            newEntry.scale = scale;
            newEntry.generation = 0;
            newEntry.innerBoxPos = allocateInnerBoxPos(newId);
            newEntry.currentEntranceSize = new BlockPos(1, 1, 1);
            record.addEntry(newEntry);
            record.setDirty(true);
            
            initializeInnerBoxBlocks(null, newEntry);
            
            entry = newEntry;
        }
        return entry;
    }
    
    private static BlockPos allocateInnerBoxPos(int boxId) {
        int xIndex = boxId % 256;
        int zIndex = Mth.floorDiv(boxId, 256);
        
        return new BlockPos(xIndex * 16 * 32, 64, zIndex * 16 * 32);
    }
    
    public static BlockPos getNearestPosInScaleBoxToTeleportTo(BlockPos pos) {
        double gridLen = 16.0 * 32;
        return BlockPos.containing(
            Math.round(pos.getX() / gridLen) * gridLen + 2,
            64 + 2,
            Math.round(pos.getZ() / gridLen) * gridLen + 2
        );
    }
    
    public static void initializeInnerBoxBlocks(
        @Nullable BlockPos oldEntranceSize,
        ScaleBoxRecord.Entry entry
    ) {
        IntBox innerAreaBox = entry.getInnerAreaBox();
        
        ServerLevel voidWorld = VoidDimension.getVoidServerWorld();
        if (voidWorld == null) {
            LOGGER.error("Void world is not loaded yet, cannot initialize inner box blocks for entry {}", entry.id);
            return;
        }
        
        ChunkLoader chunkLoader = new ChunkLoader(
            new DimensionalChunkPos(
                voidWorld.dimension(),
                new ChunkPos(innerAreaBox.getCenter())
            ),
            Math.max(innerAreaBox.getSize().getX(), innerAreaBox.getSize().getZ()) / 16 + 2
        );
        
        Block glassBlock = getGlassBlock(entry.color);
        BlockState frameBlock = glassBlock.defaultBlockState();
        
        // set block after fulling loading the chunk
        // to avoid lighting problems
        chunkLoader.loadChunksAndDo(() -> {
            IntBox newEntranceOffsets = IntBox.fromBasePointAndSize(BlockPos.ZERO, entry.currentEntranceSize);
            IntBox oldEntranceOffsets = oldEntranceSize != null ?
                IntBox.fromBasePointAndSize(BlockPos.ZERO, oldEntranceSize) : null;
            
            // clear the barrier blocks if the scale box expanded
            newEntranceOffsets.stream().forEach(offset -> {
                if (oldEntranceOffsets != null) {
                    if (oldEntranceOffsets.contains(offset)) {
                        // if expanded, don't clear the existing regions
                        return;
                    }
                }
                
                entry.getInnerUnitBox(offset).fastStream().forEach(blockPos -> {
                    voidWorld.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                });
            });
            
            if (oldEntranceOffsets != null) {
                // clear the shrunk area's blocks
                oldEntranceOffsets.stream().forEach(offset -> {
                    if (newEntranceOffsets.contains(offset)) {
                        return;
                    }
                    
                    IntBox innerUnitBox = entry.getInnerUnitBox(offset);
                    
                    innerUnitBox.fastStream().forEach(blockPos -> {
                        voidWorld.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                    });
                });
            }
            
            // put the new barrier blocks
            IntBox expanded = innerAreaBox.getAdjusted(-1, -1, -1, 1, 1, 1);
            for (Direction direction : Direction.values()) {
                expanded.getSurfaceLayer(direction).fastStream().forEach(blockPos -> {
                    voidWorld.setBlockAndUpdate(blockPos, BoxBarrierBlock.instance.defaultBlockState());
                });
            }
            
            // find the untouched unit regions
            Set<BlockPos> untouchedRegionOffsets = newEntranceOffsets.stream().filter(
                offset -> isUnitRegionUntouched(entry, offset, voidWorld, frameBlock)
            ).map(BlockPos::immutable).collect(Collectors.toSet());
            
            // clear the untouched unit regions
            untouchedRegionOffsets.forEach(offset -> {
                IntBox innerUnitBox = entry.getInnerUnitBox(offset);
                
                innerUnitBox.fastStream().forEach(blockPos -> {
                    voidWorld.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                });
            });
            
            // regenerate the outer frame in the untouched unit regions
            for (IntBox edge : entry.getInnerAreaLocalBox().get12Edges()) {
                edge.fastStream().forEach(blockOffset -> {
                    BlockPos unitRegionOffset = entry.blockOffsetToUnitRegionOffset(blockOffset);
                    if (untouchedRegionOffsets.contains(unitRegionOffset)) {
                        BlockPos blockPos = entry.innerBoxPos.offset(blockOffset);
                        voidWorld.setBlockAndUpdate(blockPos, frameBlock);
                    }
                });
            }
            
        });
        
    }
    
    // untouched means it's all air, except that on the edge it can have frame blocks
    private static boolean isUnitRegionUntouched(
        ScaleBoxRecord.Entry entry,
        BlockPos regionOffset,
        ServerLevel voidWorld,
        BlockState frameBlock
    ) {
        IntBox innerUnitBox = entry.getInnerUnitBox(regionOffset);
        return innerUnitBox.fastStream().allMatch(blockPos -> {
            BlockState blockState = voidWorld.getBlockState(blockPos);
            if (innerUnitBox.isOnEdge(blockPos)) {
                return blockState.isAir() || blockState == frameBlock;
            }
            else {
                return blockState.isAir();
            }
        });
    }
    
    public static Block getGlassBlock(DyeColor color) {
        return BuiltInRegistries.BLOCK.get(new ResourceLocation("minecraft:" + color.getName() + "_stained_glass"));
    }
    
    public static boolean isValidScale(int size) {
        return Arrays.stream(supportedScales).anyMatch(s -> s == size);
    }

    /**
     * Kills portal entities identified by the given UUID list and clears the list.
     * Entities in unloaded chunks are silently skipped — the portal's own generation
     * tick will discard them once the chunk loads again.
     *
     * <p>The list is always cleared, even when {@code world} is {@code null} or entities
     * cannot be reached, so that the entry's UUID records stay consistent with intended state.</p>
     */
    public static void killPortalsByIds(List<UUID> ids, @Nullable ServerLevel world) {
        if (!ids.isEmpty()) {
            if (world != null) {
                for (UUID id : ids) {
                    Entity e = world.getEntity(id);
                    if (e != null) {
                        e.discard();
                    }
                }
            }
            ids.clear();
        }
    }

    /**
     * Immediately discards all {@link MiniScaledPortal} entities for the given
     * box whose generation is strictly less than {@code newGeneration}.
     * Called proactively so portals disappear at once rather than waiting up to
     * two ticks for the per-portal generation check in {@link MiniScaledPortal#tick}.
     *
     * @param oldDim  the dimension the previous entrance was in (may be null if never placed)
     * @param oldPos  the base-pos of the previous entrance (used to bound the search)
     * @param entry   the current record entry (used to locate the void-world inner portals)
     */
    public static void killStalePortals(
        int boxId, int newGeneration,
        @Nullable ResourceKey<Level> oldDim,
        @Nullable BlockPos oldPos,
        ScaleBoxRecord.Entry entry
    ) {
        // Kill outer portals in the old entrance dimension.
        if (oldDim != null && oldPos != null) {
            ServerLevel oldWorld = McHelper.getServerWorld(oldDim);
            if (oldWorld != null) {
                AABB searchBox = new AABB(oldPos).inflate(32);
                oldWorld.getEntitiesOfClass(MiniScaledPortal.class, searchBox)
                    .stream()
                    .filter(p -> p.boxId == boxId && p.generation < newGeneration)
                    .forEach(Entity::discard);
            }
        }

        // Kill inner portals in the void world.
        ServerLevel voidWorld = VoidDimension.getVoidServerWorld();
        if (voidWorld != null && entry.innerBoxPos != null) {
            AABB innerBB = entry.getInnerAreaBox().toRealNumberBox();
            voidWorld.getEntitiesOfClass(MiniScaledPortal.class, innerBB.inflate(2))
                .stream()
                .filter(p -> p.boxId == boxId && p.generation < newGeneration)
                .forEach(Entity::discard);
        }
    }
    
    /**
     * Emergency reset: kills all portals for this entry and re-creates them.
     * Used by the "Fix portals" GUI button.
     *
     * <p>We verify that the placeholder blocks actually exist before treating
     * the box as "placed". {@link ScaleBoxPlaceholderBlockEntity#checkShouldRemovePortals}
     * is a one-tick-deferred task, so {@code entry.currentEntranceDim} may still
     * be non-null even when the blocks are already gone. Creating portals at an
     * air position would leave broken floating portals and re-place placeholder
     * blocks where none should be.</p>
     */
    public static void resetPortalsForEntry(ScaleBoxRecord.Entry entry) {
        ServerLevel voidWorld = VoidDimension.getVoidServerWorld();

        // Kill all registered portals by UUID before re-creating.
        ServerLevel outerWorld = entry.currentEntranceDim != null
            ? McHelper.getServerWorld(entry.currentEntranceDim) : null;
        killPortalsByIds(entry.outerPortalIds, outerWorld);
        killPortalsByIds(entry.innerPortalIds, voidWorld);

        entry.generation++;
        ScaleBoxRecord.get().setDirty(true);

        // Verify block state to decide whether the box is actually placed.
        boolean isActuallyPlaced = false;
        if (outerWorld != null && entry.currentEntrancePos != null) {
            final ServerLevel outerWorldFinal = outerWorld;
            isActuallyPlaced = entry.getOuterAreaBox().stream().allMatch(
                blockPos -> outerWorldFinal.getBlockState(blockPos).getBlock()
                    == ScaleBoxPlaceholderBlock.instance
            );
        }

        // Re-initialise the inner box (barrier blocks, chunk loading, glass frame)
        initializeInnerBoxBlocks(entry.currentEntranceSize, entry);

        // Re-create portals based on verified block state.
        // createScaleBoxPortals / createInnerPortalsPointingToVoidUnderneath
        // will populate the UUID lists and call setDirty.
        if (isActuallyPlaced && voidWorld != null) {
            createScaleBoxPortals(voidWorld, outerWorld, entry);
        } else if (voidWorld != null) {
            createInnerPortalsPointingToVoidUnderneath(entry);
        }
    }

    /**
     * Periodic safety sweep called from the server tick (every ~100 ticks).
     *
     * <p>For every {@link ScaleBoxRecord.Entry} this method:</p>
     * <ol>
     *   <li>Verifies the entrance blocks still exist when the chunk is loaded.
     *       If they are gone (or the dimension has disappeared), it drives the same
     *       cleanup that {@link qouteall.mini_scaled.block.ScaleBoxPlaceholderBlockEntity#checkShouldRemovePortals}
     *       would perform.</li>
     *   <li>Discards any {@link MiniScaledPortal} entities whose {@code generation}
     *       doesn't match the entry's current generation — these are orphaned portals
     *       left behind by a previous placement that wasn't cleaned up in time.</li>
     * </ol>
     *
     * <p>This method intentionally does <em>not</em> create portals; portal creation
     * is performed by the explicit placement flow ({@link #putScaleBoxIntoWorld} and
     * {@link #createInnerPortalsPointingToVoidUnderneath}).  The reconciler is a pure
     * defensive sweep: it only removes what shouldn't be there.</p>
     */
    /**
     * Periodic safety sweep called every ~100 ticks from the server tick event.
     *
     * <p>For each entry this does two things:</p>
     * <ol>
     *   <li><b>State verification</b> — if the entrance dimension is gone or its blocks are
     *       absent (chunk loaded), drives the full cleanup sequence.</li>
     *   <li><b>UUID-based orphan sweep</b> — discards any {@link MiniScaledPortal} found in
     *       the expected spatial region whose UUID is not in the entry's registered lists.
     *       Skipped for entries with empty UUID lists (pre-UUID-tracking worlds) to preserve
     *       backward compatibility — those fall back to the per-portal generation tick.</li>
     * </ol>
     */
    public static void reconcilePortals() {
        ScaleBoxRecord record = ScaleBoxRecord.get();
        ServerLevel voidWorld = VoidDimension.getVoidServerWorld();

        for (ScaleBoxRecord.Entry entry : record.getAllEntries()) {
            // --- Step 1: verify placement state matches reality ---
            if (entry.currentEntranceDim != null && entry.currentEntrancePos != null) {
                ServerLevel outerWorld = McHelper.getServerWorld(entry.currentEntranceDim);
                if (outerWorld == null) {
                    LOGGER.warn(
                        "reconcilePortals: scale box {} entrance dim {} is gone — clearing entrance",
                        entry.id, entry.currentEntranceDim
                    );
                    // Outer world is gone; can't reach those portals — just clear the list.
                    entry.outerPortalIds.clear();
                    entry.currentEntranceDim = null;
                    entry.generation++;
                    record.setDirty(true);
                    if (voidWorld != null) {
                        createInnerPortalsPointingToVoidUnderneath(entry);
                    }
                    continue;
                }

                // Only inspect blocks when the chunk is already loaded — avoid forcing a load.
                if (outerWorld.hasChunkAt(entry.currentEntrancePos)) {
                    boolean blocksValid = entry.getOuterAreaBox().stream().allMatch(
                        pos -> outerWorld.getBlockState(pos).getBlock()
                            == ScaleBoxPlaceholderBlock.instance
                    );
                    if (!blocksValid) {
                        LOGGER.warn(
                            "reconcilePortals: scale box {} entrance blocks missing — clearing entrance",
                            entry.id
                        );
                        killPortalsByIds(entry.outerPortalIds, outerWorld);
                        entry.currentEntranceDim = null;
                        entry.generation++;
                        record.setDirty(true);
                        if (voidWorld != null) {
                            createInnerPortalsPointingToVoidUnderneath(entry);
                        }
                        continue;
                    }
                }
            }

            // --- Step 2: UUID-based orphan sweep ---
            // Only run if we have a complete UUID record for this entry.
            // Entries from before UUID tracking have empty lists; for those,
            // the per-portal generation tick provides the safety net.
            if (!entry.outerPortalIds.isEmpty() && entry.currentEntranceDim != null) {
                ServerLevel outerWorld = McHelper.getServerWorld(entry.currentEntranceDim);
                if (outerWorld != null) {
                    Set<UUID> outerIdSet = new java.util.HashSet<>(entry.outerPortalIds);
                    AABB outerBB = entry.getOuterAreaBox().toRealNumberBox().inflate(4);
                    outerWorld.getEntitiesOfClass(MiniScaledPortal.class, outerBB)
                        .stream()
                        .filter(p -> p.boxId == entry.id && !outerIdSet.contains(p.getUUID()))
                        .forEach(Entity::discard);
                }
            }

            if (!entry.innerPortalIds.isEmpty() && voidWorld != null && entry.innerBoxPos != null) {
                Set<UUID> innerIdSet = new java.util.HashSet<>(entry.innerPortalIds);
                AABB innerBB = entry.getInnerAreaBox().toRealNumberBox().inflate(4);
                voidWorld.getEntitiesOfClass(MiniScaledPortal.class, innerBB)
                    .stream()
                    .filter(p -> p.boxId == entry.id && !innerIdSet.contains(p.getUUID()))
                    .forEach(Entity::discard);
            }
        }
    }

    // will set dirty
    public static void updateScaleBoxPortals(
        ScaleBoxRecord.Entry entry,
        ServerPlayer player
    ) {
        ResourceKey<Level> currentEntranceDim = entry.currentEntranceDim;
        if (currentEntranceDim == null) {
            LOGGER.error("Updating a scale box that has no entrance");
            return;
        }
        putScaleBoxIntoWorld(
            entry,
            McHelper.getServerWorld(currentEntranceDim),
            entry.currentEntrancePos,
            entry.getEntranceRotation(),
            player
        );
    }
    
    public static void createInnerPortalsPointingToVoidUnderneath(
        ScaleBoxRecord.Entry entry
    ) {
        ServerLevel voidWorld = VoidDimension.getVoidServerWorld();
        // Kill any previously-registered inner portals before creating new ones.
        killPortalsByIds(entry.innerPortalIds, voidWorld);

        AABB innerAreaBox = entry.getInnerAreaBox().toRealNumberBox();
        Vec3 innerAreaBoxSize = Helper.getBoxSize(innerAreaBox);
        int boxId = entry.id;
        int generation = entry.generation;
        for (Direction innerDirection : Direction.values()) {
            MiniScaledPortal portal = MiniScaledPortal.entityType.create(voidWorld);
            Validate.notNull(portal);
            
            portal.setOriginPos(Helper.getBoxSurface(innerAreaBox, innerDirection).getCenter());
            portal.setDestination(portal.getOriginPos().add(0, -1000, 0));
            portal.setDestinationDimension(voidWorld.dimension());
            
            Tuple<Direction, Direction> perpendicularDirections =
                Helper.getPerpendicularDirections(innerDirection.getOpposite());
            Direction pd1 = perpendicularDirections.getA();
            Direction pd2 = perpendicularDirections.getB();
            
            portal.setOrientation(Vec3.atLowerCornerOf(pd1.getNormal()), Vec3.atLowerCornerOf(pd2.getNormal()));
            portal.setWidth(Helper.getCoordinate(innerAreaBoxSize, pd1.getAxis()));
            portal.setHeight(Helper.getCoordinate(innerAreaBoxSize, pd2.getAxis()));
            
            portal.renderingMergable = true;
            portal.portalTag = "mini_scaled:scaled_box_inner_wrapping";
            portal.boxId = boxId;
            portal.generation = generation;
            
            McHelper.spawnServerEntity(portal);
            entry.innerPortalIds.add(portal.getUUID());
        }
        ScaleBoxRecord.get().setDirty(true);
    }
}
