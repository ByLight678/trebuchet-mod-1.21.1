package com.josep.trebuchet.client;

import net.minecraft.client.model.geom.ModelPart;

/** Generated from the original launch clip by tools/export-animation.ps1. */
public final class TrebuchetLaunchAnimation {
    public static void apply(ModelPart model, float seconds) {
        if (seconds < 0 || seconds >= 3.2F) return;
        ModelPart root = model.getChild("rig_root");
        rotate(root, "throwing_arm", seconds, new float[] {0F, 0.2F, 0.35F, 0.5F, 0.65F, 0.8F, 0.95F, 1.2F, 1.6F, 2.2F, 2.8F, 3.2F}, new float[] {0F, 0F, 0.13962634F, 0.55850536F, 1.13446401F, 1.65806279F, 1.57079633F, 1.57079633F, 1.22173048F, 0.6981317F, 0.17453293F, 0F});
        rotate(root, "hanging_counterweight", seconds, new float[] {0F, 0.2F, 0.35F, 0.5F, 0.65F, 0.8F, 0.95F, 1.2F, 1.6F, 2.2F, 2.8F, 3.2F}, new float[] {0F, 0F, -0.13962634F, -0.55850536F, -1.13446401F, -1.65806279F, -1.57079633F, -1.57079633F, -1.22173048F, -0.6981317F, -0.17453293F, 0F});
        rotate(root, "sling", seconds, new float[] {0F, 0.2F, 0.35F, 0.5F, 0.65F, 0.8F, 0.95F, 1.2F, 1.6F, 2.2F, 2.8F, 3.2F}, new float[] {0F, 0F, 0F, -0.20943951F, -1.04719755F, -2.87979327F, -3.57792497F, -2.26892803F, -1.22173048F, -0.6981317F, -0.17453293F, 0F});
        rotate(root, "winch", seconds, new float[] {0F, 1.2F, 1.6F, 2.2F, 2.8F, 3.2F}, new float[] {0F, 0F, -1.25663706F, -3.14159265F, -5.02654825F, -6.28318531F});
    }

    private static void rotate(ModelPart root, String name, float seconds, float[] times, float[] values) {
        ModelPart part = switch (name) {
            case "hanging_counterweight", "sling" -> root.getChild("throwing_arm").getChild(name);
            default -> root.getChild(name);
        };
        for (int i = 1; i < times.length; i++) {
            if (seconds <= times[i]) {
                float alpha = Math.clamp((seconds - times[i - 1]) / (times[i] - times[i - 1]), 0F, 1F);
                part.xRot += values[i - 1] + alpha * (values[i] - values[i - 1]);
                return;
            }
        }
        part.xRot += values[values.length - 1];
    }
}
