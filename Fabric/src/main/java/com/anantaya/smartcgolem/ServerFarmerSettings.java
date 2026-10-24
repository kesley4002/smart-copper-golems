package com.anantaya.smartcgolem;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** World-local farming preferences. Composting is explicitly opt-in. */
public final class ServerFarmerSettings {
    private ServerFarmerSettings() {}
    private static final String FILE = "smart-copper-golem-farming.properties";
    private static final int DEFAULT_RESERVE = 32;
    private static boolean compostEnabled;
    private static int reserve = DEFAULT_RESERVE;
    private static Path file;

    public static boolean compostEnabled() { return compostEnabled; }
    public static int reserve() { return reserve; }

    public static void load(MinecraftServer server) {
        compostEnabled = false;
        reserve = DEFAULT_RESERVE;
        file = server.getWorldPath(LevelResource.ROOT).resolve(FILE);
        if (!Files.isRegularFile(file)) return;
        Properties data = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            data.load(in);
            compostEnabled = Boolean.parseBoolean(data.getProperty("compost.enabled", "false"));
            int next = Integer.parseInt(data.getProperty("seed.reserve", Integer.toString(DEFAULT_RESERVE)));
            if (next >= 0 && next <= 256) reserve = next;
        } catch (Exception e) {
            compostEnabled = false;
            reserve = DEFAULT_RESERVE;
            SmartCopperGolem.LOGGER.error("Cannot load farming settings; compost disabled for safety", e);
        }
    }

    public static void changeCompost(boolean enabled) throws IOException {
        boolean prev = compostEnabled;
        compostEnabled = enabled;
        try { save(); } catch (IOException e) { compostEnabled = prev; throw e; }
    }

    public static void changeReserve(int amount) throws IOException {
        if (amount < 0 || amount > 256) throw new IllegalArgumentException("Reserva válida: 0 a 256.");
        int prev = reserve;
        reserve = amount;
        try { save(); } catch (IOException e) { reserve = prev; throw e; }
    }

    public static void unload() {
        file = null;
        compostEnabled = false;
        reserve = DEFAULT_RESERVE;
    }

    private static void save() throws IOException {
        if (file == null) throw new IOException("Mundo ainda não foi carregado.");
        Files.createDirectories(file.getParent());
        Properties data = new Properties();
        data.setProperty("compost.enabled", Boolean.toString(compostEnabled));
        data.setProperty("seed.reserve", Integer.toString(reserve));
        Path temp = file.resolveSibling(FILE + ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) {
                data.store(out, "Smart Copper Golem: farmland compost settings");
            }
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temp); }
    }
}
