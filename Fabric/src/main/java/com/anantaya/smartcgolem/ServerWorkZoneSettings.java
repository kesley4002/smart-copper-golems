package com.anantaya.smartcgolem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
    private static final Map<UUID, Integer> INDIVIDUAL_WIDTHS = new HashMap<>();
    private static boolean nightWorkEnabled = false;
    private static Integer nightWidth = null;
    private static final Map<UUID, BlockPos> ANCHORS = new HashMap<>();
    private static Path worldFile;

    public static int width() { return width; }
    public static int halfWidth() { return width / 2; }
    public static boolean nightWorkEnabled() { return nightWorkEnabled; }
    public static int nightWidth() { return nightWidth == null ? width : nightWidth; }
    public static boolean hasNightWidthOverride() { return nightWidth != null; }
    public static Integer individualWidth(UUID id) { return INDIVIDUAL_WIDTHS.get(id); }

    public static boolean isNight(ServerLevel level) {
        long t = Math.floorMod(level.getOverworldClockTime(), 24000L);
        return t >= 13000L && t < 23000L;
    }

    public static int effectiveWidth(CopperGolem golem, ServerLevel level) {
        Integer personal = INDIVIDUAL_WIDTHS.get(golem.getUUID());
        if (isNight(level) && nightWorkEnabled && nightWidth != null) {
            return personal == null ? nightWidth : Math.min(personal, nightWidth);
        }
        return personal == null ? width : personal;
    }

    public static int halfWidth(CopperGolem golem, ServerLevel level) {
        return effectiveWidth(golem, level) / 2;
    }

    public static boolean contains(CopperGolem golem, ServerLevel level, BlockPos pos) {
        return containsSquare(anchor(golem), pos, halfWidth(golem, level));
    }

    private static boolean containsSquare(BlockPos center, BlockPos pos, int half) {
        long dx = (long)pos.getX() - center.getX();
        long dz = (long)pos.getZ() - center.getZ();
        return dx >= -half && dx < half && dz >= -half && dz < half;
    }

    public static boolean contains(BlockPos center, BlockPos pos) {
        return containsSquare(center, pos, halfWidth());
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
        nightWorkEnabled = false;
        nightWidth = null;
        INDIVIDUAL_WIDTHS.clear();
        ANCHORS.clear();
        worldFile = server.getWorldPath(LevelResource.ROOT).resolve(FILE);
        if (!Files.isRegularFile(worldFile)) return;
        Properties data = new Properties();
        try (InputStream in = Files.newInputStream(worldFile)) {
            data.load(in);
            int savedWidth = Integer.parseInt(data.getProperty("width", "500"));
            if (validWidth(savedWidth)) width = savedWidth;
            nightWorkEnabled = Boolean.parseBoolean(data.getProperty("night.enabled", "false"));
            String n = data.getProperty("night.width");
            if (n != null) {
                try {
                    int candidate = Integer.parseInt(n);
                    if (validWidth(candidate)) nightWidth = candidate;
                } catch (NumberFormatException ignored) {
                    SmartCopperGolem.LOGGER.warn("Invalid night work width");
                }
            }
            for (String key : data.stringPropertyNames()) {
                if (!key.startsWith("radius.")) continue;
                try {
                    UUID id = UUID.fromString(key.substring("radius.".length()));
                    int value = Integer.parseInt(data.getProperty(key));
                    if (validWidth(value)) INDIVIDUAL_WIDTHS.put(id, value);
                } catch (IllegalArgumentException ignored) {
                    SmartCopperGolem.LOGGER.warn("Invalid per-golem width {}", key);
                }
            }
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

    private static boolean validWidth(int value) {
        return value >= 16 && value <= 1000 && value % 2 == 0;
    }

    public static void setIndividualWidth(UUID id, Integer newWidth) throws java.io.IOException {
        if (newWidth != null && !validWidth(newWidth))
            throw new IllegalArgumentException("Use largura par entre 16 e 1000.");
        Integer previous = INDIVIDUAL_WIDTHS.get(id);
        if (newWidth == null) INDIVIDUAL_WIDTHS.remove(id);
        else INDIVIDUAL_WIDTHS.put(id, newWidth);
        try { save(); }
        catch (java.io.IOException e) {
            if (previous == null) INDIVIDUAL_WIDTHS.remove(id);
            else INDIVIDUAL_WIDTHS.put(id, previous);
            throw e;
        }
    }

    public static void setNightWork(boolean enabled) throws java.io.IOException {
        boolean previous = nightWorkEnabled;
        nightWorkEnabled = enabled;
        try { save(); }
        catch (java.io.IOException e) { nightWorkEnabled = previous; throw e; }
    }

    public static void setNightWidth(int newWidth) throws java.io.IOException {
        if (!validWidth(newWidth))
            throw new IllegalArgumentException("Use largura par entre 16 e 1000.");
        Integer previous = nightWidth;
        nightWidth = newWidth;
        try { save(); }
        catch (java.io.IOException e) { nightWidth = previous; throw e; }
    }

    public static void resetNightWidth() throws java.io.IOException {
        Integer previous = nightWidth;
        nightWidth = null;
        try { save(); }
        catch (java.io.IOException e) { nightWidth = previous; throw e; }
    }

    public static void setWidth(int newWidth) throws java.io.IOException {
        if (!validWidth(newWidth))
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
        INDIVIDUAL_WIDTHS.clear();
        nightWorkEnabled = false;
        nightWidth = null;
        worldFile = null;
        width = DEFAULT_WIDTH;
    }

    private static void save() throws java.io.IOException {
        if (worldFile == null) return;
        Files.createDirectories(worldFile.getParent());
        Properties data = new Properties();
        data.setProperty("width", Integer.toString(width));
        data.setProperty("night.enabled", Boolean.toString(nightWorkEnabled));
        if (nightWidth != null) data.setProperty("night.width", Integer.toString(nightWidth));
        INDIVIDUAL_WIDTHS.forEach((id, w) -> data.setProperty("radius." + id, Integer.toString(w)));
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
