package com.anantaya.smartcgolem.ai;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
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
            case WHITE -> Blocks.WHITE_WOOL.asItem();
            case ORANGE -> Blocks.ORANGE_WOOL.asItem();
            case MAGENTA -> Blocks.MAGENTA_WOOL.asItem();
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL.asItem();
            case YELLOW -> Blocks.YELLOW_WOOL.asItem();
            case LIME -> Blocks.LIME_WOOL.asItem();
            case PINK -> Blocks.PINK_WOOL.asItem();
            case GRAY -> Blocks.GRAY_WOOL.asItem();
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL.asItem();
            case CYAN -> Blocks.CYAN_WOOL.asItem();
            case PURPLE -> Blocks.PURPLE_WOOL.asItem();
            case BLUE -> Blocks.BLUE_WOOL.asItem();
            case BROWN -> Blocks.BROWN_WOOL.asItem();
            case GREEN -> Blocks.GREEN_WOOL.asItem();
            case RED -> Blocks.RED_WOOL.asItem();
            case BLACK -> Blocks.BLACK_WOOL.asItem();
        };
    }
}
