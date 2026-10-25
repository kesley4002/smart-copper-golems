package com.anantaya.smartcgolem;

import org.junit.Test;
import static org.junit.Assert.*;

public class ServerPrivateStorageSettingsTest {
    @Test
    public void regularGolemCanStillUsePublicChests() {
        assertTrue(ServerPrivateStorageSettings.allows(false, false));
        assertTrue(ServerPrivateStorageSettings.allows(false, true));
    }

    @Test
    public void privateOnlyGolemRefusesPublicContainers() {
        assertFalse(ServerPrivateStorageSettings.allows(true, false));
        assertTrue(ServerPrivateStorageSettings.allows(true, true));
    }
}
