package com.josep.trebuchet;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = TrebuchetMod.MOD_ID)
public final class LaunchMotionSync {
    private static final Set<Entity> TRACKED = Collections.newSetFromMap(new WeakHashMap<>());

    public static void launch(Entity entity, Vec3 velocity) {
        entity.stopRiding();
        entity.setDeltaMovement(velocity);
        entity.setOnGround(false);
        entity.fallDistance = 0;
        entity.hasImpulse = true;
        // A vanilla hurt-motion packet would overwrite the player's full-speed impulse with 3.9.
        entity.hurtMarked = false;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new LaunchVelocityPayload(entity.getId(), velocity));
        TRACKED.add(entity);
    }

    @SubscribeEvent
    public static void afterServerTick(ServerTickEvent.Post event) {
        TRACKED.removeIf(entity -> {
            if (entity.isRemoved() || !entity.isAlive()) return true;
            Vec3 velocity = entity.getDeltaMovement();
            if (Math.max(Math.abs(velocity.x), Math.max(Math.abs(velocity.y), Math.abs(velocity.z))) <= 3.9) return true;
            LaunchVelocityPayload payload = new LaunchVelocityPayload(entity.getId(), velocity);
            // Players simulate their own gravity/drag. Do not reapply their initial server impulse to them.
            // Other viewers and server-controlled entities need an uncapped update after vanilla tracking.
            if (entity instanceof ServerPlayer) {
                PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
                return true;
            } else {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
            }
            return false;
        });
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) { TRACKED.clear(); }
}
