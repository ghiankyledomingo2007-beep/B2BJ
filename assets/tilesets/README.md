# Tilesets by area

| Group | Files | Used for |
| --- | --- | --- |
| [Outpost](outpost/) | `outpost_tileset.png`, `outpost_ground.png`, `outpost_ground.layout`, `outpost_decals.png` | Ruined Outpost campaign |
| [Forest](forest/) | `forest_tileset.png`, `forest_ground.png`, `forest_ground.layout` | Corrupted Forest campaign |
| [Legacy room](legacy/ruined_outpost/) | `ruined_outpost_wang.png` | Older room mode; kept separate from campaign art |

The current region sheets use 16×16 tiles. Both contain eight ground materials,
then their own transition tiles. Shared materials such as grass and dirt stay in
each sheet so either area can be edited on its own. There is no separate shared
tile image yet.

- **Outpost:** grass, meadow, dirt, flagstone, cobble, water, mud, gravel.
- **Forest:** leaf litter, moss, grass, dirt, blackwater, dry riverbed, blight, mud.

`*_tileset.png` is the reusable tile sheet; `*_ground.png` is the full map floor.
The game loads the full floors. Forest baking reads its tiles from `Tiles16`;
Outpost baking draws its materials through `GroundBake`.

Editable projects live in [source/outpost](../source/outpost/) and
[source/forest](../source/forest/). The palette and reusable fire sources live in
[source/shared](../source/shared/). Keep original filenames and dimensions when
returning polished art. See [the art guide](../../tools/art/README.md) for rebaking
and the tile row layout.

This is file organization only; it does not add regions or change map layouts.
