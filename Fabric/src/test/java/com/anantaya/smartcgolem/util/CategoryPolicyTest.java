package com.anantaya.smartcgolem.util;

import java.util.Objects;
import org.junit.Test;

/** Category policy regression tests executed by Gradle/JUnit. */
public final class CategoryPolicyTest {
    private static int checked;
    private static void check(String expected, String id, boolean wood, boolean wool, boolean food) {
        String actual = CategoryPolicy.classify(id, wood, wool, food);
        if (!Objects.equals(expected, actual)) throw new AssertionError(id + ": expected " + expected + ", got " + actual);
        checked++;
    }
    @Test
    public void categoryFamiliesAndBoundaries() {
        check("wood", "minecraft:oak_log", true, false, false);
        check("wood", "minecraft:crimson_planks", true, false, false);
        check("wood", "example:custom_log", true, false, false);
        check(null, "example:iron_ore", false, false, false);
        check("food", "minecraft:apple", false, false, true);
        check("food", "minecraft:golden_carrot", false, false, true);
        check("food", "example:meal", true, true, true);
        check("minerals", "minecraft:diamond", false, false, false);
        check("minerals", "minecraft:deepslate_iron_ore", false, false, false);
        check("minerals", "minecraft:raw_copper_block", false, false, false);
        check("minerals", "minecraft:quartz", false, false, false);
        check("stone", "minecraft:quartz_block", false, false, false);
        check("stone", "minecraft:stone_brick_stairs", false, false, false);
        check("stone", "minecraft:mossy_stone_brick_slab", false, false, false);
        check("stone", "minecraft:cobbled_deepslate_wall", false, false, false);
        check("stone", "minecraft:polished_andesite_slab", false, false, false);
        check("stone", "minecraft:quartz_stairs", false, false, false);
        check("glass", "minecraft:red_stained_glass_pane", false, false, false);
        check("glass", "minecraft:tinted_glass", false, false, false);
        check("wool", "minecraft:blue_carpet", false, true, false);
        check(null, "minecraft:diamond_sword", false, false, false);
        check(null, "minecraft:chest", false, false, false);
        check(null, "minecraft:air", false, false, false);
        check(null, "minecraft:oak_stairs", false, false, false);
        System.out.println(checked + " category policy checks passed");
    }
}
