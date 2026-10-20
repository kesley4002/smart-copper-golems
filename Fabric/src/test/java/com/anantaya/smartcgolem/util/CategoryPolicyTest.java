package com.anantaya.smartcgolem.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.TreeSet;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Full regression coverage of the user-curated 1413-ID, 30-category catalog. */
public final class CategoryPolicyTest {
    @Test
    public void allSurvivalItemsHaveExactlyOneKnownCategory() {
        Map<String, String> catalog = CategoryPolicy.all();
        assertEquals("Missing or extra catalog IDs", 1413, catalog.size());
        assertEquals("Expected 30 categories", 30, CategoryPolicy.categoryKeys().size());
        assertEquals(30, new HashSet<>(catalog.values()).size());
        assertTrue(catalog.keySet().stream().allMatch(id -> id.startsWith("minecraft:")));
        assertTrue(catalog.values().stream().allMatch(c -> CategoryPolicy.categoryKeys().contains(c)));
        for (Map.Entry<String, String> item : catalog.entrySet()) {
            assertEquals("Incorrect category for " + item.getKey(),
                    item.getValue(), CategoryPolicy.classify(item.getKey()));
        }
        assertThrows(UnsupportedOperationException.class,
                () -> catalog.put("minecraft:test", "minerals"));
    }

