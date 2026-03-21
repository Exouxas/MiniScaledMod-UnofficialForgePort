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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import qouteall.mini_scaled.MiniScaledPortal;
import qouteall.mini_scaled.MiniScaledRegistries;
import qouteall.mini_scaled.ScaleBoxEntranceCreation;
import qouteall.mini_scaled.ScaleBoxRecord;
import qouteall.mini_scaled.VoidDimension;
import qouteall.mini_scaled.block.ScaleBoxPlaceholderBlock;
import qouteall.mini_scaled.block.ScaleBoxPlaceholderBlockEntity;
import qouteall.mini_scaled.util.MSUtil;
import qouteall.q_misc_util.my_util.AARotation;
import qouteall.q_misc_util.my_util.IntBox;

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
}
