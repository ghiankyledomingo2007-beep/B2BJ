# Rainoray character refit — 2026-10-24

Ghian requested a better human character and chose to keep the porcelain mask.
The white hair, navy outfit, sword and existing action poses remain.

- Shorter upper head gives the silhouette a rounder crown and less stretched proportions.
- A solid ivory mask carries paired black hooks, cheek curves, a jaw line and one red dot,
  following the supplied [mask reference](../sprite3d-run/source/rainoray-editable/mask-reference-20261024.png).
  The markings are part of the removable mask mesh and do not fight its old texture.
- Broad navy coat panels, a dark opening, muted edge trim and a small clasp replace
  the busy original coat texture. Body geometry and skinning are unchanged.
- Two broad boot tones separate the feet from the coat and remove scattered boot highlights.
- All eight directions were rebuilt for all 19 actions: 14 body sheets, four matching
  effect sheets and both 16-frame transformations. Shared framing keeps the side thrust inside its cell.
- The editable project contains 1,344 frames and 152 tags. Its hidden **Before polish**
  layer holds the preceding eight-way body art. Editor attack durations and contact
  holds now match the current game; gameplay timings did not change.

[Before and after](before-after.png) · [Eight-way animation samples](animation-review.png)
· [Game-renderer preview](game-preview.png) (controlled, temporary camp state)
· [Native editor project](../../../assets/source/characters/rainoray/Rainoray-Human.pixie-dust)

The [generated design reference](design-reference.png) guided the initial proportions.
The supplied mask reference supersedes that illustration's forehead crest. Runtime
art is rendered from the edited existing model so every action stays compatible.
The coat now uses authored material panels; the original hand and hair geometry
remain. [Built-in imagegen prompt](design-prompt.md).

## Source and recovery

[Editable model](../sprite3d-run/source/rainoray-editable/Rainoray-Body-and-Separate-Sword.blend)
and [one-time conversion](../../../tools/sprite3d/refit_appearance.py). The conversion
requires a fresh output and refuses to run twice. It verifies the original model
bytes, 20-bone skeleton, body weights and foot geometry remain intact as applicable.
Only the new output receives appearance changes. All procedural poses are retained.
The [mask-and-coat follow-up](../../../tools/sprite3d/polish_appearance.py) also checks
that every body vertex, skin weight and bone remains unchanged.

Original model, runtime PNGs, native project and renderer scripts were copied and
hash-verified under `~/tmp/rainoray-refit-20261024/before/`. The previous JAR and editor
scripts are there too. [Installed hashes](installed-assets.json).
The follow-up's preceding refit is preserved at `~/tmp/rainoray-mask-coat-20261024/before/`.

Regenerate into a fresh folder with `tools/sprite3d/build_sheets.py`, run the existing
cluster cleanup once on those fresh renders, then rebuild transforms from the cleaned
idle. Review before installing. The existing native editor importer/exporter handles
all directions and preserves the packed runtime layouts.

## Validation

- No clipped body frames or repeated poses in the final rendered strips; transformation frames fit too.
- Front idle retains exactly one red mask pixel in all eight frames. All installed PNGs keep binary alpha and their existing layouts.
- Blender checks cover planted guard, combat continuity, sword carry, contact arcs and hidden sword/mask behavior.
- Native editor round-trip: 18 sheets match runtime pixels, all 1,344 frames/152 tags and current timings pass, repeated export refuses overwrite.

- Full Java 17 build: all 92 test programs and packaged asset checks passed; `git diff --check` passed.
- Opened a muted, temporary Blade-form preview on the laptop. The normal campaign save hash stayed unchanged; no existing game window was closed. Launch log was empty.

No full manual campaign playthrough or individual-finger redraw was performed.