    @Test
    public void exactOriginalCatalogMembershipFingerprintNeverDrifts() throws Exception {
        // SHA-256 of the alphabetically sorted 1413 unique IDs from the
        // user's original 61-section Java 26.2 Survival catalog, joined by LF.
        // Detects swapping a missing item for another even when the total
        // remains 1413 and all category totals look correct.
        String allIds = String.join("\\n", new TreeSet<>(CategoryPolicy.all().keySet()));
        String sha256 = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(
                        allIds.getBytes(StandardCharsets.UTF_8)));
        assertEquals("The catalog must contain EXACTLY the user's 1413 original IDs",
                "6e52a0750e4d09c8173380b48098ff4ad3327b349d273248d1f25362bd7d68ca",
                sha256);
    }

    @Test
    public void representativeItemsCoverEveryCategory() {
        assertEquals("stone_blocks", CategoryPolicy.classify("minecraft:stone"));
        assertEquals("stone_construction", CategoryPolicy.classify("minecraft:stone_stairs"));
        assertEquals("soil", CategoryPolicy.classify("minecraft:dirt"));
        assertEquals("special_blocks", CategoryPolicy.classify("minecraft:obsidian"));
        assertEquals("minerals", CategoryPolicy.classify("minecraft:diamond"));
        assertEquals("copper_building", CategoryPolicy.classify("minecraft:cut_copper"));
        assertEquals("wood", CategoryPolicy.classify("minecraft:oak_log"));
        assertEquals("wood_crafting", CategoryPolicy.classify("minecraft:oak_door"));
        assertEquals("vegetation", CategoryPolicy.classify("minecraft:oak_leaves"));
        assertEquals("glass", CategoryPolicy.classify("minecraft:glass"));
        assertEquals("wool", CategoryPolicy.classify("minecraft:white_wool"));
        assertEquals("concrete", CategoryPolicy.classify("minecraft:white_concrete"));
        assertEquals("terracotta", CategoryPolicy.classify("minecraft:terracotta"));
        assertEquals("dyes", CategoryPolicy.classify("minecraft:white_dye"));
        assertEquals("colorful_decor", CategoryPolicy.classify("minecraft:white_bed"));
        assertEquals("portable_storage", CategoryPolicy.classify("minecraft:shulker_box"));
        assertEquals("aquatic", CategoryPolicy.classify("minecraft:brain_coral"));
        assertEquals("agriculture", CategoryPolicy.classify("minecraft:wheat_seeds"));
        assertEquals("food", CategoryPolicy.classify("minecraft:bread"));
        assertEquals("mob_drops", CategoryPolicy.classify("minecraft:blaze_rod"));
        assertEquals("redstone", CategoryPolicy.classify("minecraft:repeater"));
        assertEquals("copper_machines", CategoryPolicy.classify("minecraft:copper_bulb"));
        assertEquals("transport", CategoryPolicy.classify("minecraft:rail"));
        assertEquals("tools", CategoryPolicy.classify("minecraft:diamond_pickaxe"));
        assertEquals("armor", CategoryPolicy.classify("minecraft:diamond_chestplate"));
        assertEquals("enchantments", CategoryPolicy.classify("minecraft:enchanted_book"));
        assertEquals("functional", CategoryPolicy.classify("minecraft:crafting_table"));
        assertEquals("lighting", CategoryPolicy.classify("minecraft:torch"));
        assertEquals("containers", CategoryPolicy.classify("minecraft:bucket"));
        assertEquals("music_archaeology", CategoryPolicy.classify("minecraft:music_disc_cat"));
        assertEquals(30, CategoryPolicy.categoryKeys().size());
    }

    @Test
    public void formerlySupportedAndBoundaryCases() {
        assertEquals("minerals", CategoryPolicy.classify("minecraft:coal"));
        assertEquals("minerals", CategoryPolicy.classify("minecraft:iron_ingot"));
        assertEquals("minerals", CategoryPolicy.classify("minecraft:amethyst_cluster"));
        assertEquals("wood", CategoryPolicy.classify("minecraft:crimson_planks"));
        assertEquals("wood", CategoryPolicy.classify("minecraft:oak_stairs"));
        assertEquals("wood_crafting", CategoryPolicy.classify("minecraft:oak_fence"));
        assertEquals("wood_crafting", CategoryPolicy.classify("minecraft:stick"));
        assertEquals("stone_construction", CategoryPolicy.classify("minecraft:quartz_stairs"));
        assertEquals("stone_blocks", CategoryPolicy.classify("minecraft:quartz_block"));
        assertEquals("glass", CategoryPolicy.classify("minecraft:red_stained_glass_pane"));
        assertEquals("wool", CategoryPolicy.classify("minecraft:blue_carpet"));
        assertEquals("food", CategoryPolicy.classify("minecraft:carrot"));
        assertEquals("food", CategoryPolicy.classify("minecraft:potato"));
        assertEquals("food", CategoryPolicy.classify("minecraft:glow_berries"));
        assertEquals("enchantments", CategoryPolicy.classify("minecraft:glistering_melon_slice"));
        assertEquals("special_blocks", CategoryPolicy.classify("minecraft:popped_chorus_fruit"));
        assertEquals("agriculture", CategoryPolicy.classify("minecraft:sugar"));
        assertEquals("portable_storage", CategoryPolicy.classify("minecraft:shulker_box"));
        assertNull(CategoryPolicy.classify("minecraft:bedrock"));
        assertNull(CategoryPolicy.classify("minecraft:command_block"));
        assertNull(CategoryPolicy.classify("minecraft:spawn_egg"));
        assertNull(CategoryPolicy.classify("example:custom_log"));
        assertNull(CategoryPolicy.classify("minecraft:unknown_26_2_item"));
        assertNull(CategoryPolicy.classify(null));
    }

    @Test
    public void manifestCategoryTotalsMatchTheActualCatalog() throws Exception {
        Map<String, Integer> actual = new HashMap<>();
        CategoryPolicy.all().values().forEach(key -> actual.merge(key, 1, Integer::sum));
        InputStream in = CategoryPolicyTest.class.getResourceAsStream(
                "/smart-copper-golem/category-manifest.tsv");
        assertNotNull("Missing category manifest in built mod", in);
        Set<String> manifestKeys = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isBlank()) continue;
                String[] columns = line.split("\\t", -1);
                assertEquals("Invalid category manifest row", 4, columns.length);
                assertTrue("Duplicate manifest category: " + columns[0],
                        manifestKeys.add(columns[0]));
                assertEquals("Catalog/manifest discrepancy for " + columns[0],
                        Integer.parseInt(columns[3]), actual.get(columns[0]).intValue());
            }
        }
        assertEquals(30, manifestKeys.size());
        assertEquals(CategoryPolicy.categoryKeys(), manifestKeys);
    }
}
