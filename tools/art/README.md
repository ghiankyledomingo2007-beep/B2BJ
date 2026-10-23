# Outpost art pipeline

Most environment art is drawn by the Java code in this folder using the night palette
(`Pal.java`, exported as `assets/source/shared/outpost_night.gpl`). The camp tent, training dummy and
dialogue sprites are imported from preserved generated masters by `ImportedArt.java`,
which applies native dimensions, locked palettes and hard transparency. Generation
records live in `docs/art-review/asset-cleanup/`, `docs/art-review/area-one-story/`
and `docs/art-review/outpost-polish/`.
Rebuilding from the same code, map and source images produces the same pixels.

## Rebuild

```bash
tools/art/build-art.sh            # everything (about 3 s)
tools/art/build-art.sh ground     # only the floor, after changing the map layout
tools/art/build-art.sh campfire   # only the flat-color eight-frame camp flame
tools/art/build-art.sh brazier    # shared tower and brazier fire
tools/art/build-art.sh warden     # solid boss sweeps and compact phase-two flames
tools/art/build-art.sh tent       # only the 128 × 100 camp tent
tools/art/build-art.sh dummy      # two native 36 × 72 training-dummy variants
tools/art/build-art.sh gate       # eight opening poses for the East Gate
tools/art/build-art.sh portraits  # native Mara and slime dialogue art
tools/art/export-sources.sh       # missing editable projects, grouped by region; existing projects kept
./build.sh                        # then the normal build and tests
```

`OutpostArtTest` fails with "run tools/art/build-art.sh ground" when `CampaignWorld`, the
obstacles or `src/SetPieces.java` change without a rebake (the floor stores a layout fingerprint).
`python3 tools/art/ExportSourcesTest.py` checks source-folder routing and preserves existing
editable layers, using a temporary folder and a mock editor.

## Retouching by hand

- Edit the `.pixie-dust` file in `assets/source/outpost/` or `assets/source/forest/`, then export PNG over the matching asset. Shared fire and palette sources are in `assets/source/shared/`. The build
  remembers what it generated (`generated.sha`) and **keeps** any PNG you changed; `--force` regenerates.
- The floor is rebaked from code, so paint floor fixes on a transparent layer saved as
  `assets/source/outpost/outpost_ground_retouch.png` (3072x2048); every bake paints it on top.

## What each file draws

