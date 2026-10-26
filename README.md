# Trebuchet

Minecraft **1.21.1**, NeoForge **21.1.264**, Java **21**.
Based on the official NeoForge ModDevGradle MDK, using ModDevGradle 2.0.148 and Gradle 9.2.1.

Mod ID: `trebuchet`

Version: `0.1.0`

DISCLAIMER: The ideas of this mod are my own, but implementation and models for this project contain AI generated content.


## Timber Trebuchet block

Find **Timber Trebuchet** under Creative's **Functional Blocks** tab, or run:

```mcfunction
/give @s trebuchet:trebuchet
```

Place it on a level, solid **3-block-wide by 4-block-long** area. The clicked cell
is the middle of the rear row; the base extends three more cells in the direction
you face and one cell to either side. All twelve cells must be replaceable, within
the world border, supported from below, and clear of entities. A failed placement
does not consume the item.

Craft one trebuchet in a crafting table using this pattern:

| | | |
|---|---|---|
| Any log | Leather | Any log |
| Any log | String | Any log |
| Any log | Stone Bricks | Any log |

Logs use the `minecraft:logs` item tag, so wood types can be mixed, including
stripped logs and Nether stems. The recipe book unlocks when you obtain a log.

The collision and selection footprint is exactly **3 wide × 4 long × 1 high**.
The renderer uses the original model dimensions (16 model units per block),
including the loaded pose's approximately 3.04-block height. Geometry above the
base is intentionally non-colliding and cannot be targeted by itself. No empty
blocks are reserved above the base, so other blocks can overlap the upper model.

Breaking any of the twelve base cells removes the entire structure and drops
one trebuchet in Survival. Creative removal does not drop an item. The base cannot
be pushed by pistons. One block entity renders the whole model; the other eleven
cells provide collision and selection only. The inventory and held item use the
same textured model with their own display scale.

Right-click any base cell to play the original **3.2-second launch animation**.
Empty hands and held items both work during normal right-click interaction.
Further clicks during playback are ignored; after the winch reset finishes,
the trebuchet can be launched again. Playback is synchronized to nearby players
and includes the arm, hanging counterweight, sling, and winch. The hitbox stays
3 × 4 × 1 throughout the animation.

Entities standing on the **middle base cell at the opposite end from the placement
anchor** (three cells along the base from the originally clicked placement cell)
launch immediately when a right-click starts the animation. This includes
players, mobs, dropped items, and primed TNT. The initial velocity replaces their
current motion with **140 m/s upward and 260 m/s horizontally**, opposite the block's
stored placement facing: 7 blocks/tick upward and 13 blocks/tick horizontally at
20 ticks/second. All qualifying entities
launch together; entities on adjacent base cells or above the pad are unaffected.
Selection uses the entity's horizontal centre and feet within 0.15 blocks below to
0.25 blocks above the base's top. Passengers dismount before launching. TNT retains
its fuse, and normal gravity, drag, collision, and fall damage still apply.

A custom velocity payload avoids vanilla motion packets' per-axis speed clamp.
High-speed server-controlled entities receive full-speed tracking updates while
their motion exceeds that limit; players receive the full initial impulse and
then simulate normal movement locally.

To regenerate the Java model after editing the source artwork:

```powershell
.\tools\export-model.ps1
.\tools\export-animation.ps1
.\dev.ps1 build
```

The exporter retains all 37 cubes, their sizes, pivots, group hierarchy,
rest rotations, and box UV offsets. The source atlas is copied unchanged to
`src/main/resources/assets/trebuchet/textures/block/trebuchet.png`.

The five GameTests exercise placement in all four directions, collision and
empty space above every base cell, one-item consumption, breaking each of the
twelve cells with exactly one drop per structure, and rejected obstructed or
unsupported placement without partial structures or item consumption. They also
cover right-click animation activation and replay, synchronized playback state,
and the launch pad's position, direction, speeds, entity selection, and network payload.

## Upstream references

- https://docs.neoforged.net/docs/1.21.1/gettingstarted/
- https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle

The upstream template license is retained in `TEMPLATE_LICENSE.txt`.
The project is licensed under the MIT License; see `LICENSE`.
