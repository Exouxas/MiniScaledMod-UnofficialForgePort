package qouteall.mini_scaled.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.chunk_loading.ChunkLoader;
import qouteall.imm_ptl.core.chunk_loading.DimensionalChunkPos;
import qouteall.imm_ptl.core.commands.PortalCommand;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.teleportation.ServerTeleportationManager;
import qouteall.mini_scaled.MiniScaledPortal;
import qouteall.mini_scaled.MiniScaledRegistries;
import qouteall.mini_scaled.ScaleBoxEntranceCreation;
import qouteall.mini_scaled.ScaleBoxRecord;
import qouteall.mini_scaled.VoidDimension;
import qouteall.mini_scaled.block.ScaleBoxPlaceholderBlock;
import qouteall.mini_scaled.block.ScaleBoxPlaceholderBlockEntity;
import qouteall.mini_scaled.util.MSUtil;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.my_util.AARotation;
import qouteall.q_misc_util.my_util.IntBox;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-process Minecraft integration tests run by `./gradlew runGameTestServer`.
 *
 * Forge boots a headless dedicated server, loads all mods, runs every @GameTest method,
 * prints pass/fail, and exits — analogous to Selenium/Playwright for a website.
 *
 * Structure template ("mini_scaled:empty") is a 3×3×3 all-air zone generated at
 * build time by the `generateTestStructures` Gradle task.
 */
@GameTestHolder("mini_scaled")
public class MiniScaledGameTests {

    // Forge prepends the lowercase class name, so the resolved path is
    // "miniscaledgametests.empty" → data/mini_scaled/structures/miniscaledgametests.empty.nbt
    private static final String EMPTY = "empty";

    // 9×9×9 all-air zone used for tests that need to place a full glass frame.
    private static final String FRAME9 = "frame9";

    // -------------------------------------------------------------------------
    // Registry checks
    // -------------------------------------------------------------------------

    /**
     * All DeferredRegister objects must resolve to non-null after FMLCommonSetupEvent.
     */
    @GameTest(template = EMPTY)
    public static void registrationsComplete(GameTestHelper helper) {
        if (MiniScaledRegistries.SCALE_BOX_PLACEHOLDER_BLOCK.get() == null) {
            helper.fail("ScaleBoxPlaceholderBlock not registered");
            return;
        }
        if (MiniScaledRegistries.BARRIER_BLOCK.get() == null) {
            helper.fail("BoxBarrierBlock not registered");
            return;
        }
        if (MiniScaledRegistries.SCALE_BOX_ENTRANCE_ITEM.get() == null) {
            helper.fail("ScaleBoxEntranceItem not registered");
            return;
        }
        if (MiniScaledRegistries.MANIPULATION_WAND_ITEM.get() == null) {
            helper.fail("ManipulationWandItem not registered");
            return;
        }
        if (MiniScaledRegistries.getPortalEntityType() == null) {
            helper.fail("MiniScaledPortal entity type not registered");
            return;
        }
        helper.succeed();
    }

    // -------------------------------------------------------------------------
    // Block behaviour
    // -------------------------------------------------------------------------

