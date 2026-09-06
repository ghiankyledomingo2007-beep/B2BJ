#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
pack_dir="$(mktemp -d /tmp/b2bj-pack.XXXXXX)"
javac --release 17 -d "$pack_dir" tools/PackFrames.java
# Reviewed native frames only. Padding/alignment never resamples source pixels.
pack_rows() {
  local output=$1 columns=$2 floor=$3 width=$4
  shift 4
  local frames=() row index
  for row in "$@"; do
    for ((index=0; index<columns; index++)); do
      frames+=("docs/art-review/rainoray-rework/$row/frame-$index.png")
    done
  done
  java -cp "$pack_dir" -Db2bj.maxFloorShift=16 PackFrames "$width" "$width" "$columns" "$floor" "$output" "${frames[@]}"
}
# Only completed, visually accepted sheets belong here; pending/rejected rows stay raw.
pack_rows assets/characters/blade/rainoray_idle.png 8 72 80 human-idle-south human-idle-east human-idle-north-retry
pack_rows assets/characters/blade/rainoray_run.png 8 72 80 human-run-south-retry human-run-east human-run-north-retry
pack_rows assets/characters/blade/rainoray_dash.png 8 72 80 human-dash-south-retry human-dash-east human-dash-north-retry
pack_rows assets/characters/blade/rainoray_cast.png 8 72 80 human-cast-south human-cast-east human-cast-north
pack_rows assets/characters/blade/rainoray_guard.png 8 72 80 human-guard-south-retry human-guard-east human-guard-north
pack_rows assets/characters/blade/rainoray_slash.png 8 72 80 human-slash-a-south-retry human-slash-a-east-polish human-slash-a-north-retry human-slash-b-south-polish human-slash-b-east human-slash-b-north-polish human-slash-c-south human-slash-c-east human-slash-c-north
pack_rows assets/characters/blade/rainoray_hurt.png 8 72 80 human-hurt-south human-hurt-east human-hurt-north
# Preserve the authored jump/splash trajectory; do not floor-lock airborne gel frames.
pack_rows assets/characters/slime/slime_engulf.png 16 -1 80 engulf-south engulf-west engulf-north
# Keep morph endpoints; omit frame14's detached ground spark before packing.
morph_frames=()
for row in south east north; do
  for index in {0..13} 15 16; do
    morph_frames+=("docs/art-review/rainoray-rework/transform-in-$row/frame-$index.png")
  done
done
java -cp "$pack_dir" -Db2bj.maxFloorShift=16 PackFrames 80 80 16 72 assets/effects/transform_in.png "${morph_frames[@]}"
cp docs/art-review/rainoray-rework/cart/frame-0.png assets/props/outpost/broken-cart.png
cp docs/art-review/rainoray-rework/shield-cache/frame-0.png assets/props/outpost/shield-cache.png
pack_rows assets/props/outpost/brazier.png 8 -1 64 brazier-animated
cp docs/art-review/rainoray-rework/rubble/frame-0.png assets/props/outpost/rubble.png
cp docs/art-review/rainoray-rework/crescent-icon/frame-0.png assets/ui/crescent.png
cp docs/art-review/rainoray-rework/portrait-mask/frame-0.png assets/ui/blade.png
pack_rows assets/effects/telegraph_ring.png 8 -1 128 ring-polish
pack_rows assets/effects/ichor_crescent.png 8 -1 64 crescent-animated
cp docs/art-review/rainoray-rework/riposte-icon/frame-0.png assets/ui/riposte.png
# Warning dim-to-red, then active flare; source frames arrived in reverse brightness order.
java -cp "$pack_dir" PackFrames 32 32 8 -1 assets/effects/fissure_tile.png docs/art-review/rainoray-rework/fissure-animated/frame-8.png docs/art-review/rainoray-rework/fissure-animated/frame-7.png docs/art-review/rainoray-rework/fissure-animated/frame-6.png docs/art-review/rainoray-rework/fissure-animated/frame-5.png docs/art-review/rainoray-rework/fissure-animated/frame-3.png docs/art-review/rainoray-rework/fissure-animated/frame-2.png docs/art-review/rainoray-rework/fissure-animated/frame-1.png docs/art-review/rainoray-rework/fissure-animated/frame-4.png
# Reversion keeps both exact endpoints and omits one redundant early held pose.
morph_frames=()
for row in south east north; do
  for index in 0 {2..16}; do
    morph_frames+=("docs/art-review/rainoray-rework/transform-out-$row/frame-$index.png")
  done
done
java -cp "$pack_dir" -Db2bj.maxFloorShift=16 PackFrames 80 80 16 72 assets/effects/transform_out.png "${morph_frames[@]}"
pack_rows assets/effects/aim_reticle.png 8 -1 32 reticle-animated
pack_rows assets/effects/telegraph_chevron.png 8 -1 32 chevron-polish
pack_rows assets/effects/riposte_counter.png 8 -1 80 counter-polish
pack_rows assets/effects/riposte_guard.png 8 -1 80 guard-polish
