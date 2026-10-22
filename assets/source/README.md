# Editable art

- `outpost/`: Outpost tiles, scenery and Warden effects.
- `forest/`: Forest tiles, scenery, enemies and Briarheart effects.
- `shared/`: shared palette and camp/brazier fire.

Runtime PNGs stay in `assets/tilesets/`, `assets/props/`, `assets/characters/`
and `assets/effects/`. These editable projects are not packed into the game.

`tools/art/export-sources.sh` creates missing editable projects in the right group.
It keeps existing projects so teammates' layers are not overwritten. Export a
finished PNG back to its matching runtime path, keeping its dimensions and name.

Outpost floor touch-ups use the transparent
`outpost/outpost_ground_retouch.png` layer (3072×2048); the ground baker applies it
over each fresh map bake. See [the art guide](../../tools/art/README.md).
