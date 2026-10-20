package com.anantaya.smartcgolem.animation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StretchPayload(int entityId, int stage, int stageTicks, int maxProgress) implements CustomPacketPayload {
    public static final Type<StretchPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("smart-copper-golem", "stretch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StretchPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, StretchPayload::entityId,
                    ByteBufCodecs.VAR_INT, StretchPayload::stage,
                    ByteBufCodecs.VAR_INT, StretchPayload::stageTicks,
                    ByteBufCodecs.VAR_INT, StretchPayload::maxProgress,
                    StretchPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
