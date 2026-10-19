#!/usr/bin/env bash
# Saves every generated Outpost sheet as an editable Pixie Dust file with the Outpost palette loaded,
# under assets/source/outpost/. Edit there, export PNG over the asset; the art build keeps hand edits.
# Floor retouches go in assets/source/outpost/outpost_ground_retouch.png (painted over each bake).
set -eu
cd "$(dirname "$0")/../.."
editor="${PIXIE_DUST:-$HOME/.local/bin/pixie-dust}"
palette=assets/source/outpost/outpost_night.gpl
out=assets/source/outpost
mkdir -p "$out"
for png in assets/props/outpost/ruin_wall.png assets/props/outpost/stake_wall.png assets/props/outpost/supplies.png \
           assets/props/outpost/set_*.png assets/props/forest/set_*.png assets/tilesets/outpost/outpost_*.png \
           assets/tilesets/forest/forest_tileset.png assets/effects/fire_*.png assets/effects/warden_*.png \
           assets/effects/briar_*.png assets/characters/forest/*.png assets/characters/guardian/briarheart_motion.png; do
    name="$(basename "${png%.png}")"
    "$editor" -b "$png" --palette "$palette" --save-as "$out/$name.pixie-dust" >/dev/null
done
ls "$out" | wc -l
