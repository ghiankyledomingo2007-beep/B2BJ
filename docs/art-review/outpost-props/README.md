# Grounded prop review — 2026-09-06

User report: Blob appeared to sit on a barricade. The renderer painted all
barriers before every actor; physics already used the character's feet. Fixed
standing-object/actor order by ground contact rather than changing character size.

New PixelLab props were inspected enlarged with nearest-neighbour pixels and in
real headless game renders. No generated image was stretched to match the old
blockout rectangle. `RuinedOutpostMap.Prop` holds native-pixel anchors and physical
footprints together; collision is 2× those dimensions and drawing is exactly 2×.

| Runtime image | Native size | Ground anchor (x,y) | Runtime footprint | Review |
| --- | --- | --- | --- | --- |
| barricade.png | 96×64 | 48,56 | 152×24 | Wood posts/crossbeam. Moss extends outside physical posts. |
| cot.png | 112×64 | 56,52 | 160×44 | Footprint follows four legs, not transparent canvas padding. |
| standard.png | 48×96 | 22,90 | 40×24 | Small stone base; tall cloth remains non-solid overhead. |
| wall.png | 96×64 | 48,47 | 164×24 | Continuous frontal masonry; low silhouette distinct from palisade. |

All four have transparent margins. Native visible bounds, respectively:
`[4,8..91,59]`, `[15,10..97,52]`, `[9,5..38,90]`, `[4,19..91,49]`.
No placeholder platform or procedural wall is drawn underneath these sprites.
The first wall candidate had separated diagonal pieces and was rejected;
its exact job ID is retained in the [ledger](../../ruined-outpost-animation-plan.md).

Screenshots: `behind-blob.png`, `front-blob.png`, `behind-blade.png`,
`front-blade.png`, `barracks.png`, `standard-yard.png`, `north-watch.png`.
Blade front/rear screenshots retain the game's short transformation effect;
the render regression separately checks static Blade occlusion without that effect.
These are scene fixtures, not evidence of an unassisted full playthrough.

Recheck assets with `java tools/InspectProp.java <png> [4x-preview.png]`.
Run `./build.sh` for depth, approach collision, reachability and packaged-asset
checks. Linux and Windows packaging include only the curated outpost prop folder,
not review screenshots, unused pillars, or rejected art. The Windows workflow
was updated but not executed locally.

Remaining art includes earth banks, long/tall procedural obstacles, supply-lane
props, ground equipment and environmental storytelling. This pass does not
claim the biome is finished.
