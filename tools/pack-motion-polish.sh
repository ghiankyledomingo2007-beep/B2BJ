#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
pack_dir=$(mktemp -d /tmp/b2bj-motion-pack.XXXXXX)
javac --release 17 -d "$pack_dir" tools/PackFrames.java
root=docs/art-review/motion-polish
frames=()
for direction in south east north; do
  source="sprint-cycle-$direction"
  if [ "$direction" = east ]; then source=sprint-locked-east; fi
  for i in {0..7}; do frames+=("$root/$source/frame-$i.png"); done
done
# Preserve authored airborne strides; aligning each pose to the floor erases running lift.
java -cp "$pack_dir" PackFrames 80 80 8 -1 assets/characters/blade/rainoray_run.png "${frames[@]}"
frames=()
for i in {0..15}; do frames+=("docs/art-review/rainoray-rework/engulf-south/frame-$i.png"); done
for phase in spread reform; do
  for i in {1..8}; do frames+=("$root/engulf-$phase/frame-$i.png"); done
done
for i in {0..15}; do frames+=("docs/art-review/rainoray-rework/engulf-north/frame-$i.png"); done
java -cp "$pack_dir" PackFrames 80 80 16 -1 assets/characters/slime/slime_engulf.png "${frames[@]}"
frames=()
for action in idle approach windup strike rise charge death; do
  first=1; last=8
  case "$action" in idle|approach|charge) first=0; last=7;; esac
  source="warden-$action"
  if [ "$action" = death ]; then source=warden-final-death; fi
  for ((i=first;i<=last;i++)); do frames+=("$root/$source/frame-$i.png"); done
done
# Reviewed prone endpoint ends at row52; four native pixels align it to the existing row56 ground.
java -cp "$pack_dir" -Db2bj.maxFloorShift=4 PackFrames 64 64 8 56 assets/characters/guardian/warden_motion.png "${frames[@]}"
for effect in tide-crest tide-release tide-break tide-foam warden-impact; do
  cell=64
  case "$effect" in tide-crest) cell=48;; tide-foam) cell=32;; esac
  source="$effect-animated"
  case "$effect" in tide-crest) source=tide-projectile-loop;; tide-break) source=tide-scatter;; esac
  frames=()
  for i in {1..8}; do frames+=("$root/$source/frame-$i.png"); done
  java -cp "$pack_dir" -Db2bj.allowEmptyTail=true PackFrames "$cell" "$cell" 8 -1 "assets/effects/${effect//-/_}.png" "${frames[@]}"
done
