# Rainoray mask finish and Blade HUD

2026-10-27. Game polish for the Friday competition; trailer work deferred.

The previous mask lost an eye marking in several front-facing idle frames.
Short angular slits and slightly wider cheek ticks now read at the native pixel
size. The small red nose remains exactly one pixel throughout front idle.
White hair motion, body proportions, footwork, sword carry and action timings
remain as before.

[Before / after](comparison.png) · [Actual game renderer](game.png)

The game preview also exposed the Blade timer overlapping Raphael's seal.
Blade resources now sit beneath HP and Ichor; the testing label moves below
that panel. The bottom ability bar remains centered. This keeps the top center
available for Raphael and the boss health bar.

## Editable sources

`tools/sprite3d/simplify_mask.py` updates the existing native model into a fresh
output and checks body geometry, skin weights and the 20-bone rig. It accepts
the preceding simple mask without applying the hair palette change again.
The installed model is
`docs/art-review/sprite3d-run/source/rainoray-editable/Rainoray-Body-and-Separate-Sword.blend`.

The existing render pipeline regenerated all 14 body sheets and four contact
effect sheets. Pixel cleanup ran once on fresh body renders, followed by both
transforms. Two contact sheets remained byte-identical. The editable
`assets/source/characters/rainoray/Rainoray-Human.pixie-dust` contains the same
final art, with the preceding simple mask in its hidden comparison layer.

## Verification

- Java 17 lint compilation passed.
- Rig checks passed for moving hair, planted feet, sword carry, attack continuity,
  contact effects and hidden-mask occlusion.
- The native project has 1,344 frames / 152 tags; all 18 exported body/effect
  sheets match runtime PNGs losslessly and timing checks pass.
- The mask regression test fails on the previous build and passes on the new
  art: both eyes stay readable and the nose stays one red pixel across front idle.
- The HUD regression fails on the previous build and passes at widths 1280,
  1920 and 2560, both with and without an active boss.
- Fourteen focused mask, combat, story, loading and packaged-asset programs
  passed. Six HUD/overlay/healing/rendering programs then passed against the
  final build (PackagedAssetLoadingTest appears in both groups).
- Corrected the rescue reminder test to observe the line during a bounded
  interval; its old fixed sampling time missed the existing earlier playback.
  The old test also failed on the unchanged baseline build.
- The packaged diff contains only two renderer classes and 18 runtime PNGs.
  Voice, music and gameplay classes are byte-identical to the preceding build.

Camp and before/after frames were inspected from the actual renderer. A native
game window and continuous manual campaign playthrough were not checked; the
available GUI connection had no browser. Staging, previous files and detailed
reports are under `~/tmp/b2bj-mask-polish-2026-10-27/`.
