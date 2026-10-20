package com.anantaya.smartcgolem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.storage.LevelResource;

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
 * Per-world (operator-controlled) work area, centered separately at each
 * golem's FIRST SEEN location. Existing golems receive their anchor when
 * first observed after installation. Entries persist across server restarts.
 *
 * Half-width is in blocks (250 => 500x500 nominal area). NO chunk tickets
 * are created by this setting or by the sorter.
 */
public final class ServerWorkZoneSettings {
    private ServerWorkZoneSettings() {}
    private static final String FILE = "smart-copper-golem-work-zones.properties";
    private static final int DEFAULT_WIDTH = 500;
    private static int width = DEFAULT_WIDTH;
    private static final Map<UUID, BlockPos> ANCHORS = new HashMap<>();
    private static Path worldFile;

    public static int width() { return width; }
    public static int halfWidth() { return width / 2; }

    public static boolean contains(BlockPos center, BlockPos pos) {
        long dx = (long)pos.getX() - center.getX();
        long dz = (long)pos.getZ() - center.getZ();
        return dx >= -halfWidth() && dx < halfWidth()
                && dz >= -halfWidth() && dz < halfWidth();
    }

    public static BlockPos anchor(CopperGolem golem) {
        UUID id = golem.getUUID();
        BlockPos existing = ANCHORS.get(id);
        if (existing != null) return existing;
        BlockPos created = golem.blockPosition().immutable();
        ANCHORS.put(id, created);
        persistSafely();
        return created;
    }

    public static void load(MinecraftServer server) {
        width = DEFAULT_WIDTH;
        ANCHORS.clear();
        worldFile = server.getWorldPath(LevelResource.ROOT).resolve(FILE);
        if (!Files.isRegularFile(worldFile)) return;
        Properties data = new Properties();
        try (InputStream in = Files.newInputStream(worldFile)) {
            data.load(in);
            int savedWidth = Integer.parseInt(data.getProperty("width", "500"));
            if (savedWidth >= 16 && savedWidth <= 1000 && savedWidth % 2 == 0) width = savedWidth;
            for (String key : data.stringPropertyNames()) {
                if (!key.startsWith("anchor.")) continue;
                try {
                    UUID id = UUID.fromString(key.substring("anchor.".length()));
                    String[] coords = data.getProperty(key).split(",");
                    if (coords.length != 3) continue;
                    ANCHORS.put(id, new BlockPos(Integer.parseInt(coords[0]),
                            Integer.parseInt(coords[1]), Integer.parseInt(coords[2])));
                } catch (IllegalArgumentException ignored) {
                    SmartCopperGolem.LOGGER.warn("Invalid golem work zone anchor {}", key);
                }
            }
        } catch (Exception e) {
            SmartCopperGolem.LOGGER.error("Could not read golem work zone settings", e);
        }
    }

    public static void setWidth(int newWidth) throws java.io.IOException {
        if (newWidth < 16 || newWidth > 1000 || newWidth % 2 != 0)
            throw new IllegalArgumentException("Work zone width must be even, 16..1000");
        int before = width;
        width = newWidth;
        try { save(); } catch (java.io.IOException e) { width = before; throw e; }
    }

    private static void persistSafely() {
        try { save(); }
        catch (java.io.IOException e) {
            SmartCopperGolem.LOGGER.error("Could not persist golem spawn anchor", e);
        }
    }

    public static void reset() {
        persistSafely();
        ANCHORS.clear();
        worldFile = null;
        width = DEFAULT_WIDTH;
    }

    private static void save() throws java.io.IOException {
        if (worldFile == null) return;
        Files.createDirectories(worldFile.getParent());
        Properties data = new Properties();
        data.setProperty("width", Integer.toString(width));
        ANCHORS.forEach((id, pos) -> data.setProperty("anchor." + id,
                pos.getX() + "," + pos.getY() + "," + pos.getZ()));
        Path tmp = worldFile.resolveSibling(FILE + ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(tmp)) {
                data.store(out, "Smart Copper Golems work zones: square side + per-golem anchors");
            }
            try {
                Files.move(tmp, worldFile, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, worldFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(tmp); }
    }
}
