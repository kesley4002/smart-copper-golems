package com.anantaya.smartcgolem.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Server-wide sort language, loaded and saved by ServerSortSettings. */
public final class SortLanguage {
    private SortLanguage() {}

    public enum Mode { EN, PT }
    private static volatile Mode current = Mode.EN;
    private static final Map<String, String> PT_NAMES = loadTranslations();

    public static Mode current() { return current; }
    public static void select(Mode mode) { current = java.util.Objects.requireNonNull(mode); }
    public static String ptName(String id) { return PT_NAMES.get(id); }
    public static Map<String, String> ptNames() { return PT_NAMES; }

    /** English retains the already-tested ID order; PT uses official PT-BR names. */
    public static Comparator<String> comparator() {
        if (current == Mode.EN) return Comparator.naturalOrder();
        Collator brazilian = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        brazilian.setStrength(Collator.PRIMARY);
        return Comparator.comparing(
                (String id) -> PT_NAMES.getOrDefault(id, id),
                brazilian
        ).thenComparing(Comparator.naturalOrder());
    }

    private static Map<String, String> loadTranslations() {
        String resource = "/smart-copper-golem/item-names-pt_br.tsv";
        InputStream source = SortLanguage.class.getResourceAsStream(resource);
        if (source == null) throw new IllegalStateException("Missing " + resource);
        Map<String, String> names = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(source, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] fields = line.split("\\t", -1);
                if (fields.length != 2 || fields[1].isBlank()
                        || names.putIfAbsent(fields[0], fields[1]) != null) {
                    throw new IllegalStateException("Invalid or duplicate PT-BR translation: " + line);
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot load PT-BR names", e);
        }
        if (names.size() != CategoryPolicy.EXPECTED_ITEMS
                || !names.keySet().equals(CategoryPolicy.all().keySet())) {
            throw new IllegalStateException("PT-BR names must cover EXACTLY the catalog's 1413 IDs");
        }
        return Map.copyOf(names);
    }
}
