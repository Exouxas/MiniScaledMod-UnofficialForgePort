package qouteall.mini_scaled.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import qouteall.mini_scaled.MiniScaledPortal;
import qouteall.mini_scaled.MiniScaledRegistries;
import qouteall.mini_scaled.ScaleBoxRecord;
import qouteall.mini_scaled.VoidDimension;
import qouteall.mini_scaled.block.ScaleBoxPlaceholderBlock;
import qouteall.mini_scaled.block.ScaleBoxPlaceholderBlockEntity;
import qouteall.q_misc_util.my_util.AARotation;

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
}
