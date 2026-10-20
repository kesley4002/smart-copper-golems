package com.anantaya.smartcgolem;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Server-wide capacity for ONE carried ItemStack. Items with smaller
 * maximum stack sizes retain their normal Minecraft stack limit. */
public final class ServerCarrySettings {
    private ServerCarrySettings() {}
    private static final String FILE = "smart-copper-golem-carry.properties";
    private static int maximum = 64;
    private static Path path;

    public static int maximum() { return maximum; }
    public static void load(MinecraftServer server) {
        maximum = 64;
        path = server.getWorldPath(LevelResource.ROOT).resolve(FILE);
        if (!Files.isRegularFile(path)) return;
        Properties p = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            p.load(input);
            int saved = Integer.parseInt(p.getProperty("max", "64"));
            if (saved == 16 || saved == 32 || saved == 64) maximum = saved;
        } catch (Exception e) {
            SmartCopperGolem.LOGGER.error("Failed to load carrying capacity", e);
        }
    }
    public static void change(int next) throws IOException {
        if (next != 16 && next != 32 && next != 64)
            throw new IllegalArgumentException("Use somente 16, 32 ou 64.");
        if (path == null) throw new IOException("World is not loaded.");
        Files.createDirectories(path.getParent());
        Properties p = new Properties();
        p.setProperty("max", Integer.toString(next));
        try (OutputStream out = Files.newOutputStream(path)) { p.store(out, "Smart Copper Golems capacity"); }
        maximum = next;
    }
    public static void unload() { path = null; maximum = 64; }
}
