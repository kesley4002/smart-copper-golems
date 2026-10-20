/* Smart Copper Golems Extended — full Java 26.2 survival catalog (Kesley). Apache-2.0. */
package com.anantaya.smartcgolem.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Exact, immutable 1413-item / 43-category lookup generated from the curated
 * Survival Vanilla catalog. No broad heuristics that could accidentally accept
 * unknown items or overlap categories.
 *
 * An upright 0-degree frame uses this category for BOTH the representative and the
 * item the golem is carrying; all other frame rotations use exact-item logic.
 */
public final class CategoryPolicy {
    private CategoryPolicy() {}

    public static final int EXPECTED_ITEMS = 1413;
    public static final int EXPECTED_CATEGORIES = 43;

    private static final String RESOURCE = "/smart-copper-golem/category-index.tsv";
    private static final Map<String, String> ITEM_TO_CATEGORY = loadCatalog();
    private static final Set<String> CATEGORY_KEYS =
            Collections.unmodifiableSet(new HashSet<>(ITEM_TO_CATEGORY.values()));

    public static String classify(String itemId) {
        if (itemId == null) return null;
        return ITEM_TO_CATEGORY.get(itemId);
    }

    /** Immutable reference data, also used in regression tests. */
    public static Map<String, String> all() {
        return ITEM_TO_CATEGORY;
    }

    public static Set<String> categoryKeys() {
        return CATEGORY_KEYS;
    }

    private static Map<String, String> loadCatalog() {
        InputStream input = CategoryPolicy.class.getResourceAsStream(RESOURCE);
        if (input == null) throw new IllegalStateException("Missing item catalog: " + RESOURCE);

        Map<String, String> mapping = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] columns = line.split("\\t", -1);
                if (columns.length != 2 || !columns[0].matches("minecraft:[a-z0-9_]+")
                        || !columns[1].matches("[a-z][a-z0-9_]*")) {
                    throw new IllegalStateException("Malformed item mapping at " + RESOURCE
                            + ":" + lineNumber + ": " + line);
                }
                if (mapping.putIfAbsent(columns[0], columns[1]) != null) {
                    throw new IllegalStateException("Duplicate item mapping: " + columns[0]
                            + " at line " + lineNumber);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + RESOURCE, e);
        }

        if (mapping.size() != EXPECTED_ITEMS) {
            throw new IllegalStateException("Catalog has " + mapping.size()
                    + " items, expected " + EXPECTED_ITEMS);
        }
        if (new HashSet<>(mapping.values()).size() != EXPECTED_CATEGORIES) {
            throw new IllegalStateException("Catalog category count is not "
                    + EXPECTED_CATEGORIES);
        }
        return Map.copyOf(mapping);
    }
}
