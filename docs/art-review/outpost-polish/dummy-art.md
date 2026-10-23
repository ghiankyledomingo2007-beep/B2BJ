# Training dummy source — 2026-10-23

Generated with the built-in imagegen tool, using the old dummy and the approved tent
as visual references. The original output is preserved unchanged at
[`assets/source/outpost/dummy.generated.png`](../../../assets/source/outpost/dummy.generated.png).
It is authoring material and is excluded from the game package.

[`ImportedArt.dummy()`](../../../tools/art/ImportedArt.java) crops each source cell,
fits it within 32 × 64 pixels, quantizes it to sixteen earthy colors, and aligns the
lowest opaque row at y=69. The runtime sheet is
[`assets/props/outpost/set_dummy.png`](../../../assets/props/outpost/set_dummy.png):
two 36 × 72 cells, binary alpha, original ground anchor and collision footprint.
The matching editable project is
[`assets/source/outpost/set_dummy.pixie-dust`](../../../assets/source/outpost/set_dummy.pixie-dust).

## Generation prompt

Use case: style-transfer.
Asset type: production pixel-art sprite sheet for a top-down dark-fantasy RPG.
Image 1 is the old training-dummy sheet to replace; preserve its two-cell layout,
narrow upright wooden post and front-facing RPG perspective. Image 2 is the approved
tent, for warm cloth colors, dark stepped outlines and readable hand-pixelled material
clusters.

Redraw TWO small straw training dummies side by side in equal 36 by 72 logical-pixel
cells (overall 72 by 72 logical pixels, enlarged evenly for delivery). They are
scenery, not living characters. Each has a tied burlap sack head with a minimal
stitched face, a stuffed coarse-cloth torso, exposed straw tufts, a rope belt and
wooden crossbar arms. Rough asymmetric sack contours and a few large stitches/patches;
avoid the old smooth oval cylinder body. First dummy relatively intact; second more
worn, with a small torn patch and some loose straw. Both same scale and ground line.

Exact framing: first cell center x=18, second center x=54. Each silhouette within
local x=2..33, y=4..69, post meets the ground at local y=68. Leave transparent padding
around each cell and a clear gap between them. Simple wooden stake foot, no wide
base. Retain narrow footprint.

Strict low-resolution pixel-art style with clean flat 2–4 pixel clusters, restrained
16-color earthy palette, ochre straw, warm tan canvas, dark brown wood and ink
outlines. Materials must read at 2x game scale. No gradients, blur, airbrush,
photorealistic texture, fine noise, 3D facets, glossy shading or antialiasing. No
ground, cast shadow, scenery, labels, UI, weapons or extra objects. Genuine
transparent background. Deliver one square two-cell sprite sheet.
