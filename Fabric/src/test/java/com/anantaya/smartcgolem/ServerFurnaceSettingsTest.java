package com.anantaya.smartcgolem;

import org.junit.Test;
import static org.junit.Assert.*;

public class ServerFurnaceSettingsTest {
    @Test
    public void acceptsOnlyFullCoalBurnMultiplesInsideOneStack() {
        for (int target = 8; target <= 64; target += 8) {
            assertTrue("Target " + target + " must be accepted",
                    ServerFurnaceSettings.validCoalTarget(target));
        }
        for (int target : new int[] {-8, 0, 1, 7, 9, 15, 17, 65, 72, 128}) {
            assertFalse("Target " + target + " must be rejected",
                    ServerFurnaceSettings.validCoalTarget(target));
        }
    }
}
