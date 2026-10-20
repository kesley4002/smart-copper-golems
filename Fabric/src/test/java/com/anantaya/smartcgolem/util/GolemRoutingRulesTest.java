package com.anantaya.smartcgolem.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class GolemRoutingRulesTest {
    @Test public void unframedAndEmptyFramesNeverReceiveItems() {
        assertFalse(GolemRoutingRules.accepts(false, false, false));
        assertFalse(GolemRoutingRules.accepts(false, true, false));
        assertFalse(GolemRoutingRules.accepts(true, false, false));
        assertFalse(GolemRoutingRules.accepts(true, true, true));
        assertTrue(GolemRoutingRules.accepts(true, true, false));
    }
    @Test public void pickupHonorsVanillaStackLimitAndWorldSetting() {
        assertEquals(16, GolemRoutingRules.amountToCarry(64,64,16));
        assertEquals(32, GolemRoutingRules.amountToCarry(64,64,32));
        assertEquals(64, GolemRoutingRules.amountToCarry(64,64,64));
        assertEquals(1, GolemRoutingRules.amountToCarry(64,1,64));
        assertEquals(12, GolemRoutingRules.amountToCarry(12,64,32));
        assertThrows(IllegalArgumentException.class,
                () -> GolemRoutingRules.amountToCarry(50,64,30));
    }
    @Test public void remoteCannotTeleportThroughBasementFloor() {
        assertFalse(GolemRoutingRules.remoteDepositMayCrossHeight(55,65));
        assertFalse(GolemRoutingRules.remoteDepositMayCrossHeight(62,65));
        assertTrue(GolemRoutingRules.remoteDepositMayCrossHeight(63,65));
        assertTrue(GolemRoutingRules.remoteDepositMayCrossHeight(70,65));
    }
}
