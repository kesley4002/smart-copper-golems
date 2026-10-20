/* Smart Copper Golems Extended — exact survival catalog, Kesley. Apache-2.0. */
package com.anantaya.smartcgolem.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class SmartCategories {
    private SmartCategories() {}

    /** Single source of truth; do not classify by heuristic tags/components. */
    public static String category(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return CategoryPolicy.classify(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
}
