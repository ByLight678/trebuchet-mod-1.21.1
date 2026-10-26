# Export the source clip's linear rotation offsets, preserving unwrapped winch rotations.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$model = Get-Content (Join-Path $root 'art/trebuchet/timber_trebuchet.bbmodel') -Raw | ConvertFrom-Json
$clip = $model.animations | Where-Object name -eq 'launch' | Select-Object -First 1
$lines = [Collections.Generic.List[string]]::new()
function Float([double]$value) { $value.ToString('0.########', [Globalization.CultureInfo]::InvariantCulture) + 'F' }
foreach ($property in $clip.animators.PSObject.Properties) {
    $bone = $property.Value
    $keys = @($bone.keyframes | Where-Object channel -eq 'rotation' | Sort-Object time)
    if ($keys.Count -eq 0) { continue }
    foreach ($key in $keys) {
        if ($key.interpolation -ne 'linear') { throw 'Unsupported interpolation in source clip' }
    }
    $times = ($keys | ForEach-Object { Float $_.time }) -join ', '
    $values = ($keys | ForEach-Object { Float (-[double]$_.data_points[0].x * [Math]::PI / 180) }) -join ', '
    $lines.Add(('        rotate(root, "{0}", seconds, new float[] {{{1}}}, new float[] {{{2}}});' -f $bone.name, $times, $values))
}
$header = @'
package com.josep.trebuchet.client;

import net.minecraft.client.model.geom.ModelPart;

/** Generated from the original launch clip by tools/export-animation.ps1. */
public final class TrebuchetLaunchAnimation {
    public static void apply(ModelPart model, float seconds) {
        if (seconds < 0 || seconds >= 3.2F) return;
        ModelPart root = model.getChild("rig_root");
'@
$footer = @'
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
'@
Set-Content (Join-Path $root 'src/main/java/com/josep/trebuchet/client/TrebuchetLaunchAnimation.java') ($header + "`n" + ($lines -join "`n") + "`n" + $footer)
