package com.anantaya.smartcgolem.animation;

import net.minecraft.world.entity.animal.golem.CopperGolem;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Optional local-only coordination with the Geyser-Fabric extension.
 * A phase transition updates a small CSV under the server's config folder.
 * No network listener and no client dependency is added.
 */
public final class StretchBridgeFile {
    private StretchBridgeFile() {}
    private static final Path PATH = Path.of("config", "smart-copper-golem", "geyser-stretch-states.csv");
    private record Phase(int stage, int reach, long startedMillis) {}
    private static final Map<UUID, Phase> ACTIVE = new LinkedHashMap<>();

    public static synchronized void transition(CopperGolem golem, int stage, int maxProgress) {
        Phase old = ACTIVE.get(golem.getUUID());
        if (stage == StretchManager.IDLE) {
            if (ACTIVE.remove(golem.getUUID()) == null) return;
        } else {
            if (old != null && old.stage == stage && old.reach == maxProgress) return;
            ACTIVE.put(golem.getUUID(), new Phase(stage, maxProgress, System.currentTimeMillis()));
        }
        flush();
    }
    public static synchronized void reset() {
        ACTIVE.clear();
        flush();
    }
    private static void flush() {
        try {
            Files.createDirectories(PATH.getParent());
            StringBuilder csv = new StringBuilder("# uuid,phase,maxProgress,startedEpochMs\n");
            for (var entry : ACTIVE.entrySet()) {
                Phase phase = entry.getValue();
                csv.append(entry.getKey()).append(',')
                        .append(phase.stage).append(',')
                        .append(phase.reach).append(',')
                        .append(phase.startedMillis).append('\n');
            }
            Path temp = PATH.resolveSibling(PATH.getFileName().toString() + ".tmp");
            Files.writeString(temp, csv.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temp, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temp, PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (java.io.IOException exception) {
            System.err.println("[Smart Copper Golem] Unable to update local Geyser bridge state: " + exception);
        }
    }
}
