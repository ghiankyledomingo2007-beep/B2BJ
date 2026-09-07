#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
pack_dir=$(mktemp -d /tmp/b2bj-rain-pack.XXXXXX)
javac --release 17 -d "$pack_dir" tools/PackFrames.java
root=docs/art-review/rain-weather
# Native translation only: both falling sprites end at the rendered contact point.
java -Djava.awt.headless=true -Db2bj.maxFloorShift=16 -cp "$pack_dir" PackFrames 32 32 2 31 assets/effects/rain_streaks.png \
  "$root/streak-fine-native/frame-0.png" "$root/streak-near-native/frame-0.png"
frames=()
# Reject opaque splash frames4/6 and the ripple's late reformation; keep six distinct poses each.
for i in 0 1 2 3 5 8; do frames+=("$root/splash-animated/frame-$i.png"); done
for i in 1 2 3 4 5 6; do frames+=("$root/ripple-animated/frame-$i.png"); done
java -Djava.awt.headless=true -Db2bj.allowEmptyTail=true -cp "$pack_dir" PackFrames 16 16 6 -1 assets/effects/rain_splashes.png "${frames[@]}"
