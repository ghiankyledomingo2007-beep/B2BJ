# Ruined Outpost arena and dressing

## Scope

The requested journeys were: recognize a new boss phase, escape its clearly committed attack, and explore more richly dressed outdoor rooms without losing their open routes. This pass adds one phase-two attack and 26 authored dressing placements. Upright carts and braziers have small solid feet; flat shield caches and rubble remain walkable. It does not add walls, rooms, health, damage multipliers, loot, or dependencies.

## Arena behavior

At half health, every fourth Warden attack becomes a fissure. Successive fissures alternate between horizontal and vertical, fixing their center on the player's position when the warning begins. The full 1,152 × 84 world-pixel rectangle is exposed by `Guardian.fissureBounds()` throughout its 1.1-second warning and 0.32-second active interval. The renderer must use those exact bounds, not a decorative circle. It should use `telegraphDuration()` rather than the original fixed warning constant.

The existing `hits()` and `impactNumber()` path handles damage, so the fissure shares the existing once-per-impact rule, invulnerability behavior and hit response. Recovery, approach movement and all phase-one attacks remain intact. Death removes the hazard immediately. There is no persistent floor damage after the visible attack ends.

## Dressing contract

`RuinedOutpostMap.dressing()` returns ground-anchor positions and a `Decoration`. Render at 2× and depth-sort at the ground anchor. Existing `Prop` collision footprints are unchanged. `Dressing.obstacle()` returns a calibrated foot rectangle for carts and braziers, or null for flat wreckage. The map caches these rectangles and checks them in `waterBlocked()` and `clearLine()`; actor motion, projectiles, absorption sight lines and combat queries therefore share the same bases without turning the upper sprite into an invisible wall.

| Decoration | Asset | Native size | Ground anchor |
|---|---|---|---|
| CART | `assets/props/outpost/broken-cart.png` | 80 × 80 | 40, 70 |
| SHIELD_CACHE | `assets/props/outpost/shield-cache.png` | 64 × 64 | 32, 54 |
| BRAZIER | `assets/props/outpost/brazier.png` | 64 × 64 | 32, 56 |
| RUBBLE | `assets/props/outpost/rubble.png` | 64 × 64 | 32, 54 |

Each room has two independently placed pieces, except the arena's four. The arena's braziers sit above the fighting corridor; shield wreckage and rubble sit below it. The original broad paths, banks, doors, and boss collision footprint remain unchanged. Art generation and visual integration are handled separately from this logic pass.

The selected source PNGs were visually inspected before choosing collision sizes. Cart feet use an 80 × 24 world-pixel rectangle centered 6 pixels right and 20 pixels above its render anchor, beneath the wheels rather than the handle or grass. Brazier feet use 48 × 16 world pixels centered at its anchor, not its flame or full bowl silhouette. All 26 placements stayed in place after path and escape checks passed.

## TDD evidence

Before implementation, compiling `ArenaTwistTest` against `Guardian` and `RuinedOutpostMap` failed with 12 missing-symbol errors for the intended `FISSURE`, `fissureBounds()`, `telegraphDuration()`, `Decoration`, and `dressing()` APIs. This was the compile-time RED gate. An earlier whole-source compilation also encountered a concurrent renderer edit, so the RED gate was repeated with only the relevant source targets to isolate the intended failure.

After implementation, all five focused programs passed:

```sh
arena_build=$(mktemp -d /tmp/b2bj-arena-check.XXXXXX)
rtk proxy javac --release 17 -Xlint:all -sourcepath src -d "$arena_build" src/Guardian.java src/RuinedOutpostMap.java test/ArenaTwistTest.java test/GuardianTest.java test/GuardianMobilityTest.java test/GuardianPressureTest.java test/RuinedOutpostMapTest.java
rtk proxy java -ea -cp "$arena_build" ArenaTwistTest
rtk proxy java -ea -cp "$arena_build" GuardianTest
rtk proxy java -ea -cp "$arena_build" GuardianMobilityTest
rtk proxy java -ea -cp "$arena_build" GuardianPressureTest
rtk proxy java -ea -cp "$arena_build" RuinedOutpostMapTest
```

| Guarantee | Evidence |
|---|---|
| Phase one retains the old move set | 50 seconds of healthy-boss simulation |
| Four successive fissures alternate orientation | `ArenaTwistTest` |
| Warning is harmless, fixed and at least 1.1 seconds | Target changes during a live warning; bounds remain equal |
| Active rectangle matches the exposed geometry | Center hits; perpendicular safe point misses |
| One impact identifier per active interval | Impact counter remains unchanged while active |
| Hazard ends with recovery and death | Bounds become null and hits return false |
| Escape exists near corners, spawn and door mouths | Eight scripted positions, both orientations |
| Terrain does not trap a stationary player in a lane | Every traversable 32-pixel arena sample, both orientations, with existing boss footprint present |
| All rooms have varied dressing away from routes | Four types; every anchor over 180 pixels from spawn and doors; original door reachability tests pass |
| Upright feet block actors, projectiles and combat rays | `isBlocked`, `waterBlocked`, `clearLine` and `clearWaterLine` checks through every solid base |
| Flat wreckage and upper sprite space remain walkable | Shield/rubble anchors and points above solid bases remain unblocked |
| Existing boss movement and map paths survive | `GuardianMobilityTest`, `GuardianPressureTest`, `RuinedOutpostMapTest` |

Final art review exposed upright cart/brazier bases that should not be walk-over. A new runtime RED assertion failed with `upright dressing feet must block actors` before the collision change. After adding the shared base checks, the same test and all four existing boss/map tests passed again. This rerun includes the arena's new solid braziers in its full 32-pixel escape-grid sweep.

The exhaustive grid initially rejected (224, 96) because a straight perpendicular move met an irregular bank. Inspection showed an L-shaped walking route. The strengthened test accepts such routes only when a bounded cardinal search reaches a fully clear lane offset within 192 pixels: under one second even at the 208 px/s human-form speed. This tests reachability, not human reaction time. It does not promise escape from every dynamically overlapping enemy arrangement or while deliberately ignoring the warning.

No coverage instrument is configured for these plain Java assertion programs; no percentage is claimed. Renderer appearance, final asset dimensions and full-build integration require the separate visual/build pass. Checkpoint commits were intentionally left to the parent task because this agent does not own repository staging or publishing. Ponytail kept the feature inside the existing boss damage system; no second hazard manager or dependency was added.
