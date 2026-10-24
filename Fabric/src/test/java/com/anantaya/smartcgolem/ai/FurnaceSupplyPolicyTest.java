package com.anantaya.smartcgolem.ai;

import org.junit.Test;
import static org.junit.Assert.*;

public class FurnaceSupplyPolicyTest {
    @Test
    public void refillWhenFuelIsLowWithoutOverstocking() {
        assertEquals(8, FurnaceSupplyPolicy.requiredCount(0,8,64));
        assertEquals(4, FurnaceSupplyPolicy.requiredCount(4,8,16));
        assertEquals(0, FurnaceSupplyPolicy.requiredCount(5,8,64));
        assertEquals(0, FurnaceSupplyPolicy.requiredCount(8,8,64));
        assertEquals(2, FurnaceSupplyPolicy.requiredCount(0,8,2));
    }

    @Test
    public void preserveAtLeastOneAtEachSourceStack() {
        assertEquals(0, FurnaceSupplyPolicy.requiredCount(0,8,0));
        assertEquals(1, FurnaceSupplyPolicy.requiredCount(0,1,5));
        assertEquals(0, FurnaceSupplyPolicy.requiredCount(1,1,5));
        assertEquals(0, FurnaceSupplyPolicy.requiredCount(0,8,-1));
    }
}
