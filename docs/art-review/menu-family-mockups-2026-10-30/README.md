# Five matching menu families — 2026-10-30

Owner rejected the implemented thin-font/line-icon menu pass and requested five mockups each before more game changes. This gallery contains five complete art-direction boards, each with a main menu, Settings panel and eight-icon family: fifteen focused previews, forty icon concepts in total.

1. Ivory Heraldry — bold ivory cutouts and restrained heraldic trim.
2. Moonstone — carved blue slate, small gems and physical depth.
3. Midnight Folio — storybook lettering, dark vellum and woodcut symbols.
4. Astral Seal — gold/cyan sigils inspired by Raphael's visual identity.
5. Enamel Relic — embossed enamel and small sculpted medallions.

## View

Open `index.html` directly or the loopback gallery at http://127.0.0.1:4517/artifact/015303b68b53/ while its server is running. The Complete sets/Main menus/Settings/Icon sets tabs each show five options. Click an image to enlarge; Previous/Next or arrow keys change style, Escape closes. Full original PNG links are provided in the enlarged view. The Settings category puts all five panels side by side on a wide screen.

Reopen the local server from the repository root:

```sh
rtk proxy node /home/ghiankylledomingo/.claude/plugins/ecc/scripts/plan-canvas.js open docs/art-review/menu-family-mockups-2026-10-30/index.html --no-open
```

Use the returned `/artifact/ID/` URL for the full-width gallery. No approval-wait process is needed for this static concept deliverable.

## Images / prompts

Generated using the built-in `image_gen.imagegen` tool, one call per style board. Full prompts and constraints are in `prompts.json`. Every reference was inspected before generation: `assets/reference-menu.png` supplies the rainy ruined-gate backdrop/composition; `assets/approved-logo.png` is the exact approved logo, copied from runtime art. Generated boards approximate these references; they are raster concepts, not production-ready interface assets.

Original outputs are copied byte-for-byte into `assets/board-01.png` through `board-05.png` (1586×992 each). `manifest.json` records original generated-image paths and normalized viewport crops. `manifest.js` exposes the same data for direct file opening without fetch/CORS. Category previews crop through CSS only; original PNGs are unchanged. No new game code, runtime assets or installed-JAR changes during this mockup pass.

## Checks

All five generated boards visually reviewed. JavaScript syntax checks pass. All fifteen focused crop rectangles validated inside image bounds. Chrome verified five cards in every category, decoded assets, no horizontal overflow at the current desktop viewport, category selection, enlarged preview, next-style navigation and Escape close. Settings and icon comparisons visually inspected; browser warning/error logs empty. Not tested on a phone. No gameplay tests needed or run for this preview-only pass.