    /**
     * ScaleBoxPlaceholderBlock must be placeable and must create a
     * ScaleBoxPlaceholderBlockEntity.
     */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void placeholderBlockCreatesBlockEntity(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ScaleBoxPlaceholderBlock.instance);
        helper.assertBlock(
            pos,
            b -> b == ScaleBoxPlaceholderBlock.instance,
            "ScaleBoxPlaceholderBlock should be present after placement"
        );
        BlockEntity be = helper.getBlockEntity(pos);
        if (!(be instanceof ScaleBoxPlaceholderBlockEntity)) {
            helper.fail("Expected ScaleBoxPlaceholderBlockEntity at " + pos + ", got: " + be);
            return;
        }
        helper.succeed();
    }

    // -------------------------------------------------------------------------
    // Void dimension
    // -------------------------------------------------------------------------

    /**
     * After the server starts, VoidDimension.initializeVoidDimension() must have
     * registered the mini_scaled:void level. This is the most important integration
     * check for the mod's core mechanic.
     *
     * Marked required=false because DimensionAPI is a no-op in the compile-only
     * ImmPTL stubs; this test only passes when built against the real ImmPTL library.
     */
    @GameTest(template = EMPTY, timeoutTicks = 40, required = false)
    public static void voidDimensionIsRegistered(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        boolean exists = server.levelKeys().contains(VoidDimension.dimensionId);
        if (!exists) {
            helper.fail("Void dimension mini_scaled:void was not registered. " +
                "Check ServerDimensionsLoadEvent / DimensionAPI wiring.");
            return;
        }
        helper.succeed();
    }

    // -------------------------------------------------------------------------
    // ScaleBoxRecord NBT round-trip
    // -------------------------------------------------------------------------

    /**
     * ScaleBoxRecord.Entry must survive a full toTag → fromTag round-trip with no
     * data loss. This guards the save/load path.
     */
    @GameTest(template = EMPTY)
    public static void scaleBoxRecordNbtRoundTrip(GameTestHelper helper) {
        ScaleBoxRecord.Entry original = new ScaleBoxRecord.Entry();
        original.id             = 99;
        original.scale          = 16;
        original.color          = DyeColor.CYAN;
        original.ownerId        = UUID.fromString("aaaabbbb-cccc-dddd-eeee-ffffffffffff");
        original.ownerNameCache = "TestPlayer";
        original.generation     = 5;
        original.innerBoxPos    = new BlockPos(1024, 64, 512);
        original.currentEntrancePos  = new BlockPos(10, 65, 20);
        original.currentEntranceSize = new BlockPos(1, 1, 1);
        original.entranceRotation    = AARotation.IDENTITY;
        original.teleportChangesScale   = true;
        original.teleportChangesGravity = false;
        original.accessControl          = true;

        net.minecraft.nbt.CompoundTag saved = original.toTag();
        ScaleBoxRecord.Entry loaded = ScaleBoxRecord.Entry.fromTag(saved);

        if (loaded.id != 99)    { helper.fail("id mismatch: " + loaded.id);      return; }
        if (loaded.scale != 16) { helper.fail("scale mismatch: " + loaded.scale); return; }
        if (loaded.color != DyeColor.CYAN) { helper.fail("color mismatch: " + loaded.color); return; }
        if (!loaded.ownerId.equals(original.ownerId)) { helper.fail("ownerId mismatch"); return; }
        if (loaded.generation != 5)          { helper.fail("generation mismatch"); return; }
        if (!loaded.innerBoxPos.equals(original.innerBoxPos)) { helper.fail("innerBoxPos mismatch"); return; }
        if (!loaded.teleportChangesScale)    { helper.fail("teleportChangesScale should be true"); return; }
        if (loaded.teleportChangesGravity)   { helper.fail("teleportChangesGravity should be false"); return; }
        if (!loaded.accessControl)           { helper.fail("accessControl should be true"); return; }

        helper.succeed();
    }

    // -------------------------------------------------------------------------
    // Scale box entrance item
    // -------------------------------------------------------------------------

    /**
     * The creative inventory helper must supply at least one ScaleBoxEntranceItem
     * per supported scale (4 scales × 16 dye colours = 64 expected stacks).
     */
    @GameTest(template = EMPTY)
    public static void scaleBoxEntranceItemCoversAllVariants(GameTestHelper helper) {
        java.util.List<net.minecraft.world.item.ItemStack> stacks = new java.util.ArrayList<>();
        qouteall.mini_scaled.item.ScaleBoxEntranceItem.registerCreativeInventory(stacks::add);

        int expected = 4 * 16; // 4 scales × 16 dye colours
        if (stacks.size() < expected) {
            helper.fail("Expected at least " + expected + " ScaleBoxEntranceItem stacks, got " + stacks.size());
            return;
        }
        // Every stack must hold a ScaleBoxEntranceItem
        for (net.minecraft.world.item.ItemStack stack : stacks) {
            if (!(stack.getItem() instanceof qouteall.mini_scaled.item.ScaleBoxEntranceItem)) {
                helper.fail("Non-entrance item in creative stacks: " + stack);
                return;
            }
        }
        helper.succeed();
    }

    // -------------------------------------------------------------------------
    // Entity type sanity
    // -------------------------------------------------------------------------

    /**
     * MiniScaledPortal.entityType must match the registered entity type so that
     * portal spawning uses the correct type.
     */
    @GameTest(template = EMPTY)
    public static void portalEntityTypeAssigned(GameTestHelper helper) {
        if (MiniScaledPortal.entityType == null) {
            helper.fail("MiniScaledPortal.entityType is null — was FMLCommonSetupEvent.enqueueWork() called?");
            return;
        }
        if (MiniScaledPortal.entityType != MiniScaledRegistries.getPortalEntityType()) {
            helper.fail("MiniScaledPortal.entityType does not match the registered EntityType");
            return;
        }
        helper.succeed();
    }

    // -------------------------------------------------------------------------
    // Frame creation — interior clearing
    // -------------------------------------------------------------------------

    /**
     * When a player converts a glass frame to a scale box item (by right-clicking with
     * the creation item), the blocks INSIDE the frame must be cleared to air.
     *
     * <p>Test procedure:
     * <ol>
     *   <li>Build a 4×4×4 pink stained glass frame (12 edges only) at relative (2,2,2)–(5,5,5).
     *   <li>Place an oak log at the interior position (3,3,3).
     *   <li>Simulate right-click with a netherite ingot via a FakePlayer.
     *   <li>Assert the interior position is now air.
     * </ol>
     *
     * <p><b>This test FAILS before the fix</b> because
     * {@code ScaleBoxEntranceCreation.onRightClickBoxFrameUsingNetherite} only removes the
     * 12 edge strips and never clears the interior blocks.
     *
     * <p>Note: the {@code MiniScaledPortal.level()} crash (NoSuchMethodError in Sinytra
     * Connector) cannot be reproduced in a standard Forge GameTest — it requires the
     * Fabric-to-Forge bridge that replaces Entity.level() method dispatch. The fix
     * (replacing every {@code level()} call with {@code getOriginWorld()}) is applied
     * separately and verified by manual in-game testing.
     */
    @GameTest(template = FRAME9, timeoutTicks = 40)
    public static void frameCreationClearsInteriorBlocks(GameTestHelper helper) {
        // Ensure creationItem is set (normally done by onServerStarted via config).
        ScaleBoxEntranceCreation.creationItem = Items.NETHERITE_INGOT;

        ServerLevel serverLevel = helper.getLevel();

        // Build a 4×4×4 pink stained glass frame (only the 12 edges) at relative (2,2,2).
        BlockPos frameBase = helper.absolutePos(new BlockPos(2, 2, 2));
        IntBox outerBox = IntBox.fromBasePointAndSize(frameBase, new BlockPos(4, 4, 4));
        for (IntBox edge : outerBox.get12Edges()) {
            edge.fastStream().forEach(p ->
                serverLevel.setBlockAndUpdate(p, Blocks.PINK_STAINED_GLASS.defaultBlockState())
            );
        }

        // Place a non-air block inside the frame (one block into the 2×2×2 interior).
        BlockPos interiorPos = frameBase.offset(1, 1, 1);
        serverLevel.setBlockAndUpdate(interiorPos, Blocks.OAK_LOG.defaultBlockState());

        // Create a fake player adjacent to the frame, holding a netherite ingot.
        GameProfile profile = new GameProfile(UUID.randomUUID(), "test_frame_player");
        var fakePlayer = FakePlayerFactory.get(serverLevel, profile);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_INGOT));
        fakePlayer.setPos(frameBase.getX() - 1.5, frameBase.getY() + 1.0, frameBase.getZ() + 1.5);

        // Simulate right-click on the bottom-corner glass block of the frame.
        Vec3 clickVec = Vec3.atCenterOf(frameBase);
        BlockHitResult hitResult = new BlockHitResult(clickVec, Direction.UP, frameBase, false);

        InteractionResult result = ScaleBoxEntranceCreation.onRightClickBlock(
            fakePlayer, serverLevel, InteractionHand.MAIN_HAND, hitResult
        );

        if (result != InteractionResult.CONSUME) {
            helper.fail("Frame creation should have succeeded (CONSUME) but returned: " + result +
                ". Is the frame complete and the creation item set?");
            return;
        }

        // Interior block must be air after the fix is applied.
        // Before the fix this assertion fails because the oak log is left untouched.
        BlockState interiorState = serverLevel.getBlockState(interiorPos);
        if (!interiorState.isAir()) {
            helper.fail(
                "Interior blocks were NOT cleared after frame creation. " +
                "Expected air at " + interiorPos + " but found: " + interiorState.getBlock() +
                ". Fix: add IntBox.getAdjusted(1,1,1,-1,-1,-1) clearing in " +
                "ScaleBoxEntranceCreation.onRightClickBoxFrameUsingNetherite."
            );
            return;
        }

        helper.succeed();
    }

    // -------------------------------------------------------------------------
    // MSUtil.getGravityVec — Sinytra Connector compatibility
    // -------------------------------------------------------------------------

    /**
     * {@code MSUtil.getGravityVec} must never throw when called with a live entity.
     *
     * <p>In Sinytra Connector environments, {@code GravityChangerInterface$Invoker}
     * is transformed from an interface into a class, causing an
     * {@code IncompatibleClassChangeError} (a {@link LinkageError}) on the first
     * {@code invokeinterface} bytecall. The fix adds a try-catch with a static
     * broken-flag so subsequent calls short-circuit to the {@code DOWN} fallback.
     *
     * <p>In the standard Forge GameTest environment (no Sinytra Connector) the
     * stub implementation correctly defines {@code Invoker} as an interface and
     * returns {@link Direction#DOWN}, so this test exercises the happy path and
     * confirms the return value is a valid unit vector. The broken-flag path is
     * exercised by {@link #getGravityVecFallbackWhenBroken}.
     */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void getGravityVecDoesNotThrow(GameTestHelper helper) {
        // Reset the broken flag so this test always checks the live path.
        MSUtil.gravityInterfaceBroken = false;

        GameProfile profile = new GameProfile(UUID.randomUUID(), "gravity_test_player");
        var fakePlayer = FakePlayerFactory.get(helper.getLevel(), profile);

        Vec3 gravity;
        try {
            gravity = MSUtil.getGravityVec(fakePlayer);
        } catch (Throwable t) {
            helper.fail("MSUtil.getGravityVec threw unexpectedly: " + t);
            return;
        }

        if (gravity == null) {
            helper.fail("MSUtil.getGravityVec returned null");
            return;
        }
        double len = gravity.length();
        if (Math.abs(len - 1.0) > 0.001) {
            helper.fail("Expected a unit vector from getGravityVec, got length " + len + ": " + gravity);
            return;
        }

        helper.succeed();
    }

    /**
     * When the {@code gravityInterfaceBroken} flag is set (simulating the
     * Sinytra Connector {@code IncompatibleClassChangeError} scenario), {@code getGravityVec}
     * must return the {@link Direction#DOWN} fallback vector {@code (0, -1, 0)}
     * without attempting to call {@code GravityChangerInterface.invoker}.
     */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void getGravityVecFallbackWhenBroken(GameTestHelper helper) {
        boolean savedFlag = MSUtil.gravityInterfaceBroken;
        MSUtil.gravityInterfaceBroken = true;
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "gravity_broken_player");
            var fakePlayer = FakePlayerFactory.get(helper.getLevel(), profile);

            Vec3 gravity = MSUtil.getGravityVec(fakePlayer);

            if (gravity == null) {
                helper.fail("getGravityVec returned null when gravityInterfaceBroken=true");
                return;
            }
            if (gravity.x != 0 || gravity.y != -1 || gravity.z != 0) {
                helper.fail("Expected fallback (0,-1,0) but got: " + gravity);
                return;
            }
        } finally {
            MSUtil.gravityInterfaceBroken = savedFlag;
        }

        helper.succeed();
    }

    // -------------------------------------------------------------------------
    // IPGlobal.clientTaskList — MyTaskList API contract
    // -------------------------------------------------------------------------

    /**
     * {@code IPGlobal.clientTaskList.addTask(MyTask)} must accept a
     * {@link MyTaskList.MyTask} (the real ImmPTL inner interface), NOT a
     * plain {@code BooleanSupplier}.
     *
     * <p><b>This test would have caught the crash before the fix:</b> the original
     * stub defined {@code addTask(BooleanSupplier)}, which compiled fine but threw
     * {@code NoSuchMethodError} at runtime against ImmPTL 3.0.7 because the real
     * method signature is {@code addTask(MyTaskList.MyTask)}.</p>
     *
     * <p>The test calls both {@code addTask(MyTask)} and
     * {@code addOneShotTask(Runnable)} — the two overloads our code uses — and
     * verifies they complete without throwing. If the stub (and therefore our
     * compiled bytecode) ever drifts away from the real ImmPTL API again, this
     * test will fail to compile, surfacing the mismatch at build time instead of
     * at runtime inside the player\'s modpack.</p>
     */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void clientTaskListAcceptsMyTask(GameTestHelper helper) {
        // addTask(MyTask) — this is the signature used by the real ImmPTL jar.
        // If the stub were still addTask(BooleanSupplier) this would not compile.
        boolean[] ran = {false};
        MyTaskList.MyTask task = () -> {
            ran[0] = true;
            return true; // one-shot: finished after first call
        };

        try {
            IPGlobal.clientTaskList.addTask(task);
        } catch (Throwable t) {
            helper.fail("clientTaskList.addTask(MyTask) threw: " + t);
            return;
        }

        // addOneShotTask(Runnable) — the simpler overload used in MiniScaledPortal.
        boolean[] ran2 = {false};
        try {
            IPGlobal.clientTaskList.addOneShotTask(() -> ran2[0] = true);
        } catch (Throwable t) {
            helper.fail("clientTaskList.addOneShotTask(Runnable) threw: " + t);
            return;
        }

        helper.succeed();
    }

    // -------------------------------------------------------------------------
    // ImmPTL linkage verification
    // -------------------------------------------------------------------------

    /**
     * Verifies via reflection that every ImmPTL / q_misc_util method and field
     * that our code actually calls exists in the runtime jar with the exact
     * signature our stubs declare.
     *
     * <p>This is the automated equivalent of the manual in-game crashes we have
     * been fixing one by one (NoSuchMethodError, IncompatibleClassChangeError, …).
     * If the test passes here but a crash still occurs in-game, the signature
     * mismatch is in a class that we haven't stubbed yet — add it to this list.</p>
     *
     * <p>All failures are collected and reported together so you get the full
     * list in one run instead of one crash at a time.</p>
     */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void immPtlLinkageCheck(GameTestHelper helper) {
        List<String> failures = new ArrayList<>();

        // Utility: resolve a class, recording failure if not found.
        // (Class.forName uses the runtime jar, not our compile-time stubs.)

        // --- Portal fields ---
        checkField(failures, Portal.class, double.class,  "scaling");
        checkField(failures, Portal.class, boolean.class, "teleportChangesScale");
        checkField(failures, Portal.class, boolean.class, "fuseView");
        checkField(failures, Portal.class, boolean.class, "renderingMergable");
        checkField(failures, Portal.class, boolean.class, "hasCrossPortalCollision");
        checkField(failures, Portal.class, boolean.class, "doRenderPlayer");
        checkField(failures, Portal.class, String.class,  "portalTag");

        // --- Portal methods ---
        checkMethod(failures, Portal.class, "getOriginWorld");
        checkMethod(failures, Portal.class, "getOriginPos");
        checkMethod(failures, Portal.class, "setOriginPos",          Vec3.class);
        checkMethod(failures, Portal.class, "setDestination",        Vec3.class);
        checkMethod(failures, Portal.class, "setDestinationDimension", net.minecraft.resources.ResourceKey.class);
        checkMethod(failures, Portal.class, "setOrientation",        Vec3.class, Vec3.class);
        checkMethod(failures, Portal.class, "setWidth",              double.class);
        checkMethod(failures, Portal.class, "setHeight",             double.class);
        checkMethod(failures, Portal.class, "setInteractable",       boolean.class);
        checkMethod(failures, Portal.class, "setTeleportChangesGravity", boolean.class);
        checkMethod(failures, Portal.class, "getScale");
        checkMethod(failures, Portal.class, "getNormal");
        checkMethod(failures, Portal.class, "allowOverlappedTeleport");
        checkMethod(failures, Portal.class, "onCollidingWithEntity", Entity.class);
        checkMethod(failures, Portal.class, "canTeleportEntity",     Entity.class);
        checkMethod(failures, Portal.class, "getDistanceToNearestPointInPortal", Vec3.class);
        checkMethod(failures, Portal.class, "transformVelocityRelativeToPortal", Vec3.class, Entity.class);

        // --- PortalExtension ---
        checkField(failures, PortalExtension.class, boolean.class, "adjustPositionAfterTeleport");
        checkMethod(failures, PortalExtension.class, "get", Portal.class);

        // --- PortalManipulation ---
        checkMethod(failures, PortalManipulation.class, "createReversePortal", Portal.class, EntityType.class);

        // --- McHelper ---
        checkMethod(failures, McHelper.class, "getServerWorld",           net.minecraft.resources.ResourceKey.class);
        checkMethod(failures, McHelper.class, "getOverWorldOnServer");
        checkMethod(failures, McHelper.class, "spawnServerEntity",        Entity.class);
        checkMethod(failures, McHelper.class, "updateBoundingBox",        Entity.class);
        checkMethod(failures, McHelper.class, "getDimensionName",         net.minecraft.resources.ResourceKey.class);
        checkMethod(failures, McHelper.class, "getRenderDistanceOnServer");

        // --- IPGlobal fields (static, accessed directly) ---
        checkField(failures, IPGlobal.class, MyTaskList.class, "clientTaskList");
        checkField(failures, IPGlobal.class, MyTaskList.class, "serverTaskList");
        checkField(failures, IPGlobal.class, boolean.class,    "enableDepthClampForPortalRendering");

        // --- MyTaskList ---
        checkInnerInterface(failures, "qouteall.q_misc_util.my_util.MyTaskList$MyTask", "runAndGetIsFinished");
        checkMethod(failures, MyTaskList.class, "addTask",         MyTaskList.MyTask.class);
        checkMethod(failures, MyTaskList.class, "addOneShotTask",  Runnable.class);
        checkMethod(failures, MyTaskList.class, "oneShotTask",     Runnable.class);  // static factory

        // --- PortalAPI ---
        checkMethod(failures, PortalAPI.class, "addChunkLoaderForPlayer",
            net.minecraft.server.level.ServerPlayer.class, ChunkLoader.class);
        checkMethod(failures, PortalAPI.class, "removeChunkLoaderForPlayer",
            net.minecraft.server.level.ServerPlayer.class, ChunkLoader.class);

        // --- ServerTeleportationManager ---
        checkMethod(failures, ServerTeleportationManager.class, "teleportEntityGeneral",
            Entity.class, Vec3.class, ServerLevel.class);

        // --- PortalCommand ---
        checkMethod(failures, PortalCommand.class, "raytracePortals",
            Level.class, Vec3.class, Vec3.class, boolean.class);

        // --- McRemoteProcedureCall ---
        checkMethod(failures, McRemoteProcedureCall.class, "tellServerToInvoke", String.class, Object[].class);
        checkMethod(failures, McRemoteProcedureCall.class, "tellClientToInvoke",
            net.minecraft.server.level.ServerPlayer.class, String.class, Object[].class);

        // --- MiscHelper ---
        checkMethod(failures, MiscHelper.class, "getServer");

        // --- Helper ---
        checkMethod(failures, Helper.class, "log",                       Object.class);
        checkMethod(failures, Helper.class, "getAnotherFourDirections",  Direction.Axis.class);
        checkMethod(failures, Helper.class, "getPerpendicularDirections", Direction.class);
        checkMethod(failures, Helper.class, "getBoxSurface",             AABB.class, Direction.class);
        checkMethod(failures, Helper.class, "getBoxSize",                AABB.class);
        checkMethod(failures, Helper.class, "getCoordinate",             net.minecraft.core.Vec3i.class, Direction.Axis.class);
        checkMethod(failures, Helper.class, "putCoordinate",             net.minecraft.core.Vec3i.class, Direction.Axis.class, int.class);
        checkMethod(failures, Helper.class, "getVec3i",                  net.minecraft.nbt.CompoundTag.class, String.class);
        checkMethod(failures, Helper.class, "putVec3i",                  net.minecraft.nbt.CompoundTag.class, String.class, net.minecraft.core.Vec3i.class);
        checkMethod(failures, Helper.class, "secondToNano",              double.class);

        // --- GravityChangerInterface: must still be an interface (not a class) ---
        try {
            Class<?> iface = Class.forName("qouteall.imm_ptl.core.compat.GravityChangerInterface");
            if (!iface.isInterface()) {
                failures.add("GravityChangerInterface is not an interface in this runtime — " +
                    "Sinytra Connector may have transformed it into a class");
            }
            // The Invoker nested type also must be an interface
            try {
                Class<?> invoker = Class.forName("qouteall.imm_ptl.core.compat.GravityChangerInterface$Invoker");
                if (!invoker.isInterface()) {
                    failures.add("GravityChangerInterface$Invoker is not an interface — " +
                        "getGravityVec() would throw IncompatibleClassChangeError (covered by runtime fallback in MSUtil)");
                }
            } catch (ClassNotFoundException e) {
                failures.add("GravityChangerInterface$Invoker class not found: " + e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            failures.add("GravityChangerInterface class not found: " + e.getMessage());
        }

        if (!failures.isEmpty()) {
            helper.fail(failures.size() + " ImmPTL linkage problem(s) detected:\n  - " +
                String.join("\n  - ", failures));
        } else {
            helper.succeed();
        }
    }

    // -------------------------------------------------------------------------
    // Reflection helpers used by immPtlLinkageCheck
    // -------------------------------------------------------------------------

    /**
     * Checks that {@code owner} declares (or inherits) a method named {@code name}
     * with the given parameter types. Records a human-readable failure message if not.
     */
    private static void checkMethod(List<String> failures, Class<?> owner, String name, Class<?>... params) {
        try {
            // getDeclaredMethod only checks the exact class; getMethod also walks supers/interfaces.
            // We use getMethod so that inherited methods (e.g. from Entity) are accepted.
            owner.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            StringBuilder sb = new StringBuilder();
            sb.append(owner.getSimpleName()).append('.').append(name).append('(');
            for (int i = 0; i < params.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(params[i].getSimpleName());
            }
            sb.append(") — method not found in runtime jar");
            failures.add(sb.toString());
        }
    }

    /**
     * Checks that {@code owner} declares a field named {@code name} with the given type.
     */
    private static void checkField(List<String> failures, Class<?> owner, Class<?> type, String name) {
        try {
            Field f = findField(owner, name);
            if (!f.getType().equals(type)) {
                failures.add(owner.getSimpleName() + '.' + name +
                    " — wrong type: expected " + type.getSimpleName() +
                    " but runtime has " + f.getType().getSimpleName());
            }
        } catch (NoSuchFieldException e) {
            failures.add(owner.getSimpleName() + '.' + name + " — field not found in runtime jar");
        }
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    /**
     * Checks that an inner interface (specified by its binary name) exists and is
     * actually an interface, and that it declares the given method name.
     */
    private static void checkInnerInterface(List<String> failures, String binaryName, String methodName) {
        try {
            Class<?> cls = Class.forName(binaryName);
            if (!cls.isInterface()) {
                failures.add(binaryName + " — should be an interface but is a class");
            }
            // just check any method with that name exists
            boolean found = false;
            for (Method m : cls.getMethods()) {
                if (m.getName().equals(methodName)) { found = true; break; }
            }
            if (!found) {
                failures.add(binaryName + '.' + methodName + "() — method not found");
            }
        } catch (ClassNotFoundException e) {
            failures.add(binaryName + " — class not found: " + e.getMessage());
        }
    }
}
