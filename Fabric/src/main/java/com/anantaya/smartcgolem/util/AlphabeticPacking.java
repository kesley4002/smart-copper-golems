/* Smart Copper Golems Extended — Minecraft-free packing algorithm. Apache-2.0. */
package com.anantaya.smartcgolem.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;

/** Pure, testable slot-packing algorithm shared by chests and barrels. */
public final class AlphabeticPacking {
    private AlphabeticPacking() {}

    public record Entry<T>(String itemId, T variant, int count, int stackLimit) {
        public Entry {
            if (itemId == null || variant == null || count < 1 || stackLimit < 1 || count > stackLimit)
                throw new IllegalArgumentException("Invalid stack");
        }
    }

    public static <T> List<Entry<T>> compactAndSort(
            List<Entry<T>> input, BiPredicate<T, T> compatible) {
        List<Entry<T>> output = new ArrayList<>();

        for (Entry<T> stack : input) {
            int remaining = stack.count();
            for (int i = 0; i < output.size() && remaining > 0; i++) {
                Entry<T> prev = output.get(i);
                if (!prev.itemId().equals(stack.itemId())
                        || !compatible.test(prev.variant(), stack.variant())) continue;
                int space = prev.stackLimit() - prev.count();
                if (space <= 0) continue;
                int transfer = Math.min(space, remaining);
                output.set(i, new Entry<>(prev.itemId(), prev.variant(), prev.count() + transfer, prev.stackLimit()));
                remaining -= transfer;
            }
            if (remaining > 0) {
                output.add(new Entry<>(stack.itemId(), stack.variant(), remaining, stack.stackLimit()));
            }
        }
        // List.sort is stable: the original order of variants with equal IDs is preserved.
        output.sort(Comparator.comparing(Entry::itemId));
        return List.copyOf(output);
    }
}
