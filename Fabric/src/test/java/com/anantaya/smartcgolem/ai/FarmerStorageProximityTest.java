package com.anantaya.smartcgolem.ai;

import org.junit.Test;
import static org.junit.Assert.*;

public class FarmerStorageProximityTest {
    @Test
    public void normalChestMustBeBesideFarmer() {
        assertTrue(FarmerStorageProximity.normal(1.0D, 0.0D, 0.0D));
        assertTrue(FarmerStorageProximity.normal(1.0D, 1.0D, 1.0D));
        assertFalse(FarmerStorageProximity.normal(2.0D, 0.0D, 0.0D));
        assertFalse(FarmerStorageProximity.normal(1.0D, 2.0D, 0.0D));
        assertFalse(FarmerStorageProximity.normal(7.0D, 0.0D, 0.0D));
    }

    @Test
    public void upperChestRequiresLocalStretch() {
        assertTrue(FarmerStorageProximity.elevated(1.0D, 2, 1.0D));
        assertTrue(FarmerStorageProximity.elevated(0.0D, 5, 1.0D));
        assertFalse(FarmerStorageProximity.elevated(2.0D, 5, 0.0D));
        assertFalse(FarmerStorageProximity.elevated(1.0D, 6, 0.0D));
        assertFalse(FarmerStorageProximity.elevated(1.0D, 1, 0.0D));
    }
}
