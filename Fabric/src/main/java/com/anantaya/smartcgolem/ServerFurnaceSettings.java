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
 * Persisted coal/charcoal targets for furnace tenders.
 * Targets are multiples of 8, bounded by one vanilla inventory stack.
 * Overrides follow entity UUIDs, not mutable display names.
 */
public final class ServerFurnaceSettings {
    private ServerFurnaceSettings() {}

    public static final int DEFAULT_COAL = 8;
    private static final String FILE = "smart-copper-golem-furnaces.properties";
    private static final Map<UUID, Integer> PER_GOLEM = new HashMap<>();
    private static int globalCoal = DEFAULT_COAL;
    private static Path path;

    public static boolean validCoalTarget(int target) {
        return target >= 8 && target <= 64 && target % 8 == 0;
    }

    public static int globalCoal() { return globalCoal; }

    public static Integer individualCoal(UUID uuid) {
        return PER_GOLEM.get(uuid);
    }

    public static int coal(UUID uuid) {
        return PER_GOLEM.getOrDefault(uuid, globalCoal);
    }

    public static void load(MinecraftServer server) {
        globalCoal = DEFAULT_COAL;
        PER_GOLEM.clear();
        path = server.getWorldPath(LevelResource.ROOT).resolve(FILE);
        if (!Files.isRegularFile(path)) return;
        Properties data = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            data.load(in);
            try {
                int saved = Integer.parseInt(data.getProperty("coal.target", "8"));
                if (validCoalTarget(saved)) globalCoal = saved;
            } catch (NumberFormatException ex) {
                SmartCopperGolem.LOGGER.warn("Invalid default furnace fuel amount.");
            }
            for (String key : data.stringPropertyNames()) {
                if (!key.startsWith("golem.")) continue;
                try {
                    UUID id = UUID.fromString(key.substring("golem.".length()));
                    int target = Integer.parseInt(data.getProperty(key));
                    if (validCoalTarget(target)) PER_GOLEM.put(id, target);
                } catch (IllegalArgumentException e) {
                    SmartCopperGolem.LOGGER.warn("Invalid Furnace Tender setting: {}", key);
                }
            }
        } catch (IOException ex) {
            SmartCopperGolem.LOGGER.error("Cannot read Furnace Tender settings", ex);
        }
    }

    public static void setGlobalCoal(int target) throws IOException {
        if (!validCoalTarget(target))
            throw new IllegalArgumentException("Use 8, 16, 24, 32, 40, 48, 56 ou 64.");
        int previous = globalCoal;
        globalCoal = target;
        try { save(); } catch (IOException e) { globalCoal = previous; throw e; }
    }

    public static void setIndividualCoal(UUID id, Integer target) throws IOException {
        if (target != null && !validCoalTarget(target))
            throw new IllegalArgumentException("Use múltiplos de 8 entre 8 e 64.");
        Integer previous = PER_GOLEM.get(id);
        if (target == null) PER_GOLEM.remove(id);
        else PER_GOLEM.put(id, target);
        try { save(); }
        catch (IOException e) {
            if (previous == null) PER_GOLEM.remove(id);
            else PER_GOLEM.put(id, previous);
            throw e;
        }
    }

    public static void unload() {
        PER_GOLEM.clear();
        globalCoal = DEFAULT_COAL;
        path = null;
    }

    private static void save() throws IOException {
        if (path == null) throw new IOException("Mundo ainda não carregado.");
        Files.createDirectories(path.getParent());
        Properties data = new Properties();
        data.setProperty("coal.target", Integer.toString(globalCoal));
        PER_GOLEM.forEach((id, amount) ->
                data.setProperty("golem." + id, Integer.toString(amount)));
        Path temp = path.resolveSibling(FILE + ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) {
                data.store(out, "Smart Copper Golems Furnace Tender settings");
            }
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temp); }
    }
}
