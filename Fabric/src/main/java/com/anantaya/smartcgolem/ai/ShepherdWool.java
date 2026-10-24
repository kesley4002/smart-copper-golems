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

    public static Item woolFor(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_WOOL;
            case ORANGE -> Items.ORANGE_WOOL;
            case MAGENTA -> Items.MAGENTA_WOOL;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
            case YELLOW -> Items.YELLOW_WOOL;
            case LIME -> Items.LIME_WOOL;
            case PINK -> Items.PINK_WOOL;
            case GRAY -> Items.GRAY_WOOL;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
            case CYAN -> Items.CYAN_WOOL;
            case PURPLE -> Items.PURPLE_WOOL;
            case BLUE -> Items.BLUE_WOOL;
            case BROWN -> Items.BROWN_WOOL;
            case GREEN -> Items.GREEN_WOOL;
            case RED -> Items.RED_WOOL;
            case BLACK -> Items.BLACK_WOOL;
        };
    }
}
