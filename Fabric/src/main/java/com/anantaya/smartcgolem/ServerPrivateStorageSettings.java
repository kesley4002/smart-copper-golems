package com.anantaya.smartcgolem;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/**
 * Optional per-Copper-Golem owner-only storage.
 *
 * DEFAULT: public un-named chests/barrels and a golem's own named storage.
 * PRIVATE_ONLY: only anvil-named storage owned by the same golem, even when
 * shared chests have matching frames. Other golems remain unaffected.
 *
 * The option persists by UUID and is intentionally not tied to profession.
 */
public final class ServerPrivateStorageSettings {
    private ServerPrivateStorageSettings() {}
    private static final String FILE = "smart-copper-golem-private-storage.properties";
    private static final Set<UUID> PRIVATE_ONLY = new HashSet<>();
    private static Path file;

    public static boolean isPrivateOnly(UUID id) {
        return PRIVATE_ONLY.contains(id);
    }

    /** Pure rule used by the ownership check and regression tests. */
    public static boolean allows(boolean privateOnly, boolean namedForThisGolem) {
        return !privateOnly || namedForThisGolem;
    }

    public static void load(MinecraftServer server) {
        PRIVATE_ONLY.clear();
        file = server.getWorldPath(LevelResource.ROOT).resolve(FILE);
        if (!Files.isRegularFile(file)) return;
        Properties data = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            data.load(input);
            for (String key : data.stringPropertyNames()) {
                if (!key.startsWith("exclusive.")) continue;
                try {
                    UUID id = UUID.fromString(key.substring("exclusive.".length()));
                    if (Boolean.parseBoolean(data.getProperty(key)))
                        PRIVATE_ONLY.add(id);
                } catch (IllegalArgumentException e) {
                    SmartCopperGolem.LOGGER.warn("Invalid private storage ID {}", key);
                }
            }
        } catch (IOException e) {
            SmartCopperGolem.LOGGER.error("Unable to load per-golem private storage settings", e);
        }
    }

    public static void setPrivateOnly(UUID id, boolean privateOnly) throws IOException {
        boolean old = PRIVATE_ONLY.contains(id);
        if (privateOnly) PRIVATE_ONLY.add(id);
        else PRIVATE_ONLY.remove(id);
        try { save(); }
        catch (IOException e) {
            if (old) PRIVATE_ONLY.add(id);
            else PRIVATE_ONLY.remove(id);
            throw e;
        }
    }

    public static void unload() {
        PRIVATE_ONLY.clear();
        file = null;
    }

    private static void save() throws IOException {
        if (file == null) throw new IOException("World not loaded");
        Files.createDirectories(file.getParent());
        Properties data = new Properties();
        for (UUID id : PRIVATE_ONLY) data.setProperty("exclusive." + id, "true");
        Path tmp = file.resolveSibling(FILE + ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(tmp)) {
                data.store(out, "Smart Copper Golems: private-only storage per golem UUID");
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
