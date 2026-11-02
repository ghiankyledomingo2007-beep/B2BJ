# Five animated main-menu concepts — 2026-10-30

Open `index.html` directly in a browser. Everything is bundled locally: no build,
package installation or external requests. The gallery uses the exact approved
Moonlit Gate logo and the game's existing ruined-gate illustration.

1. **Gatekeeper** — left logo/menu, unobstructed gate, light rain and cyan glow.
2. **Moonlit Center** — centered composition, darker scenery, mist and motes.
3. **Moonlit Dock** — horizontal bottom navigation and slow background drift.
4. **Seal & Stone** — recessed left panel with worn gold trim and warm motes.
5. **Through the Gate** — reframed arch, right menu and opening camera settle.

The sixth tile is the already-approved loading screen, included as an identity
reference. It is not a sixth menu concept.

Use **Open preview** for a large live view. Hover/click or use arrow keys to move
the selected action; Home/End also work. Escape closes the preview. **Replay
entrance** resets that preview. **Pause motion** freezes all animations; the CSS
also respects `prefers-reduced-motion`. Offscreen gallery scenes pause.

These are silent visual prototypes. Buttons only preview their selected state.
No Java game code, installed JAR, runtime artwork or save files were changed.
Pick a layout before implementing its positioning and motion in `TitleScreen`.

## Sources

All four bundled assets are byte-identical copies:

- `assets/moonlit-gate-logo.png` ← `assets/source/ui/splash/moonlit-gate-logo.png`.
- `assets/ruined-gate.png` ← `assets/ui/title/ruined-gate.png`.
- `assets/approved-loading.png` ← `assets/ui/splash.png`.
- `assets/pixel.ttf` ← `assets/ui/fonts/b2bj-sans.ttf`.

The logo and illustration are previously generated project art, reused without
redrawing. Composition, interface icons and animation are HTML/CSS/SVG code.

## Verification

JavaScript syntax passes `node --check gallery.js`. Four asset copies compared
byte-for-byte with canonical files. Chrome checked all five scene layouts,
decoded images, no horizontal overflow, and no logo/menu/footer overlap at
1920×937 and 760×900. Visually reviewed all five cards and enlarged Gatekeeper
and Through the Gate. Verified click selection, arrow-key selection/focus,
replay resetting to Continue, Escape closing, pause/resume, and running camera
animation. Browser warnings/errors were empty.

`browser-check.js` preserves the read-only layout/asset assertions: paste its
contents into the open gallery's DevTools console to repeat them. Reduced-motion
handling is included in CSS but was not tested against an OS preference change.
No gameplay tests run because the game is unchanged.

## Reopen through the local gallery server

From the repository root:

```sh
rtk proxy node /home/ghiankylledomingo/.claude/plugins/ecc/scripts/plan-canvas.js open docs/art-review/main-menu-gallery-2026-10-30/index.html --no-open
```

Use the returned URL. For the full-width gallery, replace `/canvas/ID` with
`/artifact/ID/`. Add `--reopen` only if its review session was previously ended.
The local server expires after inactivity; the saved `index.html` remains usable.
