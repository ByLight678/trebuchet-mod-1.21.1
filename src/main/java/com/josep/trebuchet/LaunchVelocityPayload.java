package com.josep.trebuchet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Doubles preserve launch speeds beyond vanilla's 3.9-block/tick motion packet limit. */
public record LaunchVelocityPayload(int entityId, Vec3 velocity) implements CustomPacketPayload {
    public static final Type<LaunchVelocityPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrebuchetMod.MOD_ID, "launch_velocity"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LaunchVelocityPayload> CODEC = new StreamCodec<>() {
        @Override
        public LaunchVelocityPayload decode(RegistryFriendlyByteBuf buffer) {
            return new LaunchVelocityPayload(buffer.readVarInt(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, LaunchVelocityPayload payload) {
            buffer.writeVarInt(payload.entityId());
            buffer.writeDouble(payload.velocity().x);
            buffer.writeDouble(payload.velocity().y);
            buffer.writeDouble(payload.velocity().z);
        }
    };

    @Override
    public Type<LaunchVelocityPayload> type() { return TYPE; }

    public static void handle(LaunchVelocityPayload payload, IPayloadContext context) {
        Entity entity = context.player().level().getEntity(payload.entityId());
        if (entity != null) {
            entity.setDeltaMovement(payload.velocity());
            entity.setOnGround(false);
            entity.fallDistance = 0;
        }
    }
}
