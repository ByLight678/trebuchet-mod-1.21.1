package com.josep.trebuchet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FlightControlPayload(int ticks) implements CustomPacketPayload {
    public static final Type<FlightControlPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrebuchetMod.MOD_ID, "flight_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlightControlPayload> CODEC = new StreamCodec<>() {
        public FlightControlPayload decode(RegistryFriendlyByteBuf buffer) { return new FlightControlPayload(buffer.readVarInt()); }
        public void encode(RegistryFriendlyByteBuf buffer, FlightControlPayload payload) { buffer.writeVarInt(payload.ticks()); }
    };
    public Type<FlightControlPayload> type() { return TYPE; }
}
