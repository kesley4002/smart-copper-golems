package com.anantaya.smartcgolem.util;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression coverage for the old inclusive 32x8 search box. */
public class SearchVolumeTest {
    @Test
    public void inclusiveBoundsAndOutOfRangePositions() {
        SearchVolume v = new SearchVolume(0, 64, 0, 32, 8);
        assertTrue(v.contains(32, 72, -32));
        assertTrue(v.contains(-32, 56, 32));
        assertTrue(v.contains(0, 64, 0));
        assertFalse(v.contains(33, 64, 0));
        assertFalse(v.contains(0, 73, 0));
        assertFalse(v.contains(0, 64, -33));
        assertEquals(-2, v.minChunkX());
        assertEquals(2, v.maxChunkX());
        assertEquals(-2, v.minChunkZ());
        assertEquals(2, v.maxChunkZ());
    }

    @Test
    public void correctlyCoversNegativeAndChunkBoundaryCoordinates() {
        SearchVolume a = new SearchVolume(-1, 70, -16, 16, 1);
        assertEquals(-2, a.minChunkX());
        assertEquals(0, a.maxChunkX());
        assertEquals(-2, a.minChunkZ());
        assertEquals(0, a.maxChunkZ());
        assertTrue(a.contains(-17, 69, 0));
        assertTrue(a.contains(15, 71, -32));
        assertFalse(a.contains(16, 70, 0));
    }

    @Test
    public void searchRadiusZeroAndInvalidRadius() {
        SearchVolume v = new SearchVolume(31, 5, -1, 0, 0);
        assertTrue(v.contains(31, 5, -1));
        assertFalse(v.contains(31, 6, -1));
        assertFalse(v.contains(32, 5, -1));
        assertEquals(1, v.minChunkX());
        assertEquals(-1, v.minChunkZ());
        assertThrows(IllegalArgumentException.class, () -> new SearchVolume(0, 0, 0, -1, 0));
    }

    @Test
    public void chunkRangesCoverEveryCandidatePoint() {
        for (int center = -35; center <= 35; center += 5) {
            SearchVolume v = new SearchVolume(center, 80, -center, 32, 8);
            for (int x = center - 32; x <= center + 32; x++) {
                int chunkX = Math.floorDiv(x, 16);
                assertTrue(chunkX >= v.minChunkX() && chunkX <= v.maxChunkX());
            }
            for (int z = -center - 32; z <= -center + 32; z++) {
                int chunkZ = Math.floorDiv(z, 16);
                assertTrue(chunkZ >= v.minChunkZ() && chunkZ <= v.maxChunkZ());
            }
        }
    }
}
