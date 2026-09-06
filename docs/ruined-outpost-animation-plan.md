# Ruined Outpost animation / effect review

Updated: 2026-09-06. Supersedes the earlier three-Wisp shrine plan.

## Runtime scale

- Tile: 32×32 source pixels rendered at 2×.
- Blob / corrupted Slime / Wisp: 48×48 cells, 96×96 runtime.
- Blade V2: 48×64 cells, 96×128 runtime; floor aligned with Blob.
- Warden: 64×64 cells, 128×128 runtime, intentionally a larger enemy.
- Nearest-neighbour rendering. No extra enlargement to fill empty frame margins.

V2 is the established identity anchor. Existing action sheets are usable
prototype art, not a claim of finished animation quality or fresh owner approval.
V3/V4 and unrelated shrine assets remain outside active rendering.

## Current work and PixelLab ledger

Starting account check: 36 of 40 trial generations remaining, $0 purchased credit.

| Job | Cost | Decision |
| --- | ---: | --- |
| `318930f7-b7dd-42fc-bf6e-2b4d7a33953a` — V2-anchored south thrust | 1 | Rejected. Looks like stepping/jogging; no readable thrust. Nine frames retained under `docs/art-review/blade-thrust/`, outside runtime packaging. |
| `69c23dfc-1014-4d48-80b6-7e92f91887b1` — 64px gold impact pilot | 1 | Rejected. Too small and low on the canvas to read as a centred hit. |
| `3ab783e9-8d86-4efc-8eee-ca76ef57344b` — corrected 32px impact | 1 | Centred starburst retained as the animation anchor. |
| `f3fe9399-4ae4-45e3-bb86-0c728f155385` — four-frame dissipation | 1 | First three returned frames selected; final two rejected for opaque backgrounds. Native 32px frames play at 12 fps and fade out, rendered at 2× for Blade hits only. |

Historical balance after the four effect/animation jobs settled: **32 of 40** generations remained,
8 used in the account total, $0 purchased credit. Four generations used in this
work sequence, including rejected pilots.
No extra purchased credits or Pro jobs were used in that batch.

### Grounded outpost props

The owner explicitly authorizes needed PixelLab generation without a project
budget cap. The provider's per-token generation allowance is not a spending target
or a reason to accept poor art. No credentials or provider limits were changed.

| Job | Cost | Decision |
| --- | ---: | --- |
| `d16dd5d7-f3ad-478e-99c3-3713c6d0768d` — 96×64 palisade | 1 | Integrated. Weathered posts/crossbeam, no platform; native 2×, 152×24 physical footprint. |
| `93a72c08-4eaa-4370-b835-00a4820d6b92` — first stone wall | 1 | Rejected. Separated isometric pieces create a misleading walkable-looking gap. Not packaged. |
| `30752175-a658-4107-9c3f-ce269d62200d` — 112×64 cot | 1 | Integrated in barracks/infirmary. 160×44 footprint fits legs, not transparent canvas. |
| `2e820ad4-6e49-44af-8dff-d9c4eb89b8b0` — 48×96 standard | 1 | Integrated in tutorial yard. Only 40×24 base collides; cloth does not. |
| `ac39dcad-1fe3-4aa8-8f1a-c35e0b434811` — corrected 96×64 wall | 1 | Integrated in North Watch/Inner Court. Continuous frontal masonry; 164×24 footprint. |

Historical settled provider balance after the prop batch: **27 of 40** generations remaining, 13 used,
$0 purchased credit. Five generations used in this prop batch. This is inventory,
not a project budget cap. See [prop review](art-review/outpost-props/README.md).

### Owner-requested full allowance batch

The subsequent explicit instruction was "use it all". All 27 remaining
generations were spent on completed Ruined Outpost jobs. Final live balance:
**0 remaining / 40 used**, $0 purchased credit. No pending jobs or new purchases.
All 155 PNG outputs and exact IDs are preserved in
[27-job review](art-review/outpost-batch-27/README.md) and its JSON ledger.

Runtime now includes an eight-pose Blob idle, a nine-frame teal hit, clearer
Warden anticipation/slam/recovery, supply barrels and bare trees. Other candidates
remain staged or rejected; spending did not bypass visual/floor/perspective review.

The curated strip is `assets/effects/blade_hit.png` (96×32). Reproduce packing with:
`java tools/PackEffect.java docs/art-review/blade-hit assets/effects/blade_hit.png`.
Packing validates dimensions, transparency and distinct frames without resampling.

## Next animation work

1. Finish the side sword cycle. Reviewed south/north cuts are now integrated at
   native 2× with transparent sword clearance. Side mirroring is corrected, but
   the old three-frame side attack remains. Audit source frame boundaries and
   floating pixels before further generation; a complete matched set is not done.
2. Only generate the missing motion from that stable anchor. Review at native
   resolution and in combat, with fixed floor and silhouette.
3. Keep Blob impacts wet/teal and Blade impacts sharp/gold; keep danger tells red.
   Prefer existing authored Slime effect rows where they already fit.
4. Reject changing equipment, disappearing swords, jogging-as-attack, unwanted
   flashes, frame drift and duplicate loops. Do not use a candidate just because
   it cost a generation.
5. Validate a full fight with sound before calling the biome polished.

### Fresh allowance — direction pass

Owner authorizes needed spending and asks to be notified when the 40-generation
allowance runs out. Final live balance for this pass: **12 remaining / 28 used of
40**, $0 purchased credit, no pending jobs. Nine jobs produced 71 local PNGs;
eight ordinary jobs cost 8 total and one seven-frame Pro correction cost 20.
New side candidates failed visual review, including the expensive correction.
Only the previously reviewed front/back cuts were integrated; the facing fix and
native-scale regression checks also shipped. See the exact requests, rejection
reasons and outputs in [direction review](art-review/blade-direction-pass/README.md).
