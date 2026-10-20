package com.anantaya.smartcgolem.util;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
import java.util.Map;

/** Verify all official names and the differing EN/PT sorting orders. */
public final class SortLanguageTest {
    @Test
    public void portugueseNamesCoverEverySingleCatalogItem() {
        assertEquals(1413, SortLanguage.ptNames().size());
        assertEquals(CategoryPolicy.all().keySet(), SortLanguage.ptNames().keySet());
        assertEquals("Carvão", SortLanguage.ptName("minecraft:coal"));
        assertEquals("Diamante", SortLanguage.ptName("minecraft:diamond"));
        assertEquals("Lingote de Ferro", SortLanguage.ptName("minecraft:iron_ingot"));
        assertNull(SortLanguage.ptName("minecraft:invalid"));
    }

    @Test
    public void switchingLanguageChangesOrderingNotStackConsolidation() {
        try {
            var stacks = List.of(
                    new AlphabeticPacking.Entry<>("minecraft:gold_ingot", "a", 25, 64),
                    new AlphabeticPacking.Entry<>("minecraft:iron_ingot", "b", 12, 64),
                    new AlphabeticPacking.Entry<>("minecraft:iron_ingot", "b", 19, 64),
                    new AlphabeticPacking.Entry<>("minecraft:coal", "c", 9, 64));
            SortLanguage.select(SortLanguage.Mode.EN);
            var en = AlphabeticPacking.compactAndSort(stacks, String::equals,
                    SortLanguage.comparator());
            SortLanguage.select(SortLanguage.Mode.PT);
            var pt = AlphabeticPacking.compactAndSort(stacks, String::equals,
                    SortLanguage.comparator());
            assertEquals("minecraft:coal", en.get(0).itemId());
            assertEquals("minecraft:gold_ingot", en.get(1).itemId());
            assertEquals("minecraft:iron_ingot", en.get(2).itemId());
            assertEquals("minecraft:iron_ingot", pt.get(1).itemId());
            assertEquals("minecraft:gold_ingot", pt.get(2).itemId());
            assertEquals(31, en.get(2).count());
            assertEquals(31, pt.get(1).count());
            assertEquals(en.stream().mapToInt(AlphabeticPacking.Entry::count).sum(),
                    pt.stream().mapToInt(AlphabeticPacking.Entry::count).sum());
        } finally {
            SortLanguage.select(SortLanguage.Mode.EN);
        }
    }
}
