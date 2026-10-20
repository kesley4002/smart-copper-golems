package com.anantaya.smartcgolem.animation;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-owned, per-entity stretch animation stage. No extra mods required. */
public final class StretchManager {
    private StretchManager() {}
    public static final int IDLE = 0, GROW = 1, HOLD = 2, SHRINK = 3;
    public static final int DURATION = 101; // uploaded JSON animation_length=5.05s
    private record Session(int stage, int age) {}
    private static final Map<UUID, Session> CURRENT = new HashMap<>();

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(StretchPayload.TYPE, StretchPayload.CODEC);
    }
    public static void clear() { CURRENT.clear(); }

    public static void set(ServerLevel level, CopperGolem golem, int stage, int age) {
        Session previous = CURRENT.put(golem.getUUID(), new Session(stage, age));
        if (previous != null && previous.stage == stage && previous.age == age) return;
        send(level, golem, stage, age);
    }
    public static void stop(ServerLevel level, CopperGolem golem) {
        if (CURRENT.remove(golem.getUUID()) != null) send(level, golem, IDLE, 0);
    }
    private static void send(ServerLevel level, CopperGolem golem, int stage, int age) {
        StretchPayload payload = new StretchPayload(golem.getId(), stage, age);
        // Nearby clients are refreshed frequently, including joiners who
        // entered visibility after the stage began.
        for (var player : level.players()) {
            if (player.distanceToSqr(golem) <= 128 * 128
                    && ServerPlayNetworking.canSend(player, StretchPayload.TYPE)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}
