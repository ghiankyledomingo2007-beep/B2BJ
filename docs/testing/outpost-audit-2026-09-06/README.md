# Ruined Outpost quality audit — 2026-09-06

**Readiness judgment: 45/100 — prototype, not a finished first biome.** This is a
prioritization judgment, not a coverage percentage or player-study score.
Biggest blockers: reversion invulnerability, ineffective stationary boss behavior,
and enemy AI/balance that did not keep up with the new ranged Slime attacks.

## Scope and evidence

- Audited the current dirty `main` worktree, not just initial commit `ccc82a6`.
- Rebuilt the game; all 19 existing Java test programs and the external-directory
  packaged-asset check passed. Green tests did not cover the exploits below.
- Added `tools/AuditOutpost.java`: 35 deterministic observations covering actual
  fresh-run movement/combat, full-health arena fights, range sweeps, state boundaries,
  obstacle routing, summon overlap, resource behavior, spawn safety and render cost.
- Raw numbers: [scenario-results.tsv](scenario-results.tsv). Scene snapshots are
  retained in this directory. Simulation uses 10 ms steps and production gameplay
  methods. Durations below are simulated game time, not execution wall time.
- Actual fresh route: no teleports, forced kills, injected Ichor, healing commands,
  or dashes. The safe tutorial transformation and automatic camp heal remain normal
  gameplay. The separate arena probes explicitly set starting position/resources;
  they must not be described as unassisted complete playthroughs.
- Snapshot panels use the current renderer against scenario state. They are static
  state illustrations, not recordings validating every animation frame or live input.
- GCD checked: v1.0, August 5, 2026, pages 8–12 and 15–19. Source:
  `/home/ghiankylledomingo/Downloads/01-School/Game-Dev/Blob-to-Blade-Game-Concept-Document-1.pdf`.
  The owner-requested water moves supersede the old Blob melee description; the
  GCD's vulnerability, readable danger and transformation-tension goals still apply.
- SHA-256 hashes of every `src/*.java` file were unchanged across this audit.
  No gameplay fixes, asset generations, commits, pushes or configuration changes.

## Confirmed findings, in priority order

### 1. P1 — Dashing through Blade expiry grants unintended recovery immunity

**Code defect.** `src/Player.java:46`, `:48`, `:49`, `:100`.

Reproduce: transform; let roughly 11.94 seconds elapse; start a dash; let Blade
expire. Recovery returns before reducing `dashTime`, so `dashing()` stays true and
`invulnerable()` continues rejecting hits while the character is immobilized.
The probe still rejected damage after one second of recovery. The no-dash control
accepted damage normally. Remaining dash time can also resume after recovery.

Independent review also exposed a source-traced consequence: the game keeps
calling `damageEnemiesFromDash` while that frozen flag is set, and the Blade guard
no longer applies after reversion. New enemies entering contact can take stale
dash damage. This consequence was not separately scenario-tested.

**Fix direction:** terminate dash consistently on reversion, without extending
dash i-frames through recovery. Cover timer expiry and damage-induced expiry at the
shared player-state boundary. Preserve ordinary hurt i-frames according to policy.

**Acceptance:** expiration during any point of a dash cannot freeze its timer,
grant recovery-long dash immunity, or resume stale dash movement afterward.

### 2. P1 — Boss movement is absent, and its own attacks provide no pressure

**Missing behavior plus confirmed balance problem.** `src/Guardian.java:25`, `:26`,
`:64`, `:82`; `src/RuinedOutpostGame.java:178`; `src/RuinedOutpostMap.java:139`.

Coordinates are immutable. Over 60 seconds, boss displacement was exactly **0 px**,
with 23 impacts. Both 240-HP and 60-HP scenarios produced the same first 12 attacks:
`TTRTTRTTRTTR` (targeted, targeted, radial). There is no low-health behavioral phase.

Three Slime-only orbit probes beat the full 240-HP boss in **208–211 seconds**, with
**zero transformations and zero hits from the boss itself**. One initial-Blade
probe won in 161 seconds with only one transformation. These are counterexamples
to assuming the intended 3–5 transformation pacing is already achieved, not a
measurement of average human performance.

Important limitation: continuing from the actual fresh-route entrance with the
same simple water-only policy **died after 53 seconds**, leaving 185 boss HP.
All five hits came from adds, not the boss. A no-attack orbit also died to adds.
The encounter is not universally harmless; danger is concentrated in summons.

**Fix direction:** give the Warden bounded repositioning and distance-based attack
selection: an approach/short gap-closing attack, a close sweep, and a ranged ground
attack with distinct tells. Add a meaningful low-health change and limit repeats.
Do not merely increase HP; current water-only fights already last about 3.5 minutes.

