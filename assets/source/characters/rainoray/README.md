# Rainoray animation project

Open **Rainoray-Human.pixie-dust** in Pixie Dust (the installed Aseprite fork).
This is the editable timeline. The PNG files in `assets/characters/blade/` are
packed game sheets and open as one image.

There are 672 frames and 76 tags: 19 actions in south, east, north and west facings.
Select a tag and press Enter to play or pause. Tab shows the timeline; F6 toggles
cel thumbnails. Edit **Character** for the body/sword, or **Effects** for the
contact arcs. **Before polish** is a hidden, locked comparison layer.

Keep tag names, frame counts and the 160×160 canvas. Small actions occupy the
middle 80×80 pixels; wide actions use the full canvas. The export checks these
bounds. Merge extra paint layers into Character or Effects before exporting.
The sword is baked into the Character pixels; its separate 3D object remains in
the [editable Blender source](../../../../docs/art-review/sprite3d-run/source/rainoray-editable/).

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
