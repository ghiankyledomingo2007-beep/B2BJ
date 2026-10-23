#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Retired absorb/vitality exports live in archive/assets/2026-10-23 and are not restored here.
# Reviewed native frames. Crop removes the orb's baked ground shadow without resizing.
java -Db2bj.cropBottom=27 tools/PackFrames.java 32 32 8 -1 assets/effects/ichor_idle.png docs/art-review/hud-40/orb-pulse/frame-1.png docs/art-review/hud-40/orb-pulse/frame-3.png docs/art-review/hud-40/orb-pulse/frame-5.png docs/art-review/hud-40/orb-pulse/frame-7.png docs/art-review/hud-40/orb-pulse/frame-9.png docs/art-review/hud-40/orb-pulse/frame-11.png docs/art-review/hud-40/orb-pulse/frame-13.png docs/art-review/hud-40/orb-pulse/frame-15.png
java -Db2bj.cropBottom=27 tools/PackFrames.java 32 32 8 -1 assets/effects/ichor_pickup.png docs/art-review/hud-40/orb-collect/frame-2.png docs/art-review/hud-40/orb-collect/frame-3.png docs/art-review/hud-40/orb-collect/frame-4.png docs/art-review/hud-40/orb-collect/frame-6.png docs/art-review/hud-40/orb-collect/frame-8.png docs/art-review/hud-40/orb-collect/frame-10.png docs/art-review/hud-40/orb-collect/frame-12.png docs/art-review/hud-40/orb-collect/frame-15.png
java tools/PackFrames.java 48 48 8 -1 assets/effects/corpse_idle.png docs/art-review/hud-40/remains-idle/frame-0.png docs/art-review/hud-40/remains-idle/frame-1.png docs/art-review/hud-40/remains-idle/frame-2.png docs/art-review/hud-40/remains-idle/frame-3.png docs/art-review/hud-40/remains-idle/frame-4.png docs/art-review/hud-40/remains-idle/frame-5.png docs/art-review/hud-40/remains-idle/frame-6.png docs/art-review/hud-40/remains-idle/frame-7.png
java tools/PackFrames.java 48 48 8 -1 assets/effects/corpse_consume.png docs/art-review/hud-40/remains-consume/frame-0.png docs/art-review/hud-40/remains-consume/frame-2.png docs/art-review/hud-40/remains-consume/frame-4.png docs/art-review/hud-40/remains-consume/frame-6.png docs/art-review/hud-40/remains-consume/frame-8.png docs/art-review/hud-40/remains-consume/frame-10.png docs/art-review/hud-40/remains-consume/frame-13.png docs/art-review/hud-40/remains-consume/frame-16.png
cp docs/art-review/hud-40/slash/frame-0.png assets/ui/slash.png
cp docs/art-review/hud-40/ui-unify/frame-1.png assets/ui/blob.png
cp docs/art-review/hud-40/ui-unify/frame-2.png assets/ui/blade.png
cp docs/art-review/hud-40/ui-unify/frame-4.png assets/ui/wave.png
cp docs/art-review/hud-40/ui-unify/frame-5.png assets/ui/dash.png
cp docs/art-review/hud-40/ui-unify/frame-6.png assets/ui/consume.png
cp docs/art-review/hud-40/hud-frame/frame-0.png assets/ui/hud_frame.png
cp docs/art-review/hud-40/skill-frame/frame-0.png assets/ui/skill_frame.png
cp docs/art-review/hud-finish-40/camp-marker/frame-0.png assets/ui/camp.png
cp docs/art-review/hud-finish-40/heal-marker/frame-0.png assets/ui/heal.png
# Frame 7 is blank; retain the sparse final motes without adding a flashing empty frame.
java tools/PackFrames.java 48 48 8 -1 assets/effects/heal_motes.png docs/art-review/hud-finish-40/heal-rise/frame-0.png docs/art-review/hud-finish-40/heal-rise/frame-1.png docs/art-review/hud-finish-40/heal-rise/frame-2.png docs/art-review/hud-finish-40/heal-rise/frame-3.png docs/art-review/hud-finish-40/heal-rise/frame-4.png docs/art-review/hud-finish-40/heal-rise/frame-5.png docs/art-review/hud-finish-40/heal-rise/frame-6.png docs/art-review/hud-finish-40/heal-rise/frame-8.png
