# Blob to Blade — five loading-screen and logo concepts

Owner selected the **Moonlit Gate title logo** on 2026-10-30 for the minimal boot splash.
The installed splash uses the logo on plain navy; the five original mockups remain available.

Created 2026-10-30 as a local design gallery. Open `index.html` directly in a browser; all assets
are relative, with no build step or external CDN. The game and its current boot splash were not edited.

Five animated HTML loading mockups, each paired with a separate transparent PNG logo:

1. **Quiet Star:** sparse boot screen, droplet/star mark and thin turquoise progress line.
2. **Raphael Seal:** gold title, slowly rotating seal and Raphael loading message.
3. **Moonlit Gate:** chapter arrival using the game's ruined-gate art, slow drift and corner loader.
4. **Reincarnation:** slime/blade title contrast with Rainoray's existing eight-frame idle animation.
5. **Ink & Ember:** brush-cut title, red slash and an anime episode-card layout.

Use the filter buttons to compare loading screens or logos. Click any preview for an enlarged
live view. Pause motion, use Escape to close the enlarged view, or save favorites in browser-local
storage. Logo download links preserve the full transparent PNG originals. Loading mockups remain
live HTML/CSS compositions rather than static exported images. Reduced-motion preferences are honored.

## Sources

The five logo concepts were generated with the built-in imagegen tool. Full base prompts, directions
and generated-file provenance are in `concepts.json`. The Raphael logo received an additional
flattening edit, recorded in `raphael-revision-prompt.txt`. These are original title directions
influenced by anime/JRPG visual language, not copied official anime logos. Raster concept lettering
may need cleanup/redrawing before final in-game production use.

Loading layouts, motion, seal and controls are local HTML/CSS/JavaScript. Existing game references:
`assets/ui/title/ruined-gate.png`, `assets/characters/slime/slime_idle.png`, `assets/ui/fonts/b2bj-sans.ttf`.
Copies are bundled under this gallery's `assets/` directory. No game source, runtime asset or JAR changed.

## Verification

- Five loading mockups and five logo panels present; every displayed image decoded in Chrome.
- Inspected normal/enlarged screens; adjusted Quiet Star and Raphael layout to separate logo/loading art.
- Loading/logo filters, motion pause and enlarged-view close checked in Chrome. No browser warnings/errors.
- JavaScript syntax check and `git diff --check` passed; local asset references checked.
- No game playthrough or final native-resolution production asset cleanup: this delivery is a concept gallery.

Opened through the installed plan-canvas local server, with the full-width artifact shown in Chrome.
Local review route: `http://127.0.0.1:4517/canvas/6378c6432e88`.
Direct gallery: `http://127.0.0.1:4517/artifact/6378c6432e88/`.
If the loopback server expires, open this folder's `index.html` directly.
