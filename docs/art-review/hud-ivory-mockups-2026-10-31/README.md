# Ivory Heraldry gameplay HUD concepts — 2026-10-31

Five full-screen generated HUD mockups extend the approved Ivory Heraldry menu identity to the HP/Ichor display, skills, mission/currency tracker, Menu button, minimap, region labels and Raphael seal.

1. Classic Heraldry — familiar corner layout and shared gold trim.
2. Crescent Crest — small slime/crescent medallion and circular map.
3. Quiet Field — least ornament and screen coverage; preferred starting point for readable combat.
4. Journey Ribbon — HP/Ichor grouped with actions at bottom-center.
5. Moonlit Compass — circular abilities and compass details.

## Review

Selected on2026-10-31: **04 — Journey Ribbon**. Implemented preview: `../journey-ribbon-2026-10-31/index.html`; verification: `../../testing/journey-ribbon-hud-2026-10-31.md`.

Open `index.html`, or the current local gallery: http://127.0.0.1:4517/artifact/2ae2ef0ca4e1/

Five full-HUD views plus twenty focused CSS views: HP/Ichor, skill bar, missions/Menu and minimap. Click a preview to enlarge; arrows/Previous/Next cycle; Escape closes. Original gameplay screenshot and approved menu board linked in the footer. Browser tab1863066976 left open on Full HUD.

To serve again from the repository:

```sh
rtk proxy node /home/ghiankylledomingo/.claude/plugins/ecc/scripts/plan-canvas.js open docs/art-review/hud-ivory-mockups-2026-10-31/index.html --no-open
```

## Provenance and limits

Generated with the built-in image_gen tool, one call per concept, using the user screenshot and approved Ivory Heraldry board as references. Exact prompts and variant directions: `prompts.json`. File origins, SHA256, dimensions and CSS crop rectangles: `manifest.json`. Five original PNGs stored unchanged as `assets/board-01.png` through `board-05.png` (1674×939–940). Reference files and the approved logo bundled alongside.

These are raster design concepts, not production UI or pixel-exact edits of the level. Terrain/sprite details may drift slightly in generated previews. HUD icons, font sizes, dynamic data, cooldown/damage states and viewport behavior need implementation/validation after selection. No runtime source, game JAR or save was edited during this mockup task. No commit or push.

## Checks

All five generated outputs visually inspected. All originals copied byte-for-byte; PNG dimensions, twenty-five crop bounds, relative asset references and JS syntax checked. Chrome verified five cards and decoded images per category, no horizontal overflow, enlargement, Next and Escape; warning/error logs empty. Health and minimap focused views visually inspected. Desktop browser only; mobile and live gameplay not checked.
