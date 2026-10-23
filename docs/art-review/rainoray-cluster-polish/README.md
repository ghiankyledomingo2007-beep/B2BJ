# Rainoray cluster polish — 2026-10-23

[Editable project and guide](../../../assets/source/characters/rainoray/README.md)
· [Idle, four facings](idle-four-facings.png)
· [Run, four facings](run-four-facings.png)
· [Face, hands and coat close-up](face-and-hands.png)

The Blender render left scattered shading pixels: in-between tones on the mask,
hair, hands and coat, single bright flecks on the cloth and comb-like stripes in the
back of the hair. This pass turns them into deliberate clusters across all 672
frames (19 actions × four facings) without changing the design or the animation.

- Render in-between tones snap to the flat colour beside them; hands become one
  skin shape. Shading next to the gold hilt stays.
- Lone specks join the material around them. Diagonal trims, seams and folds
  stay: a speck only changes material when its whole cluster is three pixels or less.
- Blurred dark marks on the mask become its flat black markings. The single red
  dot, gold shoulder trim, gold/red hilt and every alpha silhouette are unchanged.
- Back view: where two one-pixel hair strands sit one pixel apart, the longer one
  stays, and a one-pixel shade down the crown's right edge keeps it round.

The idle south frame is also the dialogue portrait, so it gets the same cleaner
face. Both transformation sheets were rebuilt from the cleaned idle.

## Measurements

| | Before | After |
|---|---|---|
| Interior pixels with no same-colour neighbour | 9.9 % | 4.2 % |
| Frame-to-frame changed pixels, idle | 10,518 | 9,636 |
| Frame-to-frame changed pixels, slash + cleave sheets | 332,945 | 321,846 |
| Frame-to-frame changed pixels, all 14 sheets | 576,019 | 554,058 |
| Pixels changed in 14 body sheets | — | 60,314 |

All 14 sheets change fewer pixels between frames afterwards (2–9 % less), so
motion flickers less. Compared with the earlier conservative draft (8.8 % lone pixels), this pass
was reviewed frame by frame at game scale and enlarged.

## Verification

- `tools/art/clean-rainoray-pixels.py` runs its fixtures before every use (speck
  removal, diagonal trims, mask markings, gold/red, back-hair strands, input
  untouched) and asserts every sheet's alpha is identical.
- `/usr/bin/python3 tools/art/check-rainoray-project.py`: the native project
  exports all 18 installed sheets pixel-for-pixel; 672 frames, 76 clips, timings.
- Rebuilding the transforms from the old idle reproduces the old files exactly,
  so their change comes only from the cleaned idle.
- `bash build.sh`: Java tests and packaged-asset checks (see commit).

Before polish in the native file and Git commit `ef0b2ed` keep the previous art.
The Blender model is unchanged; a fresh 3D render would not include this 2D pass.
