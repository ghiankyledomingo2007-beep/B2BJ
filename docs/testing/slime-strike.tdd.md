# Slime strike feedback

User report: Blob attack barely changes the character and looks like a line.
Cause: attack reused dash timing/poses; the shared Blade trail drew a straight
line on combo three. Right-click is not bound to attack; left-click/hold is.

Implemented with the existing Java renderer (Ponytail/TDD), no generation jobs:

- Front attacks use the authored attack row, not the dash row.
- Lean, contact squash and recovery share the controller's existing 80ms/120ms
  hit windup; damage, range and cooldown values are unchanged.
- An attached, outlined gel appendage replaces Blob's Blade-style line/arc.
  It renders into a native pixel buffer at exact 2x, behind its own body and in
  the player's depth-sorted draw, not as an overlay over every obstacle.
- The first two jabs bend in opposite directions; the third has a larger contact
  shape and the existing longer windup. These are procedural pose variations,
  not three newly generated character sprite cycles.
- Normal on-hit splashes, sound and hit feedback still come from actual damage
  events. Missing an attack does not synthesize a hit.

## RED / GREEN evidence

`bash build.sh` uses the project's Java assertion runner, not npm.

1. New timing tests failed compilation: missing `attack(int)` and
   `strikeExtension()`. Implemented phase timing and shared windup lookup.
2. Scene review exposed a floating appendage above the flattened body. Added
   pixel-connectivity regression; RED: `strike must connect to the squashed body,
   not float above it`. Moved its root inside the compressed body and tapered the
   neck. Same test GREEN in four directions.
3. Added combo-shape regression; RED: `second jab needs a distinct bend`.
   Opposite curve directions made the same test GREEN; third-hit coverage is
   also visibly greater than the first.

Final gate: all 18 Java test programs pass. Slime rendering tests exercise actual
left/right mouse input, release, held-repeat, cardinal aim, pre-hit windup, broad
contact, body-to-tip connectivity, shared 2x pixel blocks, no generic Blob trail,
retraction and combo variation. Existing collision/combat/depth tests still pass.

Scene fixtures are in `docs/art-review/slime-strike/`: four directions, windup,
recovery. Reproduce with `PreviewOutpost 1 output.png blob-east 0.12`, compiled
against the built JAR. Fixtures are muted and not a full live combat evaluation.

No PixelLab generations spent. No new dependency, source-sprite resampling,
checkpoint commits or publishing. Existing dirty worktree preserved. Numeric
line coverage is not instrumented or claimed. Fine art polish and a full human
playthrough remain outstanding.
