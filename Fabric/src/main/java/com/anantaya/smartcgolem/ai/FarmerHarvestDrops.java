package com.anantaya.smartcgolem.ai;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Dedicated Farmer pickup allowlist. Ordinary Collectors retain their
 * original ability to collect all items, including mob loot.
 */
public final class FarmerHarvestDrops {
    private FarmerHarvestDrops() {}

    public static boolean allowed(ItemStack stack) {
        return !stack.isEmpty()
                && allowedItemId(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    /** Pure policy; testable in JUnit without bootstrapping Minecraft registries. */
    public static boolean allowedItemId(String itemId) {
        if (itemId == null) return false;
        return switch (itemId) {
            case "minecraft:wheat", "minecraft:wheat_seeds",
                 "minecraft:carrot", "minecraft:potato",
                 "minecraft:poisonous_potato", "minecraft:beetroot",
                 "minecraft:beetroot_seeds",
                 // A composter produces bone meal as a physical item.
                 "minecraft:bone_meal" -> true;
            default -> false;
        };
    }
}
