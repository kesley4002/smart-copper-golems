package com.anantaya.smartcgolem.ai;

import net.minecraft.core.BlockPos;
import org.junit.Test;
import static org.junit.Assert.*;

public class FurnaceSearchTest {
    @Test
    public void detectsFurnacesBeyondTheOldTwelveBlockLimit() {
        BlockPos golem = new BlockPos(0, 64, 0);
        assertTrue(FurnaceSearch.inside(golem, new BlockPos(40, 64, 0), 64, 12));
        assertTrue(FurnaceSearch.inside(golem, new BlockPos(0, 64, 63), 64, 12));
        assertTrue(FurnaceSearch.inside(golem, new BlockPos(64, 74, 64), 64, 12));
        assertFalse(FurnaceSearch.inside(golem, new BlockPos(65, 64, 0), 64, 12));
        assertFalse(FurnaceSearch.inside(golem, new BlockPos(0, 77, 0), 64, 12));
        assertFalse(FurnaceSearch.inside(golem, new BlockPos(32, 64, 0), 16, 12));
    }

    @Test
    public void supportsNegativeCoordinatesAndEdges() {
        BlockPos golem = new BlockPos(-100, 70, -200);
        assertTrue(FurnaceSearch.inside(golem, new BlockPos(-164, 70, -264), 64, 12));
        assertFalse(FurnaceSearch.inside(golem, new BlockPos(-165, 70, -200), 64, 12));
    }

    @Test
    public void validatesBoundedConfigurableRange() {
        for (int i = 16; i <= 128; i += 8) assertTrue(FurnaceSearch.validRadius(i));
        for (int i : new int[]{0, 8, 17, 31, 65, 129, 256})
            assertFalse(FurnaceSearch.validRadius(i));
        assertEquals(64, FurnaceSearch.DEFAULT_RADIUS);
        assertEquals(128, FurnaceSearch.MAX_RADIUS);
    }
}
