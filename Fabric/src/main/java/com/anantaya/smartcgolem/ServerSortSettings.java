package com.anantaya.smartcgolem;

import com.anantaya.smartcgolem.util.SortLanguage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Properties;

/** One persistent language for the entire saved world, shared by all players. */
public final class ServerSortSettings {
    private ServerSortSettings() {}
    private static final String FILE = "smart-copper-golem-sort.properties";

    private static Path settingsPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(FILE);
    }

    public static void load(MinecraftServer server) {
        SortLanguage.select(SortLanguage.Mode.EN);
        Path path = settingsPath(server);
        if (!Files.isRegularFile(path)) return;
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
            String mode = properties.getProperty("language", "en")
                    .strip().toUpperCase(Locale.ROOT);
            if (!mode.equals("PT") && !mode.equals("EN")) {
                SmartCopperGolem.LOGGER.warn("Unknown saved sort language {}; using EN", mode);
                return;
            }
            SortLanguage.select(SortLanguage.Mode.valueOf(mode));
        } catch (Exception e) {
            SmartCopperGolem.LOGGER.error("Could not load world sorting language; using EN", e);
        }
        SmartCopperGolem.LOGGER.info("World container sort language: {}", SortLanguage.current());
    }

    /** Save first, then switch the live value. A failure never lies to the operator. */
    public static void change(MinecraftServer server, SortLanguage.Mode mode)
            throws java.io.IOException {
        Path path = settingsPath(server);
        Files.createDirectories(path.getParent());
        Properties properties = new Properties();
        properties.setProperty("language", mode.name().toLowerCase(Locale.ROOT));
        Path temp = path.resolveSibling(FILE + ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) {
                properties.store(out, "Smart Copper Golems: world-wide container sorting language");
            }
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
        SortLanguage.select(mode);
    }

    public static void reset() {
        SortLanguage.select(SortLanguage.Mode.EN);
    }
}
