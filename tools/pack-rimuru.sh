#!/usr/bin/env bash
# Repack the reviewed human form. Generation and credentials are not needed.
set -euo pipefail
cd "$(dirname "$0")/.."
pack_dir=$(mktemp -d /tmp/b2bj-human-pack.XXXXXX)
trap 'rm -rf "$pack_dir"' EXIT
javac --release 17 -d "$pack_dir" tools/PackFrames.java
root=docs/art-review/rimuru-human
frames=()
append() {
  local source=$1
  shift
  for i in "$@"; do frames+=("$root/$source/frame-$i.png"); done
}
pack() {
  java -Djava.awt.headless=true -cp "$pack_dir" PackFrames 80 80 "$1" -1 "$2" "${frames[@]}"
  frames=()
}

# Omit the repeated endpoint on loops; preserve authored lift and all native pixels.
for direction in south east north; do append "idle-$direction" {0..7}; done
pack 8 assets/characters/blade/rainoray_idle.png
for direction in south east; do append "run-$direction-v2" {0..15}; done
append run-north-v3 {0..15}
pack 16 assets/characters/blade/rainoray_run.png
for action in dash cast guard hurt; do
  for direction in south east north; do append "$action-$direction" {1..8}; done
  pack 8 "assets/characters/blade/rainoray_$action.png"
done

# Each attack: 3 anticipation poses, contact at column 3, 4 recovery poses.
# Rows: combo A south/east/north, then combo B, then combo C.
append slash-a-south-open 1 5 12 15
append slash-a-south-recover {1..4}
append slash-a-east-open 1 3 4 6
append slash-a-east-recover {1..4}
append slash-c-north-open 1 4 7 8
append slash-c-north-recover {1..4}

append slash-b-south-open 4 10 14 16
append slash-b-south-recover {1..4}
append slash-b-east-open 1 2 3 6
append slash-b-east-recover {1..4}
append slash-c-north-open 3 8 11 12
append slash-b-north-recover {1..4}

append slash-south-open 1 4 8 12
append slash-c-south-recover {1..4}
append slash-b-east-open 1 2 4
append slash-east-straight 6
append slash-c-east-recover {1..4}
append slash-c-north-open 2 5 7 8
append slash-c-north-recover {1..4}
pack 8 assets/characters/blade/rainoray_slash.png

# Retain exact slime/human endpoints within the existing sixteen-column atlas.
for action in in out; do
  for direction in south east; do append "transform-$action-$direction" {0..14} 16; done
  # The generated north entrance turns toward camera; reverse its clean exit instead.
  if [ "$action" = in ]; then
    append transform-out-north 16 {14..0}
  else
    append transform-out-north {0..14} 16
  fi
  pack 16 "assets/effects/transform_$action.png"
done
cp "$root/portrait/download.png" assets/ui/blade.png