| File | Output |
|---|---|
| `GroundBake.java` | `assets/tilesets/outpost/outpost_ground.png`: grass, meadow, dirt, puddles, rutted roads along the real routes, a floor per landmark (flagstone, cobble, gravel, camp dirt, flooded ditch, the Warden's carved arena), decals and ambient shadows; firelight is rendered by `OutpostLights` |
| `Canopy.java` | the forest pressing on the boundary banks |
| `Decals.java` | tufts, reeds, glowing mushrooms, stones and the fallen expedition's kit (`outpost_decals.png`) |
| `Props.java` | ruined walls, log palisades, supplies for the map's obstacle clusters (same footprints as before) |
| `Structures.java`, `Buildings.java` | landmark set pieces, placed by `src/SetPieces.java` |
| `ImportedArt.java` | native tent, dummy and dialogue PNGs from generated masters under `assets/source/` |
| `Kit.java` | stone, plank, shingle, cylinder and fire builders |
| `WardenArt.java` | `assets/effects/warden_*.png`: cracks, debris, dust, sweep, spikes, flames, beams, motes |
| `Tiles16.java` | `outpost_tileset.png`, `forest_tileset.png`: 16 px fills with variants + Wang corner sets (three versions of every edge tile) |
| `ForestBake.java` | `assets/tilesets/forest/forest_ground.png`: the Corrupted Forest floor, built tile by tile from the forest tileset, then decals, tree shade, light pools and the canopy |
| `Creatures.java` | `assets/characters/forest/`: Rift Imp, Thorn Wolf, Mire Shaman in the remnants' sheet format (48 px cells; walk 4 frames, attack 8; front, side, back) |
| `BossArt.java` | `assets/characters/guardian/briarheart_motion.png`: Briarheart, 96 px cells drawn at 2x, rows in Guardian.Animation order |
| `WardenArt.briar` | `assets/effects/briar_*.png`: the Warden's effect sheets recoloured for Briarheart (cyan becomes violet in the dark steps, gold in the bright) |
| `ForestArt.java` | forest set pieces (oaks, pines, withered trees, bushes, logs, boulders, the boat, white cloth, the hollow tree, the Moonwell, the orchard, brambles, roots, the stair, rift stones, the lean-to), placed by `src/SetPieces.java` |

Supplies have one runtime sheet, `assets/props/outpost/set_supplies.png`, used by
both obstacle clusters and landmarks. The duplicate `supplies.png` and its editable
project are preserved in `archive/assets/2026-10-23/`; exporters no longer recreate them.

## Tileset layout (`outpost_tileset.png`, 16 px cells, 16 columns)

Current campaign tilesets use 16×16 cells. [Tilesets by area](../../assets/tilesets/README.md)
lists their runtime and editable folders. Older room tiles are kept under
`assets/tilesets/legacy/ruined_outpost/`.

Rows 0-3: Outpost — grass, meadow, dirt, flagstone, cobble, water, mud, gravel; Forest — leaf litter, moss,
grass, dirt, blackwater, dry riverbed, blight, mud. 8 variants each. Variants
0-3 are plain and 4-7 carry one small detail (flowers, a pebble, a crack, a lily pad); every
variant joins every other, so paint them at random, mostly 0-3.
Then Wang corner sets, three rows per pair. Outpost (rows 4-27): grass/dirt, grass/meadow, grass/water,
grass/flagstone, grass/cobble, dirt/flagstone, dirt/water, grass/mud. Forest (rows 4-33): litter/dirt,
litter/moss, litter/grass, litter/blackwater, litter/riverbed, riverbed/blackwater, litter/blight,
grass/dirt, dirt/riverbed, litter/mud. The forest floor itself is baked from exactly these tiles. Same corner-mask columns
as the fill rows. Column = corner mask: 1 top-left, 2 top-right, 4 bottom-left, 8 bottom-right;
a set bit is the second material. The three rows are versions of the same tiles: give them one Wang id and let the editor
pick at random so long borders don't repeat. Same pixel size as the game, so tiles sit next to
the 64 px characters without mismatched pixels; 16 px cells allow finer paths and edges than 32.
`build-art.sh` proves the sheet seamless before saving it (`Tiles16.verifySeams`) and fails on a seam.

Scale: a person is about 64 native pixels tall; the game draws everything at 2x.

## UI, title and loading art (`build-art.sh ui`)

- `fonts.py` (Python + fontTools, run on its own): the two pixel fonts as real TrueType files in
  `assets/ui/fonts/`. **B2BJ Sans** is the proportional UI text (caps 7 px, x-height 5, descenders 2);
  **B2BJ Display** is the chunky title face (11 px capitals, 2 px stems). Each glyph is a row of `#`
  pixels in the file: edit a row, rerun `python3 tools/art/fonts.py --preview /tmp/glyphs.png`, rebuild.
  One font pixel is 100 units on a 1000-unit em, so Java size 20 draws the game's 2x pixels exactly.
- `UiArt.java` + `UiIcons.java` -> `assets/ui/kit.png`: window/panel/card/inset frames, buttons (idle, hover,
  pressed, disabled), key caps, check boxes, slider parts, boss-bar parts and emblems, the sword cursor,
  trims and 24 icons. Rectangles come from `Ui.Piece` / `Ui.Icon` in `src/Ui.java`, so the game slices exactly
  what is drawn. The icons are character maps in `UiIcons.java` (one letter per palette colour).
- `TitleArt.java` imports `assets/source/ui/title/ruined-gate-master.png` into the 640×360 runtime
  `assets/ui/title/ruined-gate.png`, respecting the generated-art manifest's manual-edit guard.
  `TitleScreen.java` adds the live wordmark and a few rain streaks. The former title layers remain
  in `assets/source/ui/title/previous-title/`; the three still used for loading have dedicated paths
  under `assets/ui/loading/`.
- DejaVu Serif and Sans provide smooth menu text and boss names alongside the pixel HUD fonts.
  Their redistribution license is in `assets/ui/fonts/DejaVu-LICENSE.txt`.
- `LogoArt.java` still supplies the boot splash logo; it no longer generates a runtime title logo.
- `UiArt.splash()` -> `assets/ui/splash.png`: the boot splash, already doubled to 640x360 screen pixels because
  the JVM shows it unscaled (`tools/manifest.txt` names it in the jar). `src/Splash.java` fills its load bar.

Audio: `tools/audio/slime_sfx.py` rebuilds the slime hop and dash sounds from the CC0 slime pack credited in
`assets/audio/CREDITS.md` (download URL in the script).