**Critical integration note:** the map's Guardian collision uses fixed map spawn
coordinates. Moving only the sprite/Guardian object would leave a ghost obstacle
behind. Movement, collision, targeting, telegraphs and depth sorting must share the
same live position. Keep damage tied to the readable active phase.

**Acceptance:** attack choice responds to range; movement cannot pass through solid
terrain; old spawn position leaves no collision; one constant orbit is insufficient
against all attack types. Re-measure resource windows with both adds and boss active.

### 3. P1 — Ranged attacks defeat enemies before they engage

**Confirmed balance gap introduced by the new moveset.** `src/Wisp.java:21`, `:102`,
`:202`; `src/WaterProjectile.java:16`, `:28`; `src/RuinedOutpostGame.java:106`.

Awareness is 280 px. Basic water travels 340 px, with additional collision reach.
At 310 and 340 px, stationary Slime killed a 2-HP enemy in **1.09 / 1.15 seconds**
without an enemy lunge or player damage. Getting hit does not establish sustained
aggro. Tide killed the 310-px target in 0.88 seconds with no enemy attack.

Even inside awareness, most isolated targets died before their attack reached the
player. At 180–280 px the stationary basic-attack probe took no damage. At 120 px,
basic took one hit; Tide killed first without damage. These are isolated fixtures,
not proof every crowd is safe.

**Fresh-route confirmation:** both bot policies walked sections 0–9, killed all
21 ordinary enemies, and reached the boss in **80.47 / 81.54 seconds**, taking
**zero hits**, without dashing. Only the required tutorial transformation was used.
The first ten enemies and tutorial were reached in about 39 seconds, untouched.

**Fix direction:** make damage wake enemies and retain aggro; add actual pursuit
and closing behavior, then tune reach/HP/attack timing together. Keep the ranged
water identity. Fixing awareness first gives better evidence than blindly weakening
the player's new skill or inflating every enemy's health.

### 4. P1 — Enemies repeatedly attack cover instead of navigating around it

**Confirmed AI limitation affecting combat.** `src/Wisp.java:82`, `:110`, `:123`,
`:133`. [Example snapshot](enemy-stuck-at-cover.png).

With player and enemy on opposite sides of a short rubble obstacle, the enemy
attempted **29 lunges in 30 seconds**, advanced only 31.8 px toward the player,
and never routed around the open ends. Collision correctly prevented penetration;
there is no pursuit/path-around decision when forward movement is blocked.

**Fix direction:** a small local navigation/path-around step for pursuit, plus a
blocked-lunge transition. Do not repeatedly re-enter the same doomed attack.

**Acceptance:** approach around both ends of representative cover; no endless wall
lunges, tunneling, unreachable enemies, or route-induced room-clear soft locks.

### 5. P2 — Attacks repeat and summons visually merge

**Confirmed design/readability gaps.** `src/Wisp.java:73`, `:103`, `:110`;
`src/RuinedOutpostGame.java:186`; `src/B2BJ.java:458`.

Ordinary enemies and boss adds share one patrol/telegraph/lunge/recover loop.
All authored ordinary enemies have 2 HP. Ordinary attack art reuses four Slime
dash/squash poses; a palette swap does not supply a different encounter behavior.

Summons came within **0.20–0.30 px** of each other in arena runs. Two fixed spawn
locations plus no separation lets three enemies collapse into nearly one silhouette.
Health bars and attack rings cannot communicate the overlapping threat cleanly.

**Fix direction:** at least two purposeful first-biome behaviors with different
spacing/recovery, introduced separately before mixing. Give summons separated
valid spawn slots, local separation and modest attack staggering. Random animation
frames alone will not solve repetition or unfair-looking stacked hits.

### 6. P2 — Lunge direction can change at the very end of its tell

**Confirmed behavior; fairness/design decision.** `src/Wisp.java:110`, `:114`.

Moving the target behind the enemy in the last 20 ms caused a 180-degree retarget;
the following lunge moved 26 px in the new direction. The current tell is a red
oval, not a direction/locked lane, so the player cannot reliably read commitment.

**Fix direction:** track early, then lock direction for a short readable commitment
window before launch. Give the active move and its recovery distinct poses. The
exact lock duration needs feel testing, not an arbitrary universal number.

### 7. P2 — Full-meter drops can be banked, then mostly wasted on tiny refills

**Confirmed mechanic; GCD policy mismatch rather than a crash.**
`src/RuinedOutpostGame.java:314` and `src/Player.java:104`.

A drop remained after walking over it at 100 Ichor and waiting 30 seconds. After
transforming, the next tiny drain allowed pickup and consumed the whole drop to
refill only the newly missing fraction. The probe ended 0.1 seconds later at
99.25 Ichor / 11.91 Blade seconds, with the 10-Ichor drop gone.

The GCD says no stockpiling and excess-at-full is wasted. Current behavior banks
world drops while full, then consumes them near full. Decide the intended rule
explicitly: discard-on-contact at full, or knowingly support stored pickups with
consistent partial collection. Do not silently change the economy in an art pass.

