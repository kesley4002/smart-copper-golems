package com.anantaya.smartcgolem.util;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.Test;
import org.junit.BeforeClass;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import static org.junit.Assert.*;

/** Tests ordering, stacking, counts, and idempotency of destination containers. */
public final class ContainerAlphabetizerTest {
    @BeforeClass
    public static void setUpMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void sortsByEnglishIdAndConsolidatesCompatibleStacks() {
        SimpleContainer barrel = new SimpleContainer(27);
        barrel.setItem(0, new ItemStack(Items.IRON_INGOT, 12));
        barrel.setItem(4, new ItemStack(Items.COAL, 8));
        barrel.setItem(9, new ItemStack(Items.DIAMOND, 4));
        barrel.setItem(20, new ItemStack(Items.IRON_INGOT, 30));

        assertTrue(ContainerAlphabetizer.sortAfterDeposit(barrel));
        assertEquals(Items.COAL, barrel.getItem(0).getItem());
        assertEquals(8, barrel.getItem(0).getCount());
        assertEquals(Items.DIAMOND, barrel.getItem(1).getItem());
        assertEquals(4, barrel.getItem(1).getCount());
        assertEquals(Items.IRON_INGOT, barrel.getItem(2).getItem());
        assertEquals(42, barrel.getItem(2).getCount());
        for (int i = 3; i < barrel.getContainerSize(); i++) {
            assertTrue("Unexpected item in slot " + i, barrel.getItem(i).isEmpty());
        }
        assertFalse("Second pass must be a no-op", ContainerAlphabetizer.sortAfterDeposit(barrel));
    }

    @Test
    public void neverExceedsStackSizeOrLosesItems() {
        SimpleContainer doubleChest = new SimpleContainer(54);
        doubleChest.setItem(0, new ItemStack(Items.IRON_INGOT, 55));
        doubleChest.setItem(10, new ItemStack(Items.IRON_INGOT, 36));
        doubleChest.setItem(30, new ItemStack(Items.DIAMOND, 7));

        assertTrue(ContainerAlphabetizer.sortAfterDeposit(doubleChest));
        assertEquals(Items.DIAMOND, doubleChest.getItem(0).getItem());
        assertEquals(7, doubleChest.getItem(0).getCount());
        assertEquals(64, doubleChest.getItem(1).getCount());
        assertEquals(27, doubleChest.getItem(2).getCount());
        int total = 0;
        for (int i = 0; i < doubleChest.getContainerSize(); i++) {
            ItemStack stack = doubleChest.getItem(i);
            total += stack.getCount();
            if (!stack.isEmpty()) assertTrue(stack.getCount() <= stack.getMaxStackSize());
        }
        assertEquals(98, total);
    }

    @Test
    public void preservesDifferentItemKindsAndNonStackables() {
        SimpleContainer inv = new SimpleContainer(27);
        inv.setItem(0, new ItemStack(Items.IRON_SWORD));
        inv.setItem(1, new ItemStack(Items.DIAMOND_SWORD));
        inv.setItem(9, new ItemStack(Items.IRON_SWORD));
        assertTrue(ContainerAlphabetizer.sortAfterDeposit(inv));
        assertEquals(Items.DIAMOND_SWORD, inv.getItem(0).getItem());
        assertEquals(Items.IRON_SWORD, inv.getItem(1).getItem());
        assertEquals(Items.IRON_SWORD, inv.getItem(2).getItem());
        assertEquals(1, inv.getItem(1).getCount());
        assertEquals(1, inv.getItem(2).getCount());
    }

    @Test
    public void leavesAlreadySortedInventoryUnchanged() {
        SimpleContainer inv = new SimpleContainer(27);
        inv.setItem(0, new ItemStack(Items.COAL, 64));
        inv.setItem(1, new ItemStack(Items.DIAMOND, 2));
        assertFalse(ContainerAlphabetizer.sortAfterDeposit(inv));
    }
}
