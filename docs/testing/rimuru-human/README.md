# Human animation review — 2026-09-16

- [Running preview](run-preview.gif): all four directions, 16 frames, approximately
  the game's 571 ms loop (GIF timing rounds to 10 ms).
- [Human in the Outpost](human-south.png): new body and matching HUD portrait.
- [Attacks](attacks-contact.png): three combos; contact is column 3.
- [Fence occlusion](human-behind-fence.png): scenery covers the lower body.
- `run-*-contact.png`: all 16 poses captured through game movement and rendering.
- `action-*.png`, `transform-*.png`, `revert-*.png`: staged action/transition samples.

All 66 test programs and packaged asset validation passed with `./build.sh`.
The preview tools are `PreviewRainoray` and `PreviewMotionPolish run`, using
`-Db2bj.previewDir=docs/testing/rimuru-human` and `-Djava.awt.headless=true`.
These fixtures verify rendering and movement paths; they are not an unassisted
playthrough or a measurement of desktop frame pacing.

For the presentation, explain animation as: **input chooses an action; elapsed
time chooses its picture; the renderer draws that picture**. Hair, coat and
sword motion are in the pictures. Combat rules still decide actual hits.
See [FILE_MAP.md](../../../FILE_MAP.md) for the code locations.
