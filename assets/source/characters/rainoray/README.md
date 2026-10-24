# Rainoray animation project

Open **Rainoray-Human.pixie-dust** in Pixie Dust (the installed Aseprite fork).
This is the editable timeline. The PNG files in `assets/characters/blade/` are
packed game sheets and open as one image.

There are 1,344 frames and 152 tags: 19 actions in south, east, north, west, southeast, northeast, northwest and southwest facings.
Select a tag and press Enter to play or pause. Tab shows the timeline; F6 toggles
cel thumbnails. Edit **Character** for the body/sword, or **Effects** for the
contact arcs. **Before polish** is a hidden, locked comparison layer.

Keep tag names, frame counts and the 160×160 canvas. Small actions occupy the
middle 80×80 pixels; wide actions use the full canvas. The export checks these
bounds. Merge extra paint layers into Character or Effects before exporting.
The sword is baked into the Character pixels; its separate 3D object remains in
the [editable Blender source](../../../../docs/art-review/sprite3d-run/source/rainoray-editable/).

## Character refit — 2026-10-24

The current timeline contains the compact head, curved porcelain-mask markings,
broad navy coat panels and quieter boot shading across all 1,344 frames.
**Before polish** now holds the preceding compact-head
eight-way runtime art. The original native project and model are backed up under
`~/tmp/rainoray-refit-20261024/before/` and remain in Git history.
The mask-and-coat pass has a separate backup at `~/tmp/rainoray-mask-coat-20261024/before/`.

Attack timings now match the game's slower cuts, including the longer contact
poses. No gameplay timing or movement values changed in this art pass.
[Review, source and validation](../../../../docs/art-review/rainoray-refit/README.md).

## Export edited frames

Save the native project first. Run from the B2BJ root:

```sh
~/.local/bin/pixie-dust --batch --script-param mode=export \
  --script-param out=build/rainoray-pixel-export \
  --script tools/art/rainoray-project.lua
```

Use a fresh output directory each time; existing exports are protected.
Review its PNGs before installing. Body sheets belong in `assets/characters/blade/`;
the four `*_trail.png` sheets belong in `assets/effects/`. Rebuild `transform_in`
and `transform_out` from the edited idle using `build_sheets.py --out
build/rainoray-pixel-export transform`, then install the reviewed sheets and run
`bash build.sh`. The normal game build does not regenerate art or overwrite this project.

Timeline timings preview the game's current timing to the nearest millisecond.
Changing timing in the editor alone does not change Java combat timing.
Re-rendering from Blender does not merge hand-painted pixel changes.

Check an installed export with `/usr/bin/python3 tools/art/check-rainoray-project.py`.
For a new import, the same Lua script's default `create` mode reads runtime PNGs
and refuses to overwrite an existing native project.

## Cluster cleanup

The four cardinal Character views went through one cluster pass on 2026-10-23. The new diagonal views use the same pass. The earlier comparison layer remains in [the archived four-way project](archive/Rainoray-Human-four-way.pixie-dust). The pass flattens leftover 3D-render
tones, merges stray specks into their material, keeps mask markings flat black,
thins comb-like back-hair strands and rounds the back-view crown. Alpha, the red
dot, gold, hilt and effects are untouched. Hand-paint further fixes in the editor;
do not run the pass again on art that already had it. To repeat it on a fresh
render, clean the exported body sheets and re-import them:

```sh
/usr/bin/python3 tools/art/clean-rainoray-pixels.py build/rainoray-pixel-export build/rainoray-clean
```

Copy the four `*_trail.png` sheets next to the cleaned sheets, then use the Lua
`create` mode with `project=`, `sheets=` (that folder) and `before=` (the
uncleaned sheets) on a new project path.

## Eight-way update — 2026-10-23

Four new camera views come from the same editable 20-bone model. Original cardinal
body pixels were retained for all 672 frames. Guard and hurt now use a 160×160 cell
with transparent padding so diagonal sword tips cannot clip; the character scale and
foot anchor did not change. The four trail sheets and two transformation sheets also
have eight direction rows.

Use the updated exporter: attack blocks contain eight rows, not four. An old exporter
will place attack poses in the wrong rows. The old native project is preserved under
`archive/`; it is excluded from the game package. [Review and checks](../../../../docs/art-review/rainoray-eight-way/README.md).
