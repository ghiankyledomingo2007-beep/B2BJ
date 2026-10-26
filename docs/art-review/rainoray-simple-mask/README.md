# Rainoray — simple mask and moving hair

2026-10-24. Keep the mask readable at game size: one small red nose mark,
short eye marks and tiny cheek ticks. Remove the long copied curves and jaw line.
White hair uses broader locks; idle and running add modest sideways movement,
with the tips following the roots. Existing combat hair motion is retained.

The current Blender model, all 14 body sheets, four matching contact-effect sheets,
two transforms, and the 1,344-frame / 152-tag native animation project share this pass.
Dialogue and loading-screen portraits read the updated runtime character.

[Before/after](comparison.png) · [Idle/run motion](motion.gif) · [Camp view](game-preview.png)

## Source

`tools/sprite3d/simplify_mask.py` converts the preceding model once into a fresh
output. It verifies the original bytes, body geometry, weights and 20-bone rig.
The native model has no separate raised nose; its red mark stays on the mask.
`locomotion.py` supplies the hair's lateral drift and tip lag. Foot and sword motion
are unchanged.

Regenerate into a fresh directory with `tools/sprite3d/build_sheets.py`, run the
existing pixel cleanup once, then rebuild transforms from the cleaned idle.
Use `tools/art/rainoray-project.lua` to recreate the editable timeline. The normal
game build uses the installed PNGs and does not require Blender.

Previous model, all runtime sheets, native project and locomotion code are backed
up under `~/tmp/rainoray-simple-mask-hair-20261024/before/`.

## Verification

- All 92 Java test programs and packaged asset checks pass.
- Native editor exports all 18 sheets losslessly; 1,344 frames, 152 tags and timings match.
- All rendered strips and transforms fit their cells without clipped or repeated frames.
- Front idle keeps exactly one red nose pixel per frame. Runtime layouts and binary alpha are preserved.
- Rig checks pass for hair sway, planted feet, sword carry, combat continuity and contact effects.
- Reviewed the character in the camp renderer and opened a temporary human-form preview.
  The normal save stayed unchanged. No full manual campaign playthrough was performed.
