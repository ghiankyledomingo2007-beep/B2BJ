# Chapter One: slime attacks and barracks — 2026-10-25

**House redraw rejected and reverted.** User wanted the existing environment style, not chunkier pixels or a different palette. Original PNG, generator and manifest are restored; house screenshots below are historical rejected evidence. Slime combat changes remain. Rejected editable export moved out of the project to the task scratch folder.

## Melee slime moves

Only the Outpost REMNANT role changes. At 150px or less it uses its close pounce: 0.6s tell, 0.18s travel and 0.7s recovery. At greater distance (up to 210px) it selects a longer rush: 0.85s tell, 0.32s travel and 1s recovery. Choice stays fixed through the attack; direction locks for the final 0.3/0.4s respectively. Both use existing 520px/s travel and 20 damage. Terrain collision, attack slots, interruption and once-per-attack hit rules remain.

The existing eight-frame attack art gets a shallow/deep crouch, a short pounce arc or low stretched rush, and a landing squash. Rush windups gather existing purple motes. These are runtime poses, not new sprite-sheet frames; the visual hop does not grant airborne immunity or change collision. Reduced effects omits extra body deformation. Spitters, other chapter enemies and legacy scout rules retain their moves.

`slime-attacks.png` shows actual drawWisp poses at controlled simulation times, recentered to compare silhouettes. RemnantAttackTest checks both moves in eight directions, committed choice and heading, travel, harmless tells/recovery, full punish windows, attack slots, interruption and different windup silhouettes.

## Barracks

Reworked the existing editable Buildings.barracks() pixel source into broad timber/slate shapes, thick eave and doorway shadows, larger window recesses, sparse moss and a single damaged roof bay. Native 264x196 canvas, 2x rendering, world placement/anchor and collision footprint unchanged. No generated image service or map-layout change.

Rebuild only this asset with `tools/art/build-art.sh barracks`. Runtime: `assets/props/outpost/set_barracks.png`; updated editable export: `assets/source/outpost/set_barracks_chunky.pixie-dust`. Older editable project is retained. `barracks-in-game.png` is an actual framebuffer with player relocated for inspection.

Validation logs: `/home/ghiankylledomingo/tmp/b2bj-remnant-attacks/`. No complete manual chapter balance or Windows playthrough. Longer-distance rush pressure needs playtesting. All changes local and unpushed.

Verification complete: full 99-test build passed. New barracks passed OutpostArtTest and PixelConsistencyTest (183 sheets); final JAR passed OutpostArtTest and RemnantAttackTest from /tmp after adding the final house PNG. Packaged PNG bytes match the runtime source.
