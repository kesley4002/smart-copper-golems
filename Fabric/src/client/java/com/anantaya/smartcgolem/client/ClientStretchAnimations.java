package com.anantaya.smartcgolem.client;

import com.anantaya.smartcgolem.animation.StretchManager;
import com.anantaya.smartcgolem.animation.StretchPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

/** Client-owned visual states. The server always controls the golem movement. */
public final class ClientStretchAnimations implements ClientModInitializer {
    public record Pose(int stage, int age, long receivedAt) {}
    private static final Map<Integer, Pose> STATES = new HashMap<>();
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(StretchPayload.TYPE, (packet, ctx) -> {
            Minecraft client = ctx.client();
            client.execute(() -> {
                if (packet.stage() == StretchManager.IDLE) STATES.remove(packet.entityId());
                else STATES.put(packet.entityId(),
                        new Pose(packet.stage(), packet.stageTicks(),
                                client.level == null ? 0 : client.level.getGameTime()));
            });
        });
    }
    public static float progress(int entityId, float partialTick) {
        Pose p = STATES.get(entityId);
        Minecraft client = Minecraft.getInstance();
        if (p == null || client.level == null) return 0;
        float elapsed = p.age() + (client.level.getGameTime() - p.receivedAt()) + partialTick;
        float normalized = Math.max(0f, Math.min(1f, elapsed / StretchManager.DURATION));
        return switch (p.stage()) {
            case StretchManager.GROW -> normalized;
            case StretchManager.HOLD -> 1f;
            case StretchManager.SHRINK -> 1f - normalized;
            default -> 0f;
        };
    }
}
