package qouteall.mini_scaled;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for pure-logic methods in ScaleBoxGeneration that do not require
 * a running Minecraft instance (no Bootstrap.bootStrap() needed).
 */
public class ScaleBoxGenerationTest {

    // ---------------------------------------------------------------------------
    // isValidScale
    // ---------------------------------------------------------------------------

    @Test
    void validScalesAreAccepted() {
        assertTrue(ScaleBoxGeneration.isValidScale(4));
        assertTrue(ScaleBoxGeneration.isValidScale(8));
        assertTrue(ScaleBoxGeneration.isValidScale(16));
        assertTrue(ScaleBoxGeneration.isValidScale(32));
    }

    @Test
    void invalidScalesAreRejected() {
        assertFalse(ScaleBoxGeneration.isValidScale(0));
        assertFalse(ScaleBoxGeneration.isValidScale(1));
        assertFalse(ScaleBoxGeneration.isValidScale(3));
        assertFalse(ScaleBoxGeneration.isValidScale(5));
        assertFalse(ScaleBoxGeneration.isValidScale(17));
        assertFalse(ScaleBoxGeneration.isValidScale(64));
        assertFalse(ScaleBoxGeneration.isValidScale(-1));
        assertFalse(ScaleBoxGeneration.isValidScale(Integer.MAX_VALUE));
    }

    // ---------------------------------------------------------------------------
    // getNearestPosInScaleBoxToTeleportTo
    // The grid cell size is 512 (16 * 32). Each cell's canonical position is
    // at (cellIndex * 512 + 2, 66, cellIndex * 512 + 2).
    // ---------------------------------------------------------------------------

    /**
     * A point at the origin should snap to cell (0,0) → world pos (2, 66, 2).
     */
    @Test
    void originSnapsToFirstCell() {
        BlockPos result = ScaleBoxGeneration.getNearestPosInScaleBoxToTeleportTo(new BlockPos(0, 0, 0));
        assertEquals(2, result.getX(),  "x should be cell-0 base + 2");
        assertEquals(66, result.getY(), "y should always be 66");
        assertEquals(2, result.getZ(),  "z should be cell-0 base + 2");
    }

    /**
     * A point exactly one grid width away (512) should snap to cell (1,0).
     */
    @Test
    void oneGridWidthAwaySnapsToSecondCell() {
        BlockPos result = ScaleBoxGeneration.getNearestPosInScaleBoxToTeleportTo(new BlockPos(512, 100, 0));
        assertEquals(514, result.getX(), "x should be cell-1 base + 2");
        assertEquals(66,  result.getY());
        assertEquals(2,   result.getZ(), "z should be cell-0 base + 2");
    }

    /**
     * A point at (255, *, 255) is just inside the first cell's half-boundary
     * (255 < 256) and should still snap to cell (0,0).
     */
    @Test
    void justBeforeHalfBoundaryStaysInFirstCell() {
        BlockPos result = ScaleBoxGeneration.getNearestPosInScaleBoxToTeleportTo(new BlockPos(255, 0, 255));
        assertEquals(2, result.getX(), "x should still be cell-0 base + 2");
        assertEquals(2, result.getZ(), "z should still be cell-0 base + 2");
    }

    /**
     * A point at (768, *, 768) is halfway between cell-1 (512) and cell-2 (1024)
     * and should snap to cell-2 (Java Math.round rounds .5 toward +infinity).
     */
    @Test
    void halfwayBetweenCellsRoundsToHigherCell() {
        // 768 / 512 = 1.5 → Math.round gives 2 → 2*512+2 = 1026
        BlockPos result = ScaleBoxGeneration.getNearestPosInScaleBoxToTeleportTo(new BlockPos(768, 0, 768));
        assertEquals(1026, result.getX());
        assertEquals(1026, result.getZ());
    }

    /**
     * A point at (1024, *, 0) should snap to cell (2,0).
     */
    @Test
    void twoGridWidthsSnapsToThirdCell() {
        BlockPos result = ScaleBoxGeneration.getNearestPosInScaleBoxToTeleportTo(new BlockPos(1024, 0, 0));
        assertEquals(1026, result.getX());
        assertEquals(2,    result.getZ());
    }

    /**
     * Y coordinate is always 66 regardless of input Y.
     */
    @Test
    void yIsAlways66() {
        for (int y : new int[]{-100, 0, 64, 200, 10000}) {
            BlockPos result = ScaleBoxGeneration.getNearestPosInScaleBoxToTeleportTo(new BlockPos(0, y, 0));
            assertEquals(66, result.getY(), "y should always be 66 for input y=" + y);
        }
    }

    /**
     * Negative X snaps to the nearest cell in the negative direction.
     * -256 is exactly half a cell away from cell 0 (512/2=256), rounds to cell 0 (toward +inf at .5).
     * -257 is just past halfway, rounds to cell -1 → -512+2 = -510.
     */
    @Test
    void negativeCoordinatesSnapCorrectly() {
        // -256/512 = -0.5 → Math.round(-0.5) = 0 → 0*512+2 = 2
        BlockPos atNeg256 = ScaleBoxGeneration.getNearestPosInScaleBoxToTeleportTo(new BlockPos(-256, 0, 0));
        assertEquals(2, atNeg256.getX());

        // -257/512 ≈ -0.502 → Math.round gives -1 → -1*512+2 = -510
        BlockPos atNeg257 = ScaleBoxGeneration.getNearestPosInScaleBoxToTeleportTo(new BlockPos(-257, 0, 0));
        assertEquals(-510, atNeg257.getX());
    }
}
