# Game assets

| Folder | Use |
| --- | --- |
| `characters/` | Current player, enemy and dialogue sprites |
| `props/outpost/`, `props/forest/` | Scenery grouped by region |
| `tilesets/outpost/`, `tilesets/forest/` | Current campaign terrain |
| `tilesets/legacy/` | Terrain still used by the older room renderer |
| `effects/`, `ui/`, `audio/` | Runtime effects, interface, fonts and sound |
| `source/` | Editable projects and generated masters; excluded from the game |

Current human Rainoray uses `characters/blade/rainoray_*.png`. The older `blade_*.png`
sprites and other confirmed unused art are preserved in
[the dated archive](../archive/assets/2026-10-23/README.md). The Lab 6 slime sheet and
legacy room terrain remain here because code still uses them.

For edits, use [the source guide](source/README.md) and keep runtime filenames,
dimensions and frame order. [The audit](../docs/art-review/asset-cleanup/README.md)
lists every current asset and the reason for each archived file.
