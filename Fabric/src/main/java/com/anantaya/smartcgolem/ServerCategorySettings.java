package com.anantaya.smartcgolem;

import com.anantaya.smartcgolem.util.CategoryPolicy;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * World-wide operator-controlled catalog overlay. Vanilla item membership is
 * immutable in CategoryPolicy: disabling defaults does NOT alter its source
 * data. Custom categories take precedence over defaults and are persisted.
 */
public final class ServerCategorySettings {
    private ServerCategorySettings() {}
    private static final String FILENAME = "smart-copper-golem-custom-categories.properties";
    private static final Map<String, Set<String>> CUSTOM = new HashMap<>();
    private static final Map<String, String> OVERRIDES = new HashMap<>();
    private static boolean defaultsEnabled = true;
    private static int generation = 0;
    private static Path file;

    public static int generation() { return generation; }
    public static boolean defaultsEnabled() { return defaultsEnabled; }
    public static Map<String, Set<String>> customCategories() {
        Map<String, Set<String>> result = new TreeMap<>();
        CUSTOM.forEach((key, values) -> result.put(key, Set.copyOf(values)));
        return Map.copyOf(result);
    }
    public static String classify(String itemId) {
        if (itemId == null) return null;
        String custom = OVERRIDES.get(itemId);
        return custom != null ? custom : defaultsEnabled ? CategoryPolicy.classify(itemId) : null;
    }

    public static void load(MinecraftServer server) {
        resetInMemory();
        file = server.getWorldPath(LevelResource.ROOT).resolve(FILENAME);
        if (!Files.isRegularFile(file)) return;
        Properties p = new Properties();
        try (InputStream stream = Files.newInputStream(file)) {
            p.load(stream);
            defaultsEnabled = Boolean.parseBoolean(p.getProperty("defaults", "true"));
            for (String key : p.stringPropertyNames()) {
                if (!key.startsWith("category.")) continue;
                String name = key.substring("category.".length());
                if (!validName(name)) continue;
                Set<String> ids = new HashSet<>();
                String list = p.getProperty(key);
                if (list != null && !list.isBlank()) {
                    for (String id : list.split(",")) {
                        if (validItemId(id)) ids.add(id);
                    }
                }
                CUSTOM.put(name, ids);
            }
            rebuild();
        } catch (Exception e) {
            SmartCopperGolem.LOGGER.error("Could not load custom golem categories", e);
            resetInMemory();
        }
    }

    public static void unload() {
        resetInMemory();
        file = null;
    }

    public static void create(String name) throws IOException {
        requireName(name);
        if (CUSTOM.containsKey(name) || CategoryPolicy.categoryKeys().contains(name))
            throw new IllegalArgumentException("Categoria já existe: " + name);
        if (CUSTOM.size() >= 128) throw new IllegalArgumentException("Limite de 128 categorias.");
        CUSTOM.put(name, new HashSet<>());
        commit();
    }

    public static void add(String name, String itemId) throws IOException {
        requireItemId(itemId);
        Set<String> ids = required(name);
        if (ids.size() >= 256 && !ids.contains(itemId))
            throw new IllegalArgumentException("Máximo de 256 itens por categoria.");
        for (Set<String> other : CUSTOM.values()) other.remove(itemId);
        ids.add(itemId);
        commit();
    }

    public static void remove(String name, String itemId) throws IOException {
        requireItemId(itemId);
        if (!required(name).remove(itemId))
            throw new IllegalArgumentException("O ID não está nessa categoria.");
        commit();
    }

    public static void delete(String name) throws IOException {
        if (CUSTOM.remove(name) == null) throw new IllegalArgumentException("Categoria inexistente.");
        commit();
    }

    public static void clearAll() throws IOException {
        defaultsEnabled = false;
        CUSTOM.clear();
        commit();
    }

    public static void restoreDefaults() throws IOException {
        defaultsEnabled = true;
        CUSTOM.clear();
        commit();
    }

    private static Set<String> required(String name) {
        Set<String> ids = CUSTOM.get(name);
        if (ids == null) throw new IllegalArgumentException("Categoria não existe: " + name);
        return ids;
    }
    private static boolean validName(String s) { return s != null && s.matches("[a-z][a-z0-9_]{0,31}"); }
    private static boolean validItemId(String s) { return s != null && s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"); }
    private static void requireName(String name) {
        if (!validName(name)) throw new IllegalArgumentException("Use nome minúsculo sem espaços, ex.: blocos_laranja.");
    }
    private static void requireItemId(String itemId) {
        if (!validItemId(itemId)) throw new IllegalArgumentException("Use ID completo, ex.: minecraft:orange_wool.");
    }
    private static void rebuild() {
        OVERRIDES.clear();
        new TreeMap<>(CUSTOM).forEach((name, ids) -> ids.forEach(id -> OVERRIDES.put(id, name)));
        generation++;
    }
    private static void resetInMemory() {
        defaultsEnabled = true;
        CUSTOM.clear();
        OVERRIDES.clear();
        generation++;
    }

    private static void commit() throws IOException {
        rebuild();
        if (file == null) throw new IOException("O servidor ainda não carregou o mundo.");
        Files.createDirectories(file.getParent());
        Properties props = new Properties();
        props.setProperty("defaults", Boolean.toString(defaultsEnabled));
        CUSTOM.forEach((key, ids) -> {
            ArrayList<String> sorted = new ArrayList<>(ids);
            sorted.sort(String::compareTo);
            props.setProperty("category." + key, String.join(",", sorted));
        });
        Path tmp = file.resolveSibling(FILENAME + ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(tmp)) {
                props.store(out, "Smart Copper Golems Extended categories");
            }
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
