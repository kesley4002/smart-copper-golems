package com.anantaya.smartcgolem.ai;

import org.junit.Test;
import static org.junit.Assert.*;

public class FurnaceConfigurablePolicyTest {
    @Test
    public void coalAndCharcoalRespectEightItemSteps() {
        assertEquals(8, FurnaceSupplyPolicy.configuredTarget(false, 8));
        assertEquals(16, FurnaceSupplyPolicy.configuredTarget(false, 16));
        assertEquals(24, FurnaceSupplyPolicy.configuredTarget(false, 24));
        assertEquals(32, FurnaceSupplyPolicy.configuredTarget(false, 32));
        assertEquals(64, FurnaceSupplyPolicy.configuredTarget(false, 64));
    }

    @Test
    public void coalBlocksAlwaysStayAtOneRegardlessOfConfiguredCoal() {
        for (int target = 8; target <= 64; target += 8) {
            assertEquals(1, FurnaceSupplyPolicy.configuredTarget(true, target));
        }
    }

    @Test
    public void toppingUpRespectsConfiguredGoalAndAvailableFuel() {
        assertEquals(16, FurnaceSupplyPolicy.requiredCount(0, 16, 64));
        assertEquals(8, FurnaceSupplyPolicy.requiredCount(8, 16, 64));
        assertEquals(0, FurnaceSupplyPolicy.requiredCount(9, 16, 64));
        assertEquals(20, FurnaceSupplyPolicy.requiredCount(4, 24, 20));
        assertEquals(32, FurnaceSupplyPolicy.requiredCount(32, 64, 64));
        assertEquals(0, FurnaceSupplyPolicy.requiredCount(33, 64, 64));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTargetsThatAreNotMultiplesOfEight() {
        FurnaceSupplyPolicy.configuredTarget(false, 10);
    }
}
