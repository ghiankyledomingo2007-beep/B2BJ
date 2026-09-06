# Immersion, ranged remnant and larger Warden — 2026-09-07

Owner-approved follow-up to the [combat repair](../combat-repair-2026-09-06/README.md).
Journeys derived from the request: feel when a hit lands, collect nearby animated
Ichor smoothly, fight an enemy that does not repeat the same lunge, and face a
visually larger Warden. First biome only; no later-biome content or finished-game claim.

## Implemented

- Sprite-based gold strike/armor feedback, two teal hurt variations, gold pickup,
  violet hostile projectile/impact, sparse stone debris. Short animations fade
  instead of persisting. Bounded64 active effects; reduced-effects mode remains.
- Hurt feedback now appears on the **player**, not at the attacker/event-source
  position. Pickup no longer borrows the player's blue attack splash.
- Ichor animates and attracts inside160px with acceleration1200px/s² and a420px/s
  speed cap; collected within24px. Terrain/closed-gate clearance is required.
  Full meter leaves drops alone, overflow remains, death and pause cannot collect.
  Tiny Blade top-ups do not fire a sound/effect every physics tick.
- Spitter:3HP, bronze silhouette, violet shots; introduced one at a time after the
  safe transformation yard (sections5,6,8, optional11). Scouts and guards retained.
  Ranged tell0.75s, final0.25s aim lock, stationary release0.18s, recovery1.4s.
  Three committed non-homing globs at250px/s,1.6s lifetime, radius10, spread±0.20rad.
  Shots respect cover, pause, dash/hurt immunity, death and room resets. Cap24.
- Firing clearance fits actual projectile radius. Retreat behind cover no longer
  blocks routing; a floating-point160px boundary no longer stalls the AI forever.
- Fixed remnant/Spitter east-west mirroring with an actual sprite-render test.
- Warden now192px (integer3×), retaining its ground anchor with a fitted112×52 foot
  collider. Same240HP, damage2, committed tells and attack ranges. A wider60px
  terrain-clearance probe protects its larger body.

The Spitter is an implementation proposal within the owner-approved enemy-variation
request, not a newly discovered GCD creature. Magnetic pickup/overflow preservation
is an explicit change from the previous walk-over/indivisible-drop behavior.
Existing synthesized sound cues were reused; no new music or mastered audio claimed.

## Regression evidence

**`./build.sh`:34 Java assertion-test programs passed**, Java17 lint compilation
succeeded, and packaged assets loaded successfully from outside the project root.
**`python3 tools/test_pixellab_call.py`:2 passed.** `git diff --check` passed.
No numeric coverage claim: coverage instrumentation and real desktop input/audio
testing were not run. These are production-logic tests, not a browser application.

| Guarantee | RED observed | GREEN check |
| --- | --- | --- |
| Smooth nearby pickup, arrival, pause, overflow, blocked paths | Drop stayed at original position; later per-tick event spam | `IchorMagnetTest` |
| Hurt effect located on victim | Effect used distant attacker coordinates | `HurtFeedbackPositionTest` |
| Distinct ranged AI/finite cadence | Missing SPITTER/counter; locked-gate failure | `SpitterBehaviorTest` |
| Actual projectile clearance and cover progress | Four futile corner volleys; then zero releases/retreat stall | `SpitterBehaviorTest.projectileFitsFiringLane` |
| Finite/validated projectile; dodgeable fan, no shotgun triple damage | Missing projectile class/game API | `EnemyProjectileTest`, `SpitterCombatTest` |
| Correct hurt/pickup sprite and clean packaged art | Missing effect-kind field and new assets | `ImmersionRenderingTest` |
| West/east art follows real facing | Actual west-facing render was mirrored east | `RemnantFacingTest` |
| Larger crisp Warden without stats buff | Previous2× scale failed | `GuardianScaleTest`; existing mobility/pressure tests also pass |

Existing movement, depth sorting, water combat, reversion, summons, story, audio,
packaged-asset and UI input regressions also passed. Skills: TDD supplied observed
RED/GREEN checks, Ponytail kept changes dependency-free and reused sprite tools;
security review kept the key in a temporary process environment with redaction tests.
No commits/checkpoint commits were made in the mixed user worktree. No config changes,
pushes, releases, purchases, remote publication or unrelated cleanup.

