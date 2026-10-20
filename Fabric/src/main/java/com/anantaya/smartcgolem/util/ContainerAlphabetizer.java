/* Smart Copper Golems Extended — automatic vanilla container ordering. Apache-2.0. */
package com.anantaya.smartcgolem.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Re-packs an entire barrel or logical double chest after successful deposit.
 * Sorting and merging happen in memory, then contents are written in one pass.
 */
public final class ContainerAlphabetizer {
    private ContainerAlphabetizer() {}

    public static boolean sortAfterDeposit(Container inventory) {
        int slots = inventory.getContainerSize();
        List<ItemStack> before = new ArrayList<>(slots);
        List<AlphabeticPacking.Entry<ItemStack>> input = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            ItemStack stack = inventory.getItem(i).copy();
            before.add(stack);
            if (stack.isEmpty()) continue;
            input.add(new AlphabeticPacking.Entry<>(
                    BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                    stack,
                    stack.getCount(),
                    stack.getMaxStackSize()));
        }

        List<AlphabeticPacking.Entry<ItemStack>> ordered =
                AlphabeticPacking.compactAndSort(input, ItemStack::isSameItemSameComponents);
        if (ordered.size() > slots) {
            throw new IllegalStateException("Packed items exceed inventory slots");
        }

        List<ItemStack> result = new ArrayList<>(slots);
        for (AlphabeticPacking.Entry<ItemStack> entry : ordered) {
            result.add(entry.variant().copyWithCount(entry.count()));
        }
        while (result.size() < slots) result.add(ItemStack.EMPTY);

        boolean changed = false;
        for (int i = 0; i < slots; i++) {
            ItemStack previous = before.get(i);
            ItemStack next = result.get(i);
            if (previous.getCount() != next.getCount()
                    || !ItemStack.isSameItemSameComponents(previous, next)) {
                changed = true;
                break;
            }
        }
        if (!changed) return false;
        for (int i = 0; i < slots; i++) inventory.setItem(i, result.get(i));
        inventory.setChanged();
        return true;
    }
}
