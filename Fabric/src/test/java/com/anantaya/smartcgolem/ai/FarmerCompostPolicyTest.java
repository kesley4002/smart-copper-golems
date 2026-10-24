package com.anantaya.smartcgolem.ai;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Pure unit tests. Minecraft ItemStack requires registry bootstrap and cannot
 * be constructed safely inside the ordinary Gradle JUnit JVM.
 */
public class FarmerCompostPolicyTest {
    @Test
    public void oneItemCanOnlyBeConsumedAboveTheReserve() {
        assertFalse(FarmerCompostPolicy.canConsumeOne(32, 32));
        assertFalse(FarmerCompostPolicy.canConsumeOne(31, 32));
        assertTrue(FarmerCompostPolicy.canConsumeOne(33, 32));
        assertTrue(FarmerCompostPolicy.canConsumeOne(65, 64));
    }

    @Test
    public void reserveAcrossMultipleStacksAppliesToTotalStock() {
        int stackA = 16;
        int stackB = 20;
        int totalOfSameItem = stackA + stackB;
        int differentItem = 12;
        assertTrue(FarmerCompostPolicy.canConsumeOne(totalOfSameItem, 32));
        assertFalse(FarmerCompostPolicy.canConsumeOne(differentItem, 32));
        assertFalse(FarmerCompostPolicy.canConsumeOne(32, 32));
        assertTrue(FarmerCompostPolicy.canConsumeOne(1, 0));
    }
}
