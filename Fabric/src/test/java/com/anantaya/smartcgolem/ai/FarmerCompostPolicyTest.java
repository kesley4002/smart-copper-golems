package com.anantaya.smartcgolem.ai;

import org.junit.Test;
import static org.junit.Assert.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleContainer;

public class FarmerCompostPolicyTest {
    @Test
    public void oneItemCanOnlyBeConsumedAboveTheReserve() {
        assertFalse(FarmerCompostPolicy.canConsumeOne(32, 32));
        assertFalse(FarmerCompostPolicy.canConsumeOne(31, 32));
        assertTrue(FarmerCompostPolicy.canConsumeOne(33, 32));
        assertTrue(FarmerCompostPolicy.canConsumeOne(65, 64));
    }

    @Test
    public void countsAcrossStacksToProtectAllSeeds() {
        SimpleContainer chest = new SimpleContainer(4);
        chest.setItem(0, new ItemStack(Items.WHEAT_SEEDS, 16));
        chest.setItem(1, new ItemStack(Items.WHEAT_SEEDS, 20));
        chest.setItem(2, new ItemStack(Items.WHEAT, 12));
        assertEquals(36, FarmerCompostPolicy.totalIn(chest, new ItemStack(Items.WHEAT_SEEDS)));
        assertEquals(12, FarmerCompostPolicy.totalIn(chest, new ItemStack(Items.WHEAT)));
        assertTrue(FarmerCompostPolicy.canConsumeOne(36, 32));
        assertFalse(FarmerCompostPolicy.canConsumeOne(12, 32));
    }

    @Test
    public void blacklistNonFarmItems() {
        assertFalse(FarmerCompostPolicy.eligible(ItemStack.EMPTY));
        assertFalse(FarmerCompostPolicy.eligible(new ItemStack(Items.DIAMOND)));
        assertFalse(FarmerCompostPolicy.eligible(new ItemStack(Items.OAK_LOG)));
        assertTrue(FarmerCompostPolicy.eligible(new ItemStack(Items.WHEAT_SEEDS)));
    }
}
