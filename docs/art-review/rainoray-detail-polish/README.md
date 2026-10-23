# Rainoray pixel cleanup and editable timeline — 2026-10-23

[Editable project and export guide](../../../assets/source/characters/rainoray/README.md)
· [Editor screenshot](editor-final.png)
· [Idle comparison](idle-comparison.png)
· [Running comparison](run-comparison.png)
· [Contact-pose comparison](slash-comparison.png)

The follow-up merges nearly identical dark, blue and pale shades in the existing
character palette. The red mask mark becomes one native pixel whenever visible.
Skin/gold ramps, every body's alpha silhouette, frame layouts, motion and contact
effects are preserved. Both transformations were rebuilt from the cleaned idle.
This is a small pixel pass, not a new model or texture repaint.

The native project has 672 timeline frames and 76 named action/direction tags:
idle, run, start, stop, turn, draw cut, thrust, diagonal cut, overhead cut,
dash slash, cleave, spin cleave, charge, hold, dash, cast, guard, hurt and counter.
Character and contact Effects are separate layers. The hidden, locked Before
polish layer preserves the previous version of every character frame.

The 160×160 editing canvas centers the smaller 80×80 actions. Export restores
the game's original sheet sizes and row order. Clip timings follow the current
Java timers, including four windup frames before attack contact; durations are
rounded at cumulative millisecond boundaries.

## Verification

- `bash build.sh`: all 85 Java test programs and packaged checks passed. All 182
  runtime PNGs have binary alpha and match the JAR; editable sources stay excluded.
- All 672 body cels preserve their original alpha, and contain at most one red
  mask pixel. All four effect PNGs remain byte-identical. Four-facing comparison
  poses inspected for idle, run, attacks, dash, casting, guard and hurt.
- `/usr/bin/python3 tools/sprite3d/check_pixel.py` checks palette cleanup,
  unchanged skin/alpha, one dot, source preservation and repeated application.
- `/usr/bin/python3 tools/art/check-rainoray-project.py` reopens the native
  project and checks all 18 body/effect exports pixel-for-pixel against runtime,
  clip ranges/timing, and refusal to overwrite existing exports.
- Editor playback observed on the laptop monitor, with Character selected and
  the timeline visible. PNG sheets remain runtime exports, not editor timelines.

The prior assets are preserved in Git commit `709fc8e` and the native comparison
layer. The separate sword and rig remain in the original Blender source.
Hand anatomy and the rig were not remodeled. Editor timing changes alone do not
alter Java combat timing; the game still needs reviewed PNG exports.
