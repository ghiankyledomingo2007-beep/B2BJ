# Independent Claude pass review and slime input proof

Production audit: **65/100, risky as a finished-biome handoff**. This is a reviewable
prototype, not final visual approval. Confirmed inconsistent prop collision,
obvious terrain repetition, missing shelter behavior, and no end-to-end live
playthrough. The score is a prioritization judgment, not a measured quality metric.

## Exact surfaces checked

- Main/combat: `2edf3c4`; packaged `build/B2BJ.jar` dated 2026-09-07 23:16:40 +0800.
- Claude environment branch: `outpost-art-fable`, `64f4511`; base `7d3d2bd`.
- Claude's worktree was clean. Nothing from it was merged or changed during review.
- Read all changed production Java, changed tests, generation-helper changes,
  terrain composer and preview harness. Inspected all twelve room overviews,
  reference Blob scene, prop scale sheet and four generation ledgers. This does
  not mean every rejected generation received a new individual art review.
- Independently compiled Claude's source and tests with Java 17 `-Xlint:all`,
  then ran every `test/*Test.java` using assertions and a 256 MB heap: **52 pass**.
- Independently ran its three offline PixelLab-helper tests: **3 pass**.
- Ran its packaged asset-loading check from `/tmp` against its built JAR: **pass**.
- Exact-value secret scan: **281 changed text files**, **7 known supplied keys**,
  **zero matching files**. This does not clear the reported terminal-log exposure.

## Findings, highest priority first

1. **Same art, different collision.** `RuinedOutpostMap.java:15` gives the
   muster-ground weapon rack a **96x48** world-pixel box; the identical sprite at
   the same 2x scale is **52x14** in the barracks (`Decoration.WEAPON_RACK`).
   `ClaudeMapProbe` confirms both paths load the same asset. The larger box was
   retained to satisfy tests tied to the old blockout, not calibrated to new art.
   Fix: choose the actual rack footprint and decouple old combat fixtures from
   the authored room's first barrier.
2. **Shelter behavior is unfinished.** Both lean-tos have `null` colliders,
   including their visible support posts; `ClaudeMapProbe` confirms rooms 7/10.
   Weather has no roof occlusion. Passing underneath is intentional, but passing
   through posts and raining through roofs still need separate treatment.
3. **Terrain is still visibly repetitive.** The same full-stone pattern repeats
   at 64 world pixels. Repeated scalloped paving borders are obvious in the
   reference scene and all paved rooms. These are code-composed sheets, not
   accepted PixelLab terrain. Add material variants and improve transition
   shapes before calling the art consistent/final.
4. **The approved breach was not entirely preserved.** Prop positions remain,
   but `stone()` now ORs the shared straight roads into room 1 and applies its
   generic crater. **44 of 540 terrain masks changed** versus the previous
   reference formula. Decide this deliberately; don't call it unchanged.
5. **Generation batches are not restart-safe.** In `pixellab-batch.py`, `submit`
   saves the aggregate ledger only after the whole loop. A timeout after earlier
   successful requests leaves jobs only in per-request replies; rerunning the
   batch submits everything again without deduplication. Save after each job
   and reconcile existing submissions before another spending run.
6. **Credential exposure was reported, not independently cleared.** Claude's
   handoff says a failing offline test printed a live header. Its environment
   isolation fix passes now, and the changed tracked files contain none of the
   known keys. The affected key should still be rotated; no credentials or
   configs were modified by this audit.

## Room-by-room visual check

| Room | What actually changed | Remaining concern |
| --- | --- | --- |
| Rain Ditch | Reeds, wheel and two bare trees; east approach track | Still a mostly empty rectangle; drainage reads as repeated reed clumps, not terrain |
| Broken Palisade | Mud-base fences; existing equipment and aligned gap retained | Repeating paving; 44 terrain masks changed despite reference-preservation intent |
| Muster Ground | Weapon rack, arms, infirmary waypost | Inconsistent rack collision; parade space remains generic |
| Barracks | Broken-L walls, two cots, rack and roof debris | Landmark is clearer, but isolated in a corner away from most combat; mixed wall-end orientation |
| Standard Yard | Paved ring and fallen equipment around standard | Repeated road texture dominates; canvas asset reads as wood/roof debris |
| Supply Lane | Barrel cluster, cart, wheel and sack | More purposeful grouping; cart still reads ornate and colorful beside muted military props |
| North Watch | Platform, broken parapet and cold brazier | Platform is intact rather than collapsed; broad empty area remains |
| Captain Camp | Lean-to, tree, bedroll and cold brazier | Bedroll largely hidden by shelter; support collision and rain shelter missing |
| Inner Court | Widening paved court and east-facing defense debris | Largest obvious repeated paving field; little encounter-specific structure |
| East Gatehouse | Two slate piers, rubble, open boss floor | Clearance tests are not a live Warden fight; decorative ruins alone do not add combat twists |
| Field Infirmary | Shelter, cots, chest, bandage and stretcher | Reads more clearly as treatment area; roof/post behavior unfinished |
| Signal Tower | Tower footing, fallen mast, dead beacon | Stronger identity; needs walk-around/depth checks around the ring, not just spawn clearance |

## Slime: implemented, but visual improvement was overstated

`AttackInputProof.java` was compiled against **the existing main JAR**, not a
new source build. It invokes B2BJ's actual Swing mouse/key handlers and steps
the real game. No water projectiles are injected or faked.

Output:

```text
mouse hold: CUT, damage=1, frame=19
mouse hold: RETURN_CUT, damage=1, frame=67
mouse hold: FINISHER, damage=2, frame=120
keyboard back-dash + mouse: COUNTER, damage=1
Loaded B2BJ from .../Projects/B2BJ/build/B2BJ.jar
```

The changes are wired into the playable build. But `SlimeAnimation.attack()`
still selects one shared cast clip; `drawSlime` does not select separate combo
body animations. Projectile angles, small opposite curves and two overlapping
finisher arcs are subtler than a new attack animation. The existing body pose
is effectively the same across all three hits. The owner's readability concern
is valid; these changes should not have been presented as a full visual overhaul.

See `mouse-combo-proof.png` and full `mouse-hit-1/2/3.png`: actual JAR renderer
captures from the mouse-held sequence. They are deterministic input fixtures,
not a recording of a human playthrough. Next visual work should be distinct
body-cast timing/poses and a clearly different finisher, not more tiny rotations.

## Spending cross-check

Four ledgers contain 59 records: 8 texture results, 12 cleanup entries (11
downloaded, 1 failed), 29 landmark results and 10 retry results. **58 downloaded
jobs** is consistent with Claude's reported **58 generations spent**. This is
not a fresh balance query. Its reported 25 remaining applies only to its three
allocations and excludes the newer shared key file.

## What remains unverified

- Full player-controlled traversal and combat on Claude's new maps.
- Actual Warden fight with piers, summon pressure and revised boundaries.
- Laptop frame pacing and reduced-effects performance.
- Numeric line/branch coverage; there is no instrumentation in this project.
- User visual approval, and all rejected art candidates individually.
- Merge behavior with the newer main-branch combat changes.

Recommendation: keep Claude's branch separate until collision and art-readability
issues are addressed, then merge in a controlled pass and run combined tests.
The latest main game was reopened for the owner; no production source was
changed, no credits spent, no branch merged, no agent stopped by this audit.
