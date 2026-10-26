# Editable art

- `outpost/`: Outpost tiles, scenery, Warden effects and generated tent/dummy masters.
- `forest/`: Forest tiles, scenery, enemies and Briarheart effects.
- `shared/`: shared palette and camp/brazier fire.
- `characters/`: generated masters for Mara and Rainoray's slime portrait, plus
  [Rainoray's editable animation timeline](characters/rainoray/README.md).

Runtime PNGs stay in `assets/tilesets/`, `assets/props/`, `assets/characters/`
and `assets/effects/`. These editable projects are not packed into the game.

`tools/art/build-art.sh portraits` imports native 64 × 64 portraits and Mara's
80 × 80 body from the character masters. `tools/art/build-art.sh tent` imports the
128 × 100 tent with a sixteen-color palette. `tools/art/build-art.sh dummy` prepares
the two 36 × 72 training-dummy variants, aligned to the existing ground anchor.
These importers use hard transparency and keep
hand-edited runtime PNGs unless `--force` is explicitly supplied. See the
[tent record](../../docs/art-review/asset-cleanup/tent-art.md),
[dummy record](../../docs/art-review/outpost-polish/dummy-art.md) and
[portrait record](../../docs/art-review/area-one-story/polish-art.md).

`tools/art/export-sources.sh` creates missing editable projects in the right group.
It keeps existing projects so teammates' layers are not overwritten. Export a
finished PNG back to its matching runtime path, keeping its dimensions and name.

Outpost floor touch-ups use the transparent
`outpost/outpost_ground_retouch.png` layer (3072×2048); the ground baker applies it
over each fresh map bake. See [the art guide](../../tools/art/README.md).

The current oak, bush, log and stump imports and editable Pixilart projects live in
[the groupmate forest source folder](forest/groupmate-2026-10-25/README.md). Older matching
Pixie Dust projects are preserved; use the integrated Pixilart versions for these four assets.

Forest character masters: `characters/forest/` contains the generated creature and Briarheart keyposes; `characters/npcs/sedge.generated.png` contains Sedge's two idle poses and portrait. Runtime imports use constrained native cells/palettes; exact prompts and modes are in `docs/art-review/area-two/`.

`audio/consistent-cast-2026-10-26/` preserves MP3 takes and per-request metadata for the100 new forest lines,82 replaced Rainoray lines and seven combat efforts. Transcription reports are included. Runtime WAVs and final hashes are in `assets/audio/dialogue/` and `docs/art-review/voice-auditions/runtime-voices.json`. No API credentials or voice-reference samples are included.

Current 2026-10-26 cast and opening: [Shuna, Kaijin, Eren, Rigurd and Souei atlas](characters/npcs/README.md), [road memory](ui/opening/README.md). These supersede the historical Mara/survivor/Sedge masters above. `audio/reincarnation-2026-10-26/` contains the 88 new or replacement voice sources and transcript reports.
