package com.josep.trebuchet;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class TrebuchetConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue VERTICAL_VELOCITY;
    public static final ModConfigSpec.DoubleValue HORIZONTAL_VELOCITY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Initial launch speeds in metres per second (20 game ticks per second).",
                "On multiplayer servers, the server's values control every launch.",
                "Edit while Minecraft/the server is stopped, then restart.").push("launch");
        VERTICAL_VELOCITY = builder.comment("Upward launch speed in m/s. Default: 140. Range: 0 to 1000.")
                .defineInRange("verticalVelocity", 140.0, 0.0, 1000.0);
        HORIZONTAL_VELOCITY = builder.comment("Horizontal launch speed in m/s. Default: 260. Range: 0 to 1000.")
                .defineInRange("horizontalVelocity", 260.0, 0.0, 1000.0);
        builder.pop();
        SPEC = builder.build();
    }

    public static Vec3 launchVelocity(Direction facing) {
        double horizontal = HORIZONTAL_VELOCITY.get() / 20.0;
        return new Vec3(-facing.getStepX() * horizontal, VERTICAL_VELOCITY.get() / 20.0,
                -facing.getStepZ() * horizontal);
    }
}
