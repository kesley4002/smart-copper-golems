package com.anantaya.smartcgolem.ai;

import org.junit.Test;
import static org.junit.Assert.*;

public class FarmerHarvestDropsTest {
    @Test
    public void acceptsOnlySupportedCropsAndComposterOutput() {
        for (String item : new String[]{
                "minecraft:wheat", "minecraft:wheat_seeds", "minecraft:carrot",
                "minecraft:potato", "minecraft:poisonous_potato",
                "minecraft:beetroot", "minecraft:beetroot_seeds",
                "minecraft:bone_meal"}) {
            assertTrue(item + " must be collectable by Farmer",
                    FarmerHarvestDrops.allowedItemId(item));
        }
    }

    @Test
    public void neverChasesMobLootOrUnrelatedItems() {
        for (String item : new String[]{
                "minecraft:rotten_flesh", "minecraft:bone", "minecraft:arrow",
                "minecraft:gunpowder", "minecraft:string", "minecraft:diamond",
                "minecraft:iron_ingot", "minecraft:oak_log"}) {
            assertFalse(item + " must not distract Farmer",
                    FarmerHarvestDrops.allowedItemId(item));
        }
        assertFalse(FarmerHarvestDrops.allowedItemId(null));
    }
}
