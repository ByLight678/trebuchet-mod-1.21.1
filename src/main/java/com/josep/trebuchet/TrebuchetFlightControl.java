package com.josep.trebuchet;

import java.util.Map;
import java.util.Collections;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Temporary player-only drag adjustment. Gravity, input, and collision remain vanilla. */
@EventBusSubscriber(modid = TrebuchetMod.MOD_ID)
public final class TrebuchetFlightControl {
    private static final Map<Player, Flight> FLIGHTS = Collections.synchronizedMap(new WeakHashMap<>());
    public static final int MAX_TICKS = 1200;

    private static final class Flight {
        final Level level;
        int ticks;
        Vec3 position;
        double speedBeforeTick;
        Flight(Player player, int ticks) {
            this.level = player.level();
            this.ticks = ticks;
            this.position = player.position();
        }
    }

    // One second per initial block/tick of total launch speed; capped at one minute.
    public static int durationTicks(Vec3 velocity) {
        double speed = velocity.length();
        return !Double.isFinite(speed) || speed <= 0 ? 0 : (int)Math.min(MAX_TICKS, Math.ceil(speed * 20));
    }

    public static void begin(Player player, int ticks) {
        if (ticks <= 0) FLIGHTS.remove(player);
        else FLIGHTS.put(player, new Flight(player, Math.min(ticks, MAX_TICKS)));
    }

    static boolean isActive(Player player) { return FLIGHTS.containsKey(player); }

    public static void launch(Player player, Vec3 velocity) {
        int ticks = durationTicks(velocity);
        begin(player, ticks);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new FlightControlPayload(ticks));
        }
    }

    private static void stop(Player player) {
        if (FLIGHTS.remove(player) != null && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new FlightControlPayload(0));
        }
    }

    private static boolean interrupted(Player player, Flight flight) {
        double allowedStep = Math.max(8, player.getDeltaMovement().length() * 2 + 2);
        return flight.ticks <= 0 || !player.isAlive() || player.isRemoved() || player.level() != flight.level
                || player.isFallFlying() || player.getAbilities().flying || player.isSpectator()
                || player.onGround() || player.isInWaterOrBubble() || player.isInLava() || player.isPassenger()
                || player.horizontalCollision || player.verticalCollision
                || player.position().distanceToSqr(flight.position) > allowedStep * allowedStep;
    }

    @SubscribeEvent
    public static void beforeTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        Flight flight = FLIGHTS.get(player);
        if (flight == null) return;
        if (interrupted(player, flight)) { stop(player); return; }
        flight.speedBeforeTick = player.getDeltaMovement().horizontalDistance();
    }

    public static Vec3 compensatedMotion(Vec3 motion, double previousHorizontalSpeed) {
        double speed = motion.horizontalDistance();
        if (speed == 0) return motion;
        // Replace 0.91 horizontal retention with 0.98 without increasing pre-tick speed.
        double multiplier = Math.min(0.98 / 0.91, previousHorizontalSpeed / speed);
        return new Vec3(motion.x * multiplier, motion.y, motion.z * multiplier);
    }

    @SubscribeEvent
    public static void afterTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Flight flight = FLIGHTS.get(player);
        if (flight == null) return;
        if (interrupted(player, flight)) { stop(player); return; }
        if (player.level().isClientSide && player.isLocalPlayer()) {
            player.setDeltaMovement(compensatedMotion(player.getDeltaMovement(), flight.speedBeforeTick));
        }
        flight.position = player.position();
        if (--flight.ticks <= 0) stop(player);
    }

    @SubscribeEvent
    public static void teleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof Player player) stop(player);
    }

    @SubscribeEvent
    public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) { stop(event.getEntity()); }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { FLIGHTS.remove(event.getEntity()); }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { FLIGHTS.clear(); }
}
