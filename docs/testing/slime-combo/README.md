# Slime combo and stagger pass — 2026-09-07

Source: user requested more M1 variations, a dodge follow-up, and enemies reacting
to body slams. Enemy-copy transformations and leveling were explicitly deferred.
Claude's environment-art worktree was not edited or merged by this pass.

## Controls and balance

- LMB/hold: alternating water cuts, then a twin-cut finisher. Damage 1 / 1 / 2;
  normal 0.48-second casting cooldown stays unchanged. The chain expires after
  0.9 seconds without a new attack. Dash cancellation and transformation reset it.
- Cuts have opposite six-world-pixel side sweeps and visibly opposed sprite
  angles. Finisher has two native-size arcs, a 0.12-second windup, a shorter
  270-pixel range, stronger knockback and stagger. It is non-piercing and does
  not consume or bypass Tide Wave's cooldown.
- Dash away from the intended target, then aim back and LMB within 0.3 seconds
  after the dash: countershot. Its 0.04-second windup and faster travel reward
  timing; damage remains 1, with stagger and moderate knockback. It spends the
  normal casting cooldown, cannot fire during dash, and consumes its window once.
- Slime body slam damages each enemy once per dash and pushes it away.
- Stagger lasts 0.28 seconds; another stagger cannot reset it. Enemies resist
  further staggers for 1.2 seconds from the first interruption, but still take
  damage. Ordinary cuts do not stagger. Boss armor/recovery rules are unchanged.
- Space remains dash, not jump. No airborne combat, XP, new unlock UI, or new
  enemy-copy form was added. Zero PixelLab jobs were submitted.

## Root cause and fix

The old update loop applied enemy contact and spitter release before processing
water and dash impacts. A target could hurt the player or leave a projectile fan
on the exact physics step that should interrupt it. Outgoing impacts now resolve
before incoming contact/release. The last movement step of a dash also retains
protection without extending invulnerability beyond that step. Already released
enemy projectiles are not erased by staggering their owner.

## TDD evidence

Journeys were derived during this run; no external plan was executed.

1. RED `a231423`: compiled/ran each `SlimeComboTest` scenario against old source.
   Failures: missing opposite sweep, missing fast countershot, scout contact
   before same-step stagger, endlessly refreshed stun, and no slam impulse.
   `SlimeAttackRenderingTest` also failed on identical return-cut sprites.
2. GREEN `b0b4277`: same targets passed, then `rtk proxy bash build.sh` passed all
   52 Java test programs and packaged asset loading from outside the project.
   The full suite first caught a curved shot clipping the tree in its existing
   clear firing lane; narrowing the sweep fixed it without weakening that test.
3. Visual review rejected the initial overlapping finisher and nearly identical
   return-cut silhouette despite different image hashes. RED `2435251` added a
   real silhouette-change threshold and a separate finisher visible-area check.
4. Final art correction: opposed angles, separated finisher arcs, and a faint
   countershot echo, all rendered into the existing native 2x pixel buffer.
   `SlimeAttackRenderingTest` and the full build passed again. Extra trajectory
   chunking/expiry and final-dash-step checks passed in `SlimeComboTest`.

| Guarantee | Runnable check | Result |
| --- | --- | --- |
| Three-step sequence, expiry and cancelled-cast reset | `SlimeComboTest` | PASS |
| Timed back-dash counter; no forward/side/late counter; pause and cooldown | `SlimeComboTest` | PASS |
| Scout/guard/spitter interrupted before same-step damage or volley | `SlimeComboTest` | PASS |
| Finite stun, resistance without damage immunity, one slam hit and shove | `SlimeComboTest` | PASS |
| Frame-independent trajectories, finite lifetime, final dash-step safety | `SlimeComboTest` | PASS |
| Different cut silhouettes, visible two-cut finisher, native 2x raster | `SlimeAttackRenderingTest` | PASS |
| Cover, boss armor, cancellation, death, pause and room progression | Existing full Java suite | PASS |

Actual targeted checks used `javac --release 17 -Xlint:all` on `src/*.java` and
the test files into a fresh `/tmp/b2bj-combo-*` directory, followed by
`java -ea -Djava.awt.headless=true -cp <directory> SlimeComboTest` and
`SlimeAttackRenderingTest`. Final full command: `rtk proxy bash build.sh`.

Coverage gap: this repository has no instrumented Java coverage runner, so no
80% line/branch coverage claim is made. Tests and deterministic renderer fixtures
are not a human playthrough or proof that final balance feels right.

## Visual inspection

`cuts-8.png`, `cuts-20.png`, and `cuts-36.png` were inspected: five projectile
types, eight directions, three flight ages. They use the actual renderer and
existing art, not regenerated source assets. Reproduce by compiling
`ReviewSlimeCombo.java` against current sources or the new `build/B2BJ.jar`, then
running `ReviewSlimeCombo docs/testing/slime-combo` with assertions enabled.

## Next, not built

Keep progression independent of XP for now. First prototype one guaranteed
enemy-derived move from absorption; only then add a temporary enemy-copy form.
Full morphing needs per-form movement, attacks, eight-direction animation,
collision size, UI and balance. Random morph unlock chances should come after
that single-form loop works, not before.

The existing game process was left alone. Restart it to load the rebuilt JAR.
