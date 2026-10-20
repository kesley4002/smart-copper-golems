package com.anantaya.smartcgolem.util;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/** Pure packing regression checks: no Minecraft registry bootstrapping needed. */
public final class ContainerAlphabetizerTest {
    private static AlphabeticPacking.Entry<String> item(
            String id, String components, int count, int max) {
        return new AlphabeticPacking.Entry<>("minecraft:" + id, components, count, max);
    }

    @Test
    public void orderByEnglishRegistryIdAndConsolidateCompatibleStacks() {
        var items = List.of(item("iron_ingot", "default", 12, 64),
                item("coal", "default", 8, 64),
                item("diamond", "default", 4, 64),
                item("iron_ingot", "default", 30, 64));
        var result = AlphabeticPacking.compactAndSort(items, String::equals);
        assertEquals(3, result.size());
        assertEquals("minecraft:coal", result.get(0).itemId());
        assertEquals("minecraft:diamond", result.get(1).itemId());
        assertEquals("minecraft:iron_ingot", result.get(2).itemId());
        assertEquals(42, result.get(2).count());
        assertEquals(54, result.stream().mapToInt(AlphabeticPacking.Entry::count).sum());
    }

    @Test
    public void splitsAtStackLimitsAndConservesCounts() {
        var result = AlphabeticPacking.compactAndSort(List.of(
                item("iron_ingot", "default", 55, 64),
                item("iron_ingot", "default", 36, 64),
                item("diamond", "default", 7, 64)), String::equals);
        assertEquals(3, result.size());
        assertEquals(7, result.get(0).count());
        assertEquals(64, result.get(1).count());
        assertEquals(27, result.get(2).count());
        assertEquals(98, result.stream().mapToInt(AlphabeticPacking.Entry::count).sum());
    }

    @Test
    public void preservesDifferentComponentsIncludingEnchantmentsAndNames() {
        var result = AlphabeticPacking.compactAndSort(List.of(
                item("diamond_sword", "sharpness", 1, 1),
                item("diamond_sword", "renamed", 1, 1),
                item("iron_ingot", "custom", 3, 64),
                item("iron_ingot", "default", 5, 64)), String::equals);
        assertEquals(4, result.size());
        assertEquals("sharpness", result.get(0).variant());
        assertEquals("renamed", result.get(1).variant());
        assertEquals("custom", result.get(2).variant());
        assertEquals("default", result.get(3).variant());
    }

    @Test
    public void keepsNonStackablesDistinctAndStableOrdering() {
        var result = AlphabeticPacking.compactAndSort(List.of(
                item("iron_sword", "a", 1, 1),
                item("diamond_sword", "b", 1, 1),
                item("iron_sword", "a", 1, 1)), String::equals);
        assertEquals(3, result.size());
        assertEquals("minecraft:diamond_sword", result.get(0).itemId());
        assertEquals("minecraft:iron_sword", result.get(1).itemId());
        assertEquals("minecraft:iron_sword", result.get(2).itemId());
    }

    @Test
    public void rejectsMalformedInputAndSortIsIdempotent() {
        assertThrows(IllegalArgumentException.class, () -> item("iron_ingot", "a", 65, 64));
        var source = List.of(item("coal", "a", 64, 64), item("diamond", "a", 2, 64));
        var first = AlphabeticPacking.compactAndSort(source, String::equals);
        var second = AlphabeticPacking.compactAndSort(first, String::equals);
        assertEquals(first, second);
    }
}
