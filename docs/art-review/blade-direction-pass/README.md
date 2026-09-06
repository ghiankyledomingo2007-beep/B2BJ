# Blade direction pass — 2026-09-06

Scope: one consistent Blade cut, Ruined Outpost only. Owner authorizes needed
PixelLab spending and requests notification when the fresh 40-generation allowance
runs out. The provider reported 40 remaining / 0 used before these jobs.

The editor's existing MCP connection still held the old exhausted credential.
`tools/pixellab-call.py` reads the already-updated private owner config without
printing credentials. It does not retry paid requests automatically. Its offline
credential/redaction check is `python3 tools/test_pixellab_call.py`.

## Root cause and anchors

Visual review of the existing Blade run/slash side rows shows EAST-facing source
art. `BladeAnimation.flipHorizontal()` incorrectly mirrored EAST and left WEST
unchanged. The shared flip is corrected for idle, movement, dash and attack.
Slime has a different source orientation and is deliberately unchanged.

Initial references used unchanged 48×64 slash poses with eight transparent pixels
added on all sides. This exposed another source defect: the old slash body's
height is smaller than the run sprite, and its nominal north row shows a mask.
Those anchors are unsuitable even if a generated motion looks smooth.

The corrected east reference uses frame 0 of the east RUN row. The previously
staged south/north cuts also use their correctly facing run rows. All are padded
to 64×80, keeping source pixels and the ground line unchanged. The extra margin
is sword clearance, not a larger body or collision footprint.

## Jobs

| Direction | PixelLab job | Cost | Review |
| --- | --- | ---: | --- |
| East cut | `8e6d6559-7ae2-45fe-87d1-cd67b32c55f0` | 1 | Rejected: giant white/brown streaks obscure body. |
| South cut | `39f67a84-6fb7-4b65-926c-3e4d2abd513f` | 1 | Rejected: giant opaque sweep, smaller body, turns sideways. |
| North cut | `699250c4-bdc4-4588-82ea-433e6cb13a19` | 1 | Rejected: incorrect source facing persists; giant arc. |
| East thrust, old anchor | `98106488-9547-4ec1-b883-ba5eab801d6b` | 1 | Rejected: strikes left while looking right, stray streaks, smaller body. |
| East thrust, run anchor | `12300da2-988a-4499-a08b-7f993354e403` | 1 | Body scale retained, but sword remains upright: edit input only. |
| East strike endpoint | `aac1a123-f116-402e-b17e-9b456d71b07c` | 1 | Rejected: no sword, changed body height/detail. |
| East Pro correction, seven frames | `b315ed6f-45ea-4887-9be0-5bac0e326ade` | 20 settled | Rejected: weapon direction improved, but clipped tips/adjacent-frame fragments and vertical displacement; floor changes from 71 to 64. |
| East short-prompt thrust | `5d948150-1057-40dd-9013-0837405624ee` | 1 | Rejected: detached/smeared pale-cyan blade and floating fragments. |
| East short-prompt cut | `7b4e4f4c-05d3-44b0-95d5-40d7f2a2229a` | 1 | Rejected: missing weapon in strike poses, gold/brown body flashes and fragments. |

Final settled balance: **12 remaining / 28 used of 40**, $0 purchased credit.
Eight ordinary jobs cost one each; the Pro batch cost 20. All nine jobs completed;
all 71 returned PNGs are stored locally. None of this pass's new generations met
the runtime acceptance gate. The allowance has not run out.

Exact requests and local starting/ending frames are beside this document. First
east request exceeded PixelLab's 1,000-character action limit; validation rejected
it without creating a job. The shortened request above was then accepted.

Acceptance: readable windup → cut → recovery, constant body size, grounded feet,
same equipment, correct facing, genuine transparency, sword contained in canvas.
Generation alone does not mean integration.

## Integrated result

Reused the previous batch's accepted south/north cut studies, not new rejected
outputs. Their source indices **0,1,3,4,5,6,7,8** are padded by eight transparent
pixels and packed into `assets/characters/blade/blade_cut.png`: 512×160, eight
64×80 cells per row, south then north. Seven distinct poses plus the repeated
rest pose play over the unchanged 0.3-second attack window. No recoloring or
resampling; the body remains at native 2× scale and the floor anchor is 72.

East/west still use the old three-cell `blade_slash.png` side row. The shared
mirror correction is active, but the side art's scale/foot alignment and motion
still need replacement. This is a partial animation improvement, not a finished
directional set or three finished combo animations.

The imported side reference also contains a floating pixel above the hood.
Source atlas framing/stray pixels should be audited before more side generation;
bounding-box height alone confuses such fragments with the actual character.
The Pro result's main geometry defect is vertical displacement, not established
uniform body shrinkage; its clipped weapon tips remain a separate rejection.

Verification: all 17 Java test programs; an offline PixelLab credential/redaction
check; four in-scene attack renders (`scene-*.png`). Front/back attack entry renders
pixel-identically to idle, including floor and native scale. Side tests retain the
legacy grid and verify east/west orientation; they do not certify its art quality.

Scene checks use `PreviewOutpost 5 output.png blade-south` (or north/east/west),
compiled against `build/B2BJ.jar`. They are controlled, muted fixtures, not a full
unassisted playthrough. The running user game was not restarted.
