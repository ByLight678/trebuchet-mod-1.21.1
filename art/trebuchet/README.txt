TIMBER TREBUCHET - BASE MODEL
Target: Minecraft Java 1.21.1 / NeoForge; planned custom block entity renderer.
Preference: balanced appearance/performance. Expected use: a few siege engines at once on desktop.
Footprint: X -24..24, Z -32..32 = 48 x 64 units = 3 x 4 blocks at 16 units per block. Origin: center of ground footprint.
Loaded-pose height: approximately 48.575 units / 3.036 blocks.
Budget: <=45 cubes / 540 triangles. Actual: 37 cubes / 222 quads / 444 triangles, six groups.
Texture: trebuchet.png, 256x256, single opaque color atlas, no PBR or emissive pass. Density: one pixel per model unit; matching repeated beam sizes intentionally share box-UV regions.
Details: painted iron bands, bolts, timber grain, stone masonry, rope windings and leather.
RIG: rig_root > timber_frame; throwing_arm; winch. throwing_arm > hanging_counterweight; sling.
Throwing-arm pivot: [0,40,-4], rotates around X. Rest rotation: +25 degrees X. Counterweight pivot [0,40,-18] and sling pivot [0,40,28] are children with -25 degree X rest rotations to hang vertically. Later animation can counter-rotate them as the arm swings, and animate the sling/release separately. Winch pivot [0,14,23].
Animation: launch, 3.2 seconds, play once. Loaded hold 0-0.2s; firing 0.2-0.8s; follow-through to 1.2s; winch reset 1.2-3.2s. Suggested projectile release marker: 0.8s. Arm, hanging counterweight, sling and winch rotation tracks, 42 keyframes total. Embedded in the .bbmodel; runtime playback and projectile spawning must be connected in mod code. Sling geometry is a rigid placeholder for later simple animation, not a rope simulation.
Model saved in Modded Entity format to preserve articulated parts for a future Java block entity renderer; this does not register an entity or block in the mod.
Collision, multiblock placement/removal, interaction, projectile physics and runtime animation must be implemented separately. Footprint is measured in the resting pose; animation clearance may extend beyond it.
Validation: front/rear previews; mapped checker; measured world bounds and 1:1 UV density; embedded texture and saved-file inspection. Not tested in Minecraft.
