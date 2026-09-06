# Outpost asset batch — verification evidence

2026-09-06. User asked to use all 27 remaining PixelLab generations. Asset
integration stays within the existing Java renderer, animation states and tests.

Journeys: idle without repeated four-pose wobble or foot drift; read the Warden's
raised-arm wind-up before damage; see a distinct dissipating Blob hit; walk/dash
against fitted props without oversized invisible colliders.

## RED / GREEN

Command for each gate: `./build.sh` (Java assertion runner, no package manager).

1. New idle timing test failed compilation because `SlimeAnimation.idleFrame()`
   did not exist. Implemented eight-frame timing, packed reviewed native frames,
   and added the matching renderer selection. GREEN: all 17 test programs.
2. Warden visual anticipation check failed against old sheet: raised-arm region
   contained only 37 / 48 pixels in first / last tell pose. Repacked selected
   reviewed poses into the existing 4×3 sheet. Same test passed, all 17 GREEN.
3. Blob hit test failed with `assets/effects/blob_hit.png must load`. Packed nine
   reviewed frames and reused the existing impact renderer with palette-specific
   sheets and timing. Same test plus all 17 programs GREEN.
4. Added cardinal contact coverage for trees/barrels. Initial barrel fixture
   walked through a neighbouring barrel; corrected fixture to approach each
   cluster from the outside. No collision logic weakened. All contact tests GREEN.

## Guarantees checked

- Eight unique 48×48 idle poses; limited palette; transparent background;
  visible ground at source row 47; eighth frame reached and loop wraps correctly.
- Warden anticipation visibly raises arms; existing telegraph/damage timings and
  target locking unchanged and covered by Guardian/Combat/Game tests.
- Nine distinct transparent Blob hit frames with non-increasing visible coverage.
- Tree/barrel/native prop approaches stop at feet, within existing movement-step
  tolerance. Walking/dash, depth ordering, graph and actual foot-radius route checks pass.
- Runtime sprite paths present in the JAR and load when cwd is outside the repo.
- Four final-JAR screenshots inspected; no oversized helmet or rejected props packaged.
- Shell syntax and `git diff --check` pass. No new dependencies or credentials.

All 27 job IDs completed; local inventory verified 155 PNG results. Provider
balance settled at 0 remaining / 40 used, $0 purchased credit.

No checkpoint commits were made: the shared worktree contains extensive prior
uncommitted work, preserved rather than swept into this task's commits. Evidence
is retained here. Coverage instrumentation is not installed; 80% line coverage
was not measured or claimed. No full unassisted boss fight, live audio mastering,
or Windows installer execution. Biome remains unfinished.