## Input-driven gameplay scenarios

[`AuditImmersion.java`](../../../tools/AuditImmersion.java) drives fresh routes
without teleports, forced kills, injected resources or healing. Normal tutorial
Ichor and camp healing still apply. Isolated fixtures set actors/resources only
before the run, never during combat.

[Final27 observations](final-scenarios.tsv). Earlier `scenario-results.tsv`,
`post-clearance.tsv` document the failures caught before the final fix.

| Scenario | Final observation |
| --- | --- |
| Fresh basic-water route | Boss reached76.31s;21 kills;3 volleys;3 incoming hits;4HP,70 Ichor. No dash. |
| Fresh two-skills/reactive-dash route | Boss reached75.46s;21 kills;3 volleys;7 dashes;0 hits;5HP,70 Ichor. |
| Isolated stationary player | Dies11.18s after5 hits; first hit1.62s. |
| Isolated strafing player | Survives25s, full health, against11 volleys. |
| Isolated reactive-dash player | Survives25s, full health, against9 volleys. |
| Spitter behind cover | Completes detour, first volley2.27s;4 volleys/10s;0 blocked firing lanes; stationary player ends1HP. |
| Magnet across locked gate |0px movement and0 Ichor collected in2s. |
|95/100 capacity pickup | Collects5, preserves5; later Blade top-up emits only2 total pickup events. |

These establish pressure and counterplay, not ideal human difficulty. Main-path
enemy count remains unchanged. Early scouts remain easy to kite, and all optional
routes have not received complete human playthroughs.

### Larger-boss check

[Adaptive boss fixtures](boss-scale/scenario-results.tsv) rerun after wider footing
and magnetic Ichor. Full240HP, no healing/teleporting/resource injection during fight:

- 0 starting Ichor: victory at 269.34s, 3HP, 5 transformations, gate open.
- 70 starting Ichor: died at 207.42s, boss at 66HP; all hits during reversion.
- 100 starting Ichor: victory at 221.93s, 3HP, 2 transformations, gate open.

Victory confirms the enlarged body did not make the encounter mechanically
unwinnable. Duration and transformation/reversion balance still need human tuning.
No claim that the full fresh run through boss and exit has been completed here.

### Reproduce

```bash
./build.sh
audit_classes=$(mktemp -d /tmp/b2bj-immersion-check.XXXXXX)
javac --release 17 -cp build/B2BJ.jar -d "$audit_classes" tools/AuditImmersion.java tools/AuditOutpost.java tools/PreviewImmersion.java
java -ea -Djava.awt.headless=true -cp "build/B2BJ.jar:$audit_classes" AuditImmersion /tmp/b2bj-immersion-results.tsv
java -ea -Djava.awt.headless=true -cp "build/B2BJ.jar:$audit_classes" AuditOutpost /tmp/b2bj-boss-results adaptive
java -Djava.awt.headless=true -cp "build/B2BJ.jar:$audit_classes" PreviewImmersion
```

`PreviewImmersion` images deliberately stage art/feedback. Reviewed Spitter facing,
projectiles, Blob hurt, moving Ichor and larger Warden in actual scene rendering.
Not evidence of live input latency, human timing, sound-device behavior or Windows
release testing. Spitter movement reuses body-pulse frames; dedicated movement art
and a more consistent north-facing silhouette remain polish candidates.

## Generation and handoff

**40/40 generations used;0 remaining. All21 jobs completed;208 PNG frames retained.**
Bad coin/disc concepts, wrong-facing frames and an opaque debris frame are excluded.
[Art ledger and exact selections](../../art-review/immersion-40/README.md).
Credential process closed; scoped source/tool/report scan found no supplied token.

Current open game windows do **not** hot-reload the rebuilt JAR. Relaunch using
`./run-game.sh` to test this build. Persistent saves, music, controller/remappable
input and full first-biome balancing are still separate unfinished work.