## Scenario summary

| Scenario | Measured result | Meaning |
| --- | --- | --- |
| Fresh route, basic attack | All 21 enemies; boss reached in 81.54 s; no hits/dashes | Ordinary encounters currently offer very little pressure to ranged spacing |
| Fresh route, both water skills | Boss reached in 80.47 s; no hits/dashes | Heavy skill changes little in this simple route policy |
| Normal-entry boss continuation | Died in 53.12 s; boss 185 HP; all five hits from adds | Full-route completion is not established; summons remain dangerous |
| Staged Slime orbit, three radii | Boss defeated in 208.18–211.01 s; 0 transformations | A repeatable alternate tactic bypasses intended transformation pacing |
| Staged initial-Blade orbit | Boss defeated in 161.29 s; 1 transformation | Merely increasing HP did not establish the target cycle count |
| Boss patterns at full/quarter HP | Same TTR sequence; 0 px travel | Mobility and phase change are absent |
| Reversion while dashing | Damage rejected; dash frozen through recovery | Real state-transition bug |
| Enemy behind cover | 29 failed lunges / 30 s | Collision works; navigation/decision-making does not |
| Spawn collision sweep | 21/21 authored ordinary spawns clear | No initial embedding found in this sweep |
| Headless rendering | Warmed p95 8.75 ms in latest run | No obvious renderer bottleneck here; not a live-display FPS guarantee |

## What the first biome still needs

1. **Combat correctness:** fix reversion state, navigation, aggro and summon overlap;
   promote reproductions into regression assertions that fail before each fix.
2. **Boss encounter pass:** live-position movement/collision, range-aware moves,
   committed readable tells, phase change, and a resource/pressure plan for adds.
3. **Encounter variety:** authored enemy roles and mixed encounters, not simply
   repeating the same five spawn coordinates with more identical Slimes.
4. **Progression decisions:** disk save/load, explicit death/respawn policy and a
   reliable Continue flow. Current checkpoint state survives only this process;
   cleared rooms remain cleared after checkpoint death, unlike the GCD respawn rule.
5. **First-biome narrative payoff:** current premise, room lore, camp and forest
   boundary exist. Still need authored environmental beats, optional-room purpose
   and a boss/exit payoff. Do not invent later-biome content to fill this gap.
6. **Presentation completion:** final Blade side attacks and distinct enemy action
   poses; finish selected blockout terrain/props; music layers and a listening pass.
   Current synthesized SFX tests are not perceptual sound-quality approval.
7. **Player-facing shell:** New/Continue/Options/Quit, fullscreen/resolution options,
   remappable controls and volume controls. Current game window is fixed 1280x720.
8. **Release verification:** Windows installer and at least the intended hardware
   range; actual mouse/focus/frame-pacing sessions; human blind playtests. The
   repository has workflow files, but remote CI/release status was not checked here.

## Next implementation order and gates

**First:** reversion immunity regression and fix. Small, shared-state correction.

**Second:** enemy awareness/pursuit/cover/separation. Re-run stationary range sweep,
fresh route, corner fights and summon stacking. Preserve readable counterplay;
do not compensate for broken AI with untelegraphed instant damage.

**Third:** mobile boss and attack selection, updating its collider at the same time.
Re-run normal-entry and staged fights, with and without collecting adds' Ichor.
Log boss hits separately from add hits, transformation counts, time to kill, and
avoidable versus unavoidable damage. The GCD's 3–5 transformations is a tuning
target; these deterministic bots cannot establish human averages or deaths/zone.

**Then:** presentation, progression persistence and UX. Generate only assets required
by the approved moves and timing. The last recorded PixelLab allowance was exhausted;
its current balance was not rechecked, and no generation was attempted in this audit.

## Reproduce

From the project root:

```bash
rtk proxy bash build.sh
rtk proxy javac --release 17 -cp build/B2BJ.jar -d /tmp/b2bj-audit tools/AuditOutpost.java
rtk proxy java -ea -Djava.awt.headless=true -cp build/B2BJ.jar:/tmp/b2bj-audit AuditOutpost
```

The probe reports observed defects; it is not part of the green regression suite
and does not assert that these defective behaviors should be preserved.

## Independent review

Completed through Claude Code CLI 2.1.263 using **`claude-fable-5-1`**, verified in
the substantive response and provider result. The initial tool-enabled attempt
timed out; a bounded, tool-free review of numbered source excerpts and measured
results succeeded. No gameplay files were edited by the reviewer.

[Review and adjudication](fable-review.md) preserves its response and distinguishes
accepted findings from overclaims. In particular, neither a scripted fresh-entry
death nor staged orbit wins establish normal human difficulty. The proposed HP
cut was not adopted. Repeated `playerDied()` calls are idempotent, not a bug.
