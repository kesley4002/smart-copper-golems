package com.anantaya.smartcgolem.animation;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-owned golem stretch phases and target-dependent maximum reach. */
public final class StretchManager {
    private StretchManager() {}
    public static final int IDLE = 0, GROW = 1, HOLD = 2, SHRINK = 3;
    // Play the original 5.05-second Blockbench motion at 2x speed (about 2.55s).
    public static final int DURATION = 51;
    private record Session(int stage, int age, int maxProgress) {}
    private static final Map<UUID, Session> CURRENT = new HashMap<>();
    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(StretchPayload.TYPE, StretchPayload.CODEC);
    }
    public static void clear() { CURRENT.clear(); }
    public static void set(ServerLevel level, CopperGolem golem, int stage, int age, int maxProgress) {
        int capped = Math.max(0, Math.min(1000, maxProgress));
        Session previous = CURRENT.put(golem.getUUID(), new Session(stage, age, capped));
        if (previous != null && previous.stage == stage && previous.age == age && previous.maxProgress == capped) return;
        send(level, golem, stage, age, capped);
    }
    public static void stop(ServerLevel level, CopperGolem golem) {
        if (CURRENT.remove(golem.getUUID()) != null) send(level, golem, IDLE, 0, 0);
    }
    private static void send(ServerLevel level, CopperGolem golem, int stage, int age, int maxProgress) {
        StretchPayload payload = new StretchPayload(golem.getId(), stage, age, maxProgress);
        for (var player : level.players()) {
            if (player.distanceToSqr(golem) <= 128 * 128
                    && ServerPlayNetworking.canSend(player, StretchPayload.TYPE)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}
