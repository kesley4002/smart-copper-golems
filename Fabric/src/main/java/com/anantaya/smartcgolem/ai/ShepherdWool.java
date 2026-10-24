package com.anantaya.smartcgolem.ai;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Only the 16 raw sheep wool blocks are Pastor drops. Carpets, dyes,
 * mutton, weapons and other mob loot are NOT collected by this profession.
 */
public final class ShepherdWool {
    private ShepherdWool() {}

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() &&
                acceptsId(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static boolean acceptsId(String itemId) {
        if (itemId == null) return false;
        return switch (itemId) {
            case "minecraft:white_wool", "minecraft:orange_wool",
                 "minecraft:magenta_wool", "minecraft:light_blue_wool",
                 "minecraft:yellow_wool", "minecraft:lime_wool",
                 "minecraft:pink_wool", "minecraft:gray_wool",
                 "minecraft:light_gray_wool", "minecraft:cyan_wool",
                 "minecraft:purple_wool", "minecraft:blue_wool",
                 "minecraft:brown_wool", "minecraft:green_wool",
                 "minecraft:red_wool", "minecraft:black_wool" -> true;
            default -> false;
        };
    }

    /**
     * Minecraft 26.2 no longer exposes each color as a Blocks/Items constant
     * in these mappings. Resolve the actual registered item ID instead.
     */
    private static final java.util.Map<DyeColor, Item> WOOL_BY_COLOR =
            new java.util.EnumMap<>(DyeColor.class);

    public static Item woolFor(DyeColor color) {
        Item cached = WOOL_BY_COLOR.get(color);
        if (cached != null) return cached;
        String id = "minecraft:" + color.name().toLowerCase(java.util.Locale.ROOT) + "_wool";
        for (Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).toString().equals(id)) {
                WOOL_BY_COLOR.put(color, item);
                return item;
            }
        }
        // Fail closed: no registered wool means we must not shear an animal.
        return Items.AIR;
    }
}
