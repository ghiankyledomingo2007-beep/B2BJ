# Groupmate forest art — integrated 2026-10-25

Artwork supplied by the user's groupmate through [this shared folder](https://drive.google.com/drive/folders/1Ul5NaAXPOwmk0Dbxt3CP5NCtGssZS_7r). Individual artist name has not been supplied. Original `.pixil` projects are preserved unchanged. `oak.png`, `bush.png`, `log.png`, and `stump.png` are lossless first-frame exports from those projects; `previous_*.png` preserve the replaced runtime artwork.

**Edit `set_*.integrated.pixil` for the current game versions.** They contain the exact shipped PNG pixels and one frame each. The supplied `woods.pixil` has a completely transparent second frame; it is excluded from the integrated version. Existing older `../set_*.pixie-dust` files are preserved historical sources, not the current versions of these four assets.

The groupmate's shapes, bark cracks, roots, berries and grass are retained. `ImportedArt.forest` maps colors to the game's existing vegetation/wood/earth/crimson/bone/dry ramps; green-dominant colors use the grass ramp so vivid lime cannot become white highlights. Trees (348×162), bushes (156×34), and logs (208×28) retain their native sheet sizes and three/three/two variants. The stump is fitted from64×64 to48×48 with nearest-neighbor sampling, anchor(24,42), solid footprint64×26 world pixels. Forest placement and baked shadow use those shared metrics.

Regenerate the four exports with `bash tools/art/build-art.sh forest-props`. `props`/`all` also use this importer, so a forced generator run does not restore the previous procedural art. Runtime hand-edits remain protected by `generated.sha`; `forest-props --force` deliberately replaces only these four runtime PNGs. If changing stump metrics, also run `bash tools/art/build-art.sh forest` and the game build.

For direct pixel edits, export the matching integrated project over `assets/props/forest/set_<name>.png` at the current dimensions. Do not overwrite the64×64 raw stump master with its48×48 runtime export. Editable source folders are excluded from the playable JAR.

See [review and actual game renders](../../../../docs/art-review/groupmate-environment-2026-10-25/README.md).
