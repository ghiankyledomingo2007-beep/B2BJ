# Rainoray — editable body, sword and mask

Open **Rainoray-Body-and-Separate-Sword.blend** in Blender 4.5 or newer.

- **RainorayBody**: the full character, skinned to the existing 20-bone **RainorayRig**.
- **RainoraySword**: separate weapon mesh. Hidden initially so the whole body can be inspected. Expand RainorayRig in the Outliner; enable the sword's eye and camera icons to show it in the viewport and renders.
- **RainorayMask**: removable simplified face surface, weighted to the head. White face, black curves and smile, one flat red dot. Hide it to compare with the original face underneath. `mask-simple.png` is the packed texture source.

The rig includes torso, head, hair, both arms, hands, legs and feet. It does **not** have individual finger bones or facial controls. The blade bone remains under hand.R but is hidden in the viewport; unhide it for weapon posing. Body vertices no longer follow the blade bone; the sword is fully weighted to it.

The file opens in the rest pose. Existing animations are procedural poses in `tools/sprite3d/`, not timeline Actions stored in the file. Pressing Play alone will not animate it.

## What was checked

- 228 start/middle/end pose samples across 19 actions and four facings.
- Unaffected original vertices match the original puppet exactly. Hand/sword weights were intentionally repaired: 335 body vertices and 91 sword vertices changed weights.
- All 20 original bones, original surface vertices and UVs retained; hand and sword cut surfaces capped.
- Front, back and both side renders reviewed. Native 80×80 front idle shows a single red pixel for the nose dot. Side views can hide the dot at this scale.
- Original and separate meshes tested with slash trails. All visible body/sword/mask meshes occlude the trail; a hidden sword produces no trail.
- Texture images packed in the .blend.

## Scope and remaining polish

The reviewed armed animation set is installed in the game's PNG assets as of 2026-10-22. The editable source still opens with the sword hidden. The hands retain the original low-detail gripping shape and rough texture seams; individual fingers and new unarmed animations still need manual work. The original sculpt and hair texture have not been retouched.

Original cached puppet: `../../sheets/puppet.blend`. A separate backup also lives in the school's `Asset-Polish/Rainoray-Blender-Backup-2026-10-21.blend`.

`rebuild.py` records the one-time conversion and pose assertions. It verifies the original puppet hash against `sword-faces.json`. **Running it overwrites this editable file and its previews; do not run it after manual polishing without saving a separate copy.**

From the repository root, run `blender -b --python-exit-code 1 --python tools/sprite3d/check_editable.py` to check rig weights and trail rendering. This check needs the original local puppet cache and writes temporary previews only.

To render the edited file without installing sprites:

```sh
blender -b --python tools/sprite3d/animate.py -- docs/art-review/sprite3d-run/source/rainoray-editable/Rainoray-Body-and-Separate-Sword.blend /tmp/rainoray-preview --armed --action idle --frames 8
```

As of 2026-10-23, `build_sheets.py` renders this editable file directly, showing the sword only in memory. It no longer rebuilds the old GLB or uses the old puppet cache. Default output is `build/rainoray-sprites`. Render first, review, then use `--install`; installation replaces live sheets and does not merge teammates' pixel edits.

The renderer adds a tiny flat red decal over the existing mask dot so it stays visible at native resolution, then snaps it to the character palette. The editable model and packed mask texture remain untouched. See the [human-form polish review](../../../rainoray-human-polish/README.md) for current stance, movement and contact effects.


## 2026-10-22 — Side-run sword carry

Front/back carry was corrected first. Both side runs now use a steady trailing/downward direction through the same hand-and-sword carry logic. Body bob is retained.

- Side run angular sweep: 21.78° before, 3.38° after.
- Hilt-relative tip sweep: 0.353 before, 0.124 after (source coordinates).
- Rig blade tip stays above the rendered action floor in run/start/stop/turn; all 40 side cells reviewed with no clipped/edge-touching cells or packing shifts.
- 632 full-body idle/combat/front/back poses compared with the previous version, maximum matrix difference 3.58e-7. Front/back rows in the four repacked sheets are byte-identical as pixels.
- Native sprite comparison: [before/after right and left runs](game-preview/Rainoray-Side-Run-Comparison.mp4), 4 seconds at the game's 16fps stride cadence. This is sprite playback on a neutral background.
- Actual game input/render captures cover movement and direction changes for all four initial directions. The refreshed [four-direction animation reel](game-preview/Rainoray-Game-Animations.mp4) also includes unchanged combat/dash.
- Twenty reviewed PNGs (14 body sheets, four trails, two transforms) now live under assets/characters/blade and assets/effects. No .blend mesh/rig edits in this carry fix. Original PNGs backed up outside the repository before installation.

Measured comparison and installed-asset checksums: [side-carry-review.json](side-carry-review.json), [installed-assets.json](installed-assets.json).

## 2026-10-23 — Eight-way runtime

Four diagonal cameras were added without editing this model. Runtime sheet row order
is S, E, N, W, SE, NE, NW, SW. `build_sheets.py --preserve-cardinals` reuses the current
polished cardinal frames and renders only diagonal views. All 19 actions, four trail
sheets and transformations are covered. The native project has 1,344 frames / 152 tags.
See [the eight-way review](../../../rainoray-eight-way/README.md).
