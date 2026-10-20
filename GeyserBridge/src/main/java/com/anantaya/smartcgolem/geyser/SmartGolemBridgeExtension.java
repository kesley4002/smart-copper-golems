package com.anantaya.smartcgolem.geyser;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.entity.property.GeyserFloatEntityProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Bedrock-only render bridge: the Fabric mod emits local state transitions,
 * and this Geyser extension maps them to Molang-visible entity property.
 */
public final class SmartGolemBridgeExtension implements Extension {
    private static final Path STATE_FILE = Path.of("config", "smart-copper-golem", "geyser-stretch-states.csv");
    private static final long ANIMATION_MILLIS = 2550L; // 51 ticks at nominal 20 TPS
    private GeyserFloatEntityProperty reachProperty;
    private ScheduledExecutorService ticker;
    private final Set<UUID> previousActive = new HashSet<>();

    @Subscribe
    public void registerProperties(GeyserDefineEntityPropertiesEvent event) {
        reachProperty = event.registerFloatProperty(
                Identifier.of("minecraft:copper_golem"),
                Identifier.of("scg:reach"), 0f, 1f, 0f);
    }

    @Subscribe
    public void start(GeyserPostInitializeEvent event) {
        ticker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread worker = new Thread(r, "scg-geyser-bridge");
            worker.setDaemon(true);
            return worker;
        });
        ticker.scheduleAtFixedRate(this::tickSafely, 200L, 100L, TimeUnit.MILLISECONDS);
        logger().info("Copper Golem reach animation bridge enabled; reading " + STATE_FILE);
    }

    @Subscribe
    public void shutdown(GeyserShutdownEvent event) {
        if (ticker != null) ticker.shutdownNow();
        previousActive.clear();
    }

    private void tickSafely() {
        try { tick(); }
        catch (Exception exception) {
            logger().warning("Unable to update Copper Golem reach property: " + exception.getMessage());
        }
    }

    private void tick() throws Exception {
        if (reachProperty == null) return;
        Map<UUID, Float> now = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        if (Files.isRegularFile(STATE_FILE)) {
            for (String line : Files.readAllLines(STATE_FILE)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] cols = line.split(",");
                if (cols.length != 4) continue;
                UUID id = UUID.fromString(cols[0]);
                int phase = Integer.parseInt(cols[1]);
                float max = Math.max(0f, Math.min(1f, Integer.parseInt(cols[2]) / 1000f));
                long started = Long.parseLong(cols[3]);
                long elapsed = Math.max(0, timestamp - started);
                float fraction = Math.max(0f, Math.min(1f, elapsed / (float) ANIMATION_MILLIS));
                float progress = switch (phase) {
                    case 1 -> fraction;
                    case 2 -> 1f;
                    case 3 -> 1f - fraction;
                    default -> 0f;
                };
                // Expire stale states after the server stops uncleanly.
                if (timestamp - started > 30000L) continue;
                now.put(id, progress * max);
            }
        }
        for (var connection : GeyserApi.api().onlineConnections()) {
            for (var entry : now.entrySet()) {
                var entity = connection.entities().byUuid(entry.getKey());
                if (entity != null) entity.updateProperty(reachProperty, entry.getValue());
            }
            for (UUID old : previousActive) {
                if (now.containsKey(old)) continue;
                var entity = connection.entities().byUuid(old);
                if (entity != null) entity.updateProperty(reachProperty, 0f);
            }
        }
        previousActive.clear();
        previousActive.addAll(now.keySet());
    }
}
