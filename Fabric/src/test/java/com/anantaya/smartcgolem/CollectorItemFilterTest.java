package com.anantaya.smartcgolem;

import org.junit.Test;
import static org.junit.Assert.*;

public class CollectorItemFilterTest {
    @Test
    public void unconfiguredCollectorKeepsItsOriginalPickupBehavior() {
        assertTrue(ServerCollectorSettings.matches(null, "minecraft:egg"));
        assertTrue(ServerCollectorSettings.matches(null, "minecraft:rotten_flesh"));
        assertTrue(ServerCollectorSettings.matches(null, "minecraft:diamond"));
    }

    @Test
    public void filterForEggsRejectsOtherDrops() {
        assertTrue(ServerCollectorSettings.matches("minecraft:egg", "minecraft:egg"));
        assertFalse(ServerCollectorSettings.matches("minecraft:egg", "minecraft:rotten_flesh"));
        assertFalse(ServerCollectorSettings.matches("minecraft:egg", "minecraft:feather"));
        assertFalse(ServerCollectorSettings.matches("minecraft:egg", "minecraft:wheat"));
    }
}
