#!/usr/bin/env bash
# Saves generated sheets as editable Pixie Dust files in their region's source folder.
# Edit there, export PNG over the asset; the art build keeps hand edits.
# Floor retouches go in assets/source/outpost/outpost_ground_retouch.png (painted over each bake).
set -eu
cd "$(dirname "$0")/../.."
editor="${PIXIE_DUST:-$HOME/.local/bin/pixie-dust}"
palette=assets/source/shared/outpost_night.gpl
count=0
for png in assets/props/outpost/ruin_wall.png assets/props/outpost/stake_wall.png assets/props/outpost/supplies.png \
           assets/props/outpost/set_*.png assets/props/forest/set_*.png assets/tilesets/outpost/outpost_*.png \
           assets/tilesets/forest/forest_tileset.png assets/effects/fire_*.png assets/effects/warden_*.png \
           assets/effects/briar_*.png assets/characters/forest/*.png assets/characters/guardian/briarheart_motion.png; do
    name="$(basename "${png%.png}")"
    case "$png" in
        assets/props/forest/*|assets/tilesets/forest/*|assets/characters/forest/*|assets/effects/briar_*|assets/characters/guardian/briarheart_*) area=forest ;;
        assets/effects/fire_*) area=shared ;;
        *) area=outpost ;;
    esac
    out="assets/source/$area"
    mkdir -p "$out"
    # Existing editable projects may contain teammate layers that are not in the PNG.
    if [ -e "$out/$name.pixie-dust" ]; then continue; fi
    "$editor" -b "$png" --palette "$palette" --save-as "$out/$name.pixie-dust" >/dev/null
    count=$((count + 1))
done
printf 'Created %s editable sources; existing projects kept.\n' "$count"
