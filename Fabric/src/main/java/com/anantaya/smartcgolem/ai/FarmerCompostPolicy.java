package com.anantaya.smartcgolem.ai;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;

/** All seeds and crops retain a floor in their designated compost bin. */
public final class FarmerCompostPolicy {
    private FarmerCompostPolicy() {}

    /** Deliberately narrow allowlist: never convert arbitrary player items. */
    public static boolean eligible(ItemStack stack) {
        if (stack.isEmpty()) return false;
        boolean crop = stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.WHEAT) || stack.is(Items.CARROT)
                || stack.is(Items.POTATO) || stack.is(Items.BEETROOT);
        return crop;
    }

    public static int totalIn(Container container, ItemStack item) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, item)) count += stack.getCount();
        }
        return count;
    }

    public static boolean canConsumeOne(int totalOfItem, int reserve) {
        return totalOfItem > reserve;
    }
}
