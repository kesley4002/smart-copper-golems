package com.anantaya.smartcgolem.config;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** World-server settings, not per-player settings. */
public final class GolemSettings {
    private static final Path FILE = Paths.get("config", "smart-copper-golem.properties");
    private static int capacity = 16;
    private static final Map<String, Set<String>> categories = new LinkedHashMap<>();
    private GolemSettings() {}

    public static synchronized int capacity() { return capacity; }
    public static synchronized void capacity(int value) {
        if (value != 16 && value != 32 && value != 64) throw new IllegalArgumentException("Capacidade aceita: 16, 32 ou 64.");
        capacity = value;
        save();
    }
    public static synchronized Set<String> names() { return Set.copyOf(categories.keySet()); }
    public static synchronized Set<String> members(String name) {
        return Set.copyOf(categories.getOrDefault(name, Set.of()));
    }
    private static String normalized(String name) {
        if (!name.matches("[a-z0-9_\\-]{1,48}")) throw new IllegalArgumentException("Nome: apenas letras minusculas, numeros, _ e - (maximo 48).");
        return name;
    }
    public static synchronized void create(String name) {
        name = normalized(name);
        if (categories.containsKey(name)) throw new IllegalArgumentException("Categoria ja existe.");
        categories.put(name, new LinkedHashSet<>());
        save();
    }
    public static synchronized void delete(String name) {
        if (categories.remove(name) == null) throw new IllegalArgumentException("Categoria nao encontrada.");
        save();
    }
    public static synchronized void add(String name, String ids) {
        Set<String> current = categories.get(name);
        if (current == null) throw new IllegalArgumentException("Categoria nao encontrada.");
        Set<String> additions = parseIds(ids); // validate entire batch before applying
        current.addAll(additions);
        save();
    }
    public static synchronized void remove(String name, String ids) {
        Set<String> current = categories.get(name);
        if (current == null) throw new IllegalArgumentException("Categoria nao encontrada.");
        current.removeAll(parseIds(ids));
        save();
    }
    private static Set<String> parseIds(String raw) {
        Set<String> result = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            String id = token.trim();
            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
                throw new IllegalArgumentException("ID de item invalido: " + id);
            }
            result.add(key.toString());
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Informe pelo menos um ID.");
        return result;
    }
    public static synchronized boolean sameCategory(ItemStack frame, ItemStack carried) {
        if (frame.isEmpty() || carried.isEmpty()) return false;
        String representative = BuiltInRegistries.ITEM.getKey(frame.getItem()).toString();
        String item = BuiltInRegistries.ITEM.getKey(carried.getItem()).toString();
        for (Set<String> members : categories.values()) {
            if (members.contains(representative) && members.contains(item)) return true;
        }
        return false;
    }
    public static synchronized void reset() { categories.clear(); save(); }
    public static synchronized void restore() { categories.clear(); defaults(); save(); }
    private static void defaults() {
        // Conservative starter categories, editable in-game; not an exhaustive item catalogue.
        categories.put("pedras", new LinkedHashSet<>(List.of("minecraft:stone","minecraft:cobblestone","minecraft:granite","minecraft:diorite","minecraft:andesite","minecraft:deepslate")));
        categories.put("madeira", new LinkedHashSet<>(List.of("minecraft:oak_log","minecraft:oak_planks","minecraft:spruce_log","minecraft:spruce_planks","minecraft:birch_log","minecraft:birch_planks")));
        categories.put("minerios", new LinkedHashSet<>(List.of("minecraft:coal","minecraft:iron_ingot","minecraft:gold_ingot","minecraft:diamond","minecraft:emerald","minecraft:lapis_lazuli","minecraft:redstone")));
    }
    public static synchronized void load() {
        Properties props = new Properties();
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) { props.load(reader); }
            catch (IOException e) { throw new UncheckedIOException(e); }
        } else { defaults(); save(); return; }
        try { capacity = Integer.parseInt(props.getProperty("capacity", "16")); }
        catch (NumberFormatException e) { capacity = 16; }
        if (capacity != 16 && capacity != 32 && capacity != 64) capacity = 16;
        categories.clear();
        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith("category.")) continue;
            String name = key.substring("category.".length());
            try { normalized(name); categories.put(name, parseIds(props.getProperty(key))); }
            catch (IllegalArgumentException ignored) { categories.put(name, new LinkedHashSet<>()); }
        }
    }
    public static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Properties props = new Properties();
            props.setProperty("capacity", Integer.toString(capacity));
            for (var entry : categories.entrySet())
                props.setProperty("category." + entry.getKey(), String.join(",", entry.getValue()));
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                props.store(writer, "Smart Copper Golem server configuration");
            }
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
}