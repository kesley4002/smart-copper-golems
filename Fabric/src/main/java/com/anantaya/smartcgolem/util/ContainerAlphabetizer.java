/* Smart Copper Golems Extended — automatic vanilla container ordering. Apache-2.0. */
package com.anantaya.smartcgolem.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sorts a full barrel or logical double-chest inventory after a successful
 * copper-golem deposit. Uses stable English registry IDs, not display names.
 *
 * Every original stack is copied first; only identical item + component
 * combinations may merge. Items with damage, names, enchantments or different
 * custom components are kept separate. Does not run during idle/search ticks.
 */
public final class ContainerAlphabetizer {
    private ContainerAlphabetizer() {}

    public static boolean sortAfterDeposit(Container inventory) {
        final int slots = inventory.getContainerSize();
        final List<ItemStack> original = new ArrayList<>(slots);
        final List<ItemStack> compacted = new ArrayList<>(slots);

        for (int i = 0; i < slots; i++) {
            ItemStack stack = inventory.getItem(i);
            original.add(stack.copy());
            if (stack.isEmpty()) continue;

            ItemStack remaining = stack.copy();
            for (ItemStack existing : compacted) {
                if (remaining.isEmpty()) break;
                if (!ItemStack.isSameItemSameComponents(existing, remaining)) continue;

                int capacity = existing.getMaxStackSize() - existing.getCount();
                if (capacity <= 0) continue;

                int moved = Math.min(capacity, remaining.getCount());
                existing.grow(moved);
                remaining.shrink(moved);
            }
            if (!remaining.isEmpty()) {
                compacted.add(remaining.copy());
            }
        }

        // Stable sort retains the relative order of different components
        // sharing the same English ID; only their item kind is alphabetized.
        compacted.sort(Comparator.comparing(stack ->
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));

        if (compacted.size() > slots) {
            // Defensive: never partially alter an inventory if packing fails.
            throw new IllegalStateException("Container packing exceeded slot count");
        }

        boolean changed = false;
        for (int i = 0; i < slots; i++) {
            ItemStack expected = i < compacted.size() ? compacted.get(i) : ItemStack.EMPTY;
            ItemStack before = original.get(i);
            if (before.getCount() != expected.getCount()
                    || !ItemStack.isSameItemSameComponents(before, expected)) {
                changed = true;
                break;
            }
        }
        if (!changed) return false;

        for (int i = 0; i < slots; i++) {
            inventory.setItem(i, i < compacted.size() ? compacted.get(i) : ItemStack.EMPTY);
        }
        inventory.setChanged();
        return true;
    }
}
