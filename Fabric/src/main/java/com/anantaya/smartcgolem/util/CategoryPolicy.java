/* Smart Copper Golems Extended additions (Kesley), Apache-2.0. */
package com.anantaya.smartcgolem.util;

import java.util.Set;

/** Pure policy: one category per item, with explicit precedence. */
public final class CategoryPolicy {
    private CategoryPolicy() {}

    private static final Set<String> STONES = Set.of(
            "stone", "cobblestone", "mossy_cobblestone", "granite", "diorite", "andesite",
            "deepslate", "cobbled_deepslate", "tuff", "calcite", "dripstone_block",
            "blackstone", "basalt", "smooth_basalt", "polished_basalt", "end_stone",
            "netherrack", "sandstone", "red_sandstone", "stone_bricks", "mud_bricks",
            "bricks", "nether_bricks", "red_nether_bricks", "prismarine", "dark_prismarine",
            "prismarine_bricks", "end_stone_bricks", "deepslate_bricks", "deepslate_tiles",
            "polished_deepslate", "polished_blackstone", "polished_blackstone_bricks",
            "polished_tuff", "tuff_bricks", "polished_granite", "polished_diorite",
            "polished_andesite", "smooth_stone", "smooth_sandstone", "smooth_red_sandstone",
            "cut_sandstone", "cut_red_sandstone", "quartz_block", "quartz_bricks",
            "quartz_pillar", "smooth_quartz", "obsidian", "crying_obsidian");
    private static final Set<String> MINERALS = Set.of(
            "coal", "charcoal", "diamond", "emerald", "lapis_lazuli", "redstone",
            "quartz", "amethyst_shard", "iron_ingot", "gold_ingot", "copper_ingot",
            "netherite_ingot", "netherite_scrap", "iron_nugget", "gold_nugget", "copper_nugget",
            "raw_iron", "raw_gold", "raw_copper", "ancient_debris",
            "coal_block", "diamond_block", "emerald_block", "lapis_block", "redstone_block",
            "iron_block", "gold_block", "copper_block", "netherite_block",
            "raw_iron_block", "raw_gold_block", "raw_copper_block", "amethyst_block");

    public static String classify(String id, boolean wooden, boolean wool, boolean edible) {
        if (edible) return "food";
        if (wooden) return "wood";
        if (wool) return "wool";
        // Never infer a modded item's family from its name alone.
        if (!id.startsWith("minecraft:")) return null;
        String name = id.substring("minecraft:".length());
        if (MINERALS.contains(name) || Set.of("coal_ore", "iron_ore", "copper_ore",
                "gold_ore", "redstone_ore", "emerald_ore", "lapis_ore", "diamond_ore",
                "deepslate_coal_ore", "deepslate_iron_ore", "deepslate_copper_ore",
                "deepslate_gold_ore", "deepslate_redstone_ore", "deepslate_emerald_ore",
                "deepslate_lapis_ore", "deepslate_diamond_ore", "nether_gold_ore",
                "nether_quartz_ore").contains(name)) return "minerals";
        if (name.equals("glass") || name.equals("glass_pane") || name.equals("tinted_glass")
                || name.endsWith("_stained_glass") || name.endsWith("_stained_glass_pane")) return "glass";
        if (isStone(name)) return "stone";
        return null;
    }

    private static boolean isStone(String name) {
        if (STONES.contains(name)) return true;
        for (String prefix : new String[]{"chiseled_", "cracked_", "mossy_"}) {
            if (name.startsWith(prefix) && STONES.contains(name.substring(prefix.length()))) return true;
        }
        for (String suffix : new String[]{"_stairs", "_slab", "_wall"}) {
            if (name.endsWith(suffix)) {
                String base = name.substring(0, name.length() - suffix.length());
                return isStone(base) || isStone(base + "s") || base.equals("quartz");
            }
        }
        return false;
    }
}
