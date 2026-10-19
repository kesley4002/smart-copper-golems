/* Smart Copper Golems Extended additions (Kesley), Apache-2.0. */
package com.anantaya.smartcgolem.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

public final class SmartCategories {
    private SmartCategories() {}

    public static String category(ItemStack stack) {
        if (stack.isEmpty()) return null;
        boolean wood = stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS)
                || stack.is(ItemTags.WOODEN_STAIRS) || stack.is(ItemTags.WOODEN_SLABS)
                || stack.is(ItemTags.WOODEN_FENCES) || stack.is(ItemTags.FENCE_GATES)
                || stack.is(ItemTags.WOODEN_DOORS) || stack.is(ItemTags.WOODEN_TRAPDOORS)
                || stack.is(ItemTags.WOODEN_BUTTONS) || stack.is(ItemTags.WOODEN_PRESSURE_PLATES)
                || stack.is(ItemTags.SIGNS) || stack.is(ItemTags.HANGING_SIGNS);
        return CategoryPolicy.classify(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                wood, stack.is(ItemTags.WOOL) || stack.is(ItemTags.WOOL_CARPETS),
                stack.has(DataComponents.FOOD));
    }
}
