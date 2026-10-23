# Editable art

- `outpost/`: Outpost tiles, scenery, Warden effects and the generated tent master.
- `forest/`: Forest tiles, scenery, enemies and Briarheart effects.
- `shared/`: shared palette and camp/brazier fire.
- `characters/`: unchanged generated masters for Mara and Rainoray's slime portrait.

Runtime PNGs stay in `assets/tilesets/`, `assets/props/`, `assets/characters/`
and `assets/effects/`. These editable projects are not packed into the game.

`tools/art/build-art.sh portraits` imports native 64 × 64 portraits and Mara's
80 × 80 body from the character masters. `tools/art/build-art.sh tent` imports the
128 × 100 tent with a sixteen-color palette. Both use hard transparency and keep
hand-edited runtime PNGs unless `--force` is explicitly supplied. See the
[tent record](../../docs/art-review/asset-cleanup/tent-art.md) and
[portrait record](../../docs/art-review/area-one-story/polish-art.md).

`tools/art/export-sources.sh` creates missing editable projects in the right group.
It keeps existing projects so teammates' layers are not overwritten. Export a
finished PNG back to its matching runtime path, keeping its dimensions and name.

Outpost floor touch-ups use the transparent
`outpost/outpost_ground_retouch.png` layer (3072×2048); the ground baker applies it
over each fresh map bake. See [the art guide](../../tools/art/README.md).
