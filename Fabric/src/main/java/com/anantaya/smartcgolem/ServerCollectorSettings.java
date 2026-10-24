package com.anantaya.smartcgolem;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * World-local filters keyed by the golem UUID, never its display name.
 * Unconfigured Collectors continue to pick up ALL items.
 */
public final class ServerCollectorSettings {
    private ServerCollectorSettings() {}
    private static final String FILE = "smart-copper-golem-collectors.properties";
    private static final Map<UUID, String> ITEM_FILTERS = new HashMap<>();
    private static Path path;

    public static String itemFilter(UUID golem) { return ITEM_FILTERS.get(golem); }
    public static boolean accepts(UUID golem, String itemId) {
        return matches(ITEM_FILTERS.get(golem), itemId);
    }

    public static boolean matches(String wanted, String actual) {
        return wanted == null || wanted.equals(actual);
    }

    public static void load(MinecraftServer server) {
        ITEM_FILTERS.clear();
        path = server.getWorldPath(LevelResource.ROOT).resolve(FILE);
        if (!Files.isRegularFile(path)) return;
        Properties props = new Properties();
        try (InputStream stream = Files.newInputStream(path)) {
            props.load(stream);
            for (String key : props.stringPropertyNames()) {
                if (!key.startsWith("item.")) continue;
                try {
                    UUID uuid = UUID.fromString(key.substring(5));
                    String id = props.getProperty(key).strip();
                    if (id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) ITEM_FILTERS.put(uuid, id);
                } catch (IllegalArgumentException ignored) {
                    SmartCopperGolem.LOGGER.warn("Invalid Collector filter entry {}", key);
                }
            }
        } catch (IOException e) {
            SmartCopperGolem.LOGGER.error("Cannot load Collector filters", e);
        }
    }

    public static void change(UUID golem, String itemId) throws IOException {
        if (itemId != null && !itemId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            throw new IllegalArgumentException("Item ID inválido");
        String old = ITEM_FILTERS.get(golem);
        if (itemId == null) ITEM_FILTERS.remove(golem);
        else ITEM_FILTERS.put(golem, itemId);
        try { save(); }
        catch (IOException e) {
            if (old == null) ITEM_FILTERS.remove(golem);
            else ITEM_FILTERS.put(golem, old);
            throw e;
        }
    }

    public static void unload() {
        ITEM_FILTERS.clear();
        path = null;
    }

    private static void save() throws IOException {
        if (path == null) throw new IOException("Mundo não carregado");
        Files.createDirectories(path.getParent());
        Properties props = new Properties();
        ITEM_FILTERS.forEach((uuid, id) -> props.setProperty("item." + uuid, id));
        Path temp = path.resolveSibling(FILE + ".tmp");
        try {
            try (OutputStream stream = Files.newOutputStream(temp)) {
                props.store(stream, "Collector filters by Copper Golem UUID");
            }
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temp); }
    }
}
