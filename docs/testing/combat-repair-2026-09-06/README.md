# Ruined Outpost combat repair — 2026-09-06

Follow-up to the [read-only gameplay audit](../outpost-audit-2026-09-06/README.md).
The owner authorized fixes, animation generation and Claude Code delegation.
Scope remains the first biome. This is **not a finished-biome sign-off**.

## Implemented

- Reversion cancels Blade dash through one shared recovery entry point, including
  timer expiry and damage-driven expiry. A stale pre-update dash flag cannot
  turn that expiry into a Slime damage hit.
- Warden now approaches and physically charges. Live body collision follows its
  position instead of blocking the old spawn. Terrain and player separation are
  respected. Targeted slam, close sweep, committed charge lane and expanding
  shockwave use distinct active geometry; damage is limited to once per attack.
- Boss targets lock for the entire 0.9-second tell. Warnings, attack names and
  phase-II label follow real state. Existing 240 HP retained, not tuned merely
  to make a bot win.
- Wisp pursuit remembers damage aggro, checks line of sight, navigates simple
  rectangular cover, locks final lunge direction and ends blocked lunges early.
  Crowded enemies separate without being teleported through terrain.
- Remnant guards appear after the opening section: 4 HP, longer approach/lunge,
  longer tell and recovery, gold marker. Scouts remain 2 HP. Both are still
  lunge-family opponents; this is not a completed ranged/support enemy roster.
- Boss summons use bounded authored slots with player/boss/other-enemy clearance;
  normal/enraged caps are 2/3. No dedicated spawn-warning animation was added.
- Reviewed casting, Blade side attacks, remnant actions and Warden animations
  are packaged. [Full art ledger](../../art-review/combat-repair-40/README.md).

Claude Code CLI reported `claude-fable-5-1` for the requested delegated task.
Ownership was limited to `Wisp.java` and `WispPursuitTest.java`; integration,
guard roles, boss, reversion, art and final checks were performed separately.

## Runnable regression evidence

`./build.sh`: **26 Java assertion-test programs passed**, then the packaged-asset
check passed again from outside the project directory. Java 17 compatibility and
lint compilation succeeded. `python3 tools/test_pixellab_call.py`: **2 passed**.
`git diff --check`: passed. No coverage percentage was measured.

| Regression | Observed RED before repair | GREEN evidence |
| --- | --- | --- |
| Expiry during dash | Recovery left stale dash active/invulnerable | `ReversionSafetyTest`: timer/damage expiry, stale damage and movement |
| Stationary boss/body collider | Boss never closed distance | `GuardianMobilityTest`: travel, terrain, live collider and cleared old position |
| Boss attack geometry | Added state/geometry checks | `GuardianPressureTest`: four attacks, no tell damage, committed target, ring interior |
| Spawn overlap | Summoned enemies overlapped | `SummonSafetyTest`: bounded adds, safe spacing, separation |
| Pursuit and readable lunges | Six of eight delegated pursuit checks initially failed | `WispPursuitTest`: aggro, cover, lock, blocked lunge, separation |
| Guard variation | Compile failure: role/duration API absent | `RemnantRolesTest`: authored introduction, longer tell/travel, Blade one-hit kill |
| New art/strike timing | Missing asset, then missing timing API | `CombatArtTest`: grids, transparency, distinct poses and contact timing |

The credential helper accepts a temporary process environment override, validates
header shape and redacts authorization material from output. Tests verify override
precedence without reading the private config and output redaction. The temporary
credential-bearing process was closed; no credential/config changes were saved.
No commits, pushes, releases, global installs or unrelated cleanup were performed.
The existing mixed worktree was preserved; no checkpoint commits were created.

## Gameplay scenarios

`tools/AuditOutpost.java` drives production logic with real movement/attack inputs.
The original 35-observation runner and the additional adaptive boss policy are
diagnostic observations, not blanket assertions that current balance is desirable.

- [Core repair observations](scenario-results.tsv)
- [Final guard-role observations](roles-pass/scenario-results.tsv)
- [Adaptive boss observations and hit sources](adaptive-pass/scenario-results.tsv)

| Scenario | Observation |
| --- | --- |
| Boss movement over 60 s | 390.1 px at full HP; 473.6 px in the low-HP fixture. Old audit: 0 px. |
| Enemy behind cover, 30 s | 225.7 px advance, 6 lunges, stationary player died. Old audit: about 31.8 px advance and 29 ineffective lunges. |
| Last-20-ms direction change | Lunge kept committed direction (+26 px) instead of reversing (old −26 px). |
| Authored enemy spawn positions | 21 checked; none initially embedded in solid collision. |
| Fresh basic-water route | Reached boss in 78.35 s through rooms 0–9, 21 kills, 1 incoming hit, 70 Ichor. Camp's normal healing restored HP. No injected health/teleports/dashes/forced kills. |
| Fresh two-water-skill route | Reached boss in 82.09 s, 1 incoming hit. |
| Old circle-and-fire boss policy | Dies; Warden itself now lands hits. This alone is not evidence of fair difficulty. |
| Adaptive, 0 starting Ichor | Full 240-HP boss defeated in 256.43 s; 3 HP left; 3 transformations; gate open. Maximum 3 adds, minimum observed add spacing 214.66 px. |
| Adaptive, 70 starting Ichor | Died at 145.44 s; boss 82 HP; 3 transformations. All three hits occurred during reversion recovery. |
| Adaptive, 100 starting Ichor | Died at 176.50 s; boss 50 HP; 4 transformations. All three hits occurred during reversion recovery. |

Adaptive fixtures set initial position and Ichor only. No health refill, forced
damage, enemy relocation, invulnerability or resource injection occurs during
the fight. The input policy reads attack state, moves out of circles/charge lanes,
dashes through shockwaves, attacks summons and collects drops. Its first version
mistakenly issued a zero-direction dash when already at its desired distance;
that **test-driver bug** was corrected before the recorded adaptive results.

The 0-Ichor victory establishes mechanical counterplay, not human accessibility
or a complete fresh-run victory. The bot transforms as soon as eligible, without
scheduling expiry; its failures identify recovery timing as a tuning/playtest
priority. A 4m16s winning boss fixture is longer than desirable for a quick first-
biome iteration, but no target duration is treated as an established GCD fact.

Reproduce from the project root after `./build.sh`:

```bash
audit_classes=$(mktemp -d /tmp/b2bj-audit.XXXXXX)
javac --release 17 -cp build/B2BJ.jar -d "$audit_classes" tools/AuditOutpost.java
java -ea -Djava.awt.headless=true -cp "build/B2BJ.jar:$audit_classes" AuditOutpost /tmp/b2bj-audit-results
java -ea -Djava.awt.headless=true -cp "build/B2BJ.jar:$audit_classes" AuditOutpost /tmp/b2bj-adaptive-results adaptive
```

The headless renderer's guard-role run recorded p95 7.90 ms and p99 11.07 ms at
1280×720. This is not a live FPS, input-latency or audio-device measurement.
`PreviewCombatRepair` screenshots deliberately stage boss attacks; `PreviewOutpost`
stages character poses. These images are visual fixtures, not playthrough proof.

## Remaining before first-biome sign-off

1. Human/controller playtest of boss duration, recovery risk, warning visibility
   near screen edges and screen-effect intensity. Obtain a complete fresh-run
   victory without fixtures; present evidence, not a claim based on unit tests.
2. Ordinary scouts still die quickly to safe ranged attacks. Guards add one
   behavior variant, but the fresh route remains easy and repeated lunges still
   dominate the enemy roster. Local cover navigation is a bounded heuristic,
   not general pathfinding for arbitrary mazes.
3. Full-meter Ichor drops still remain banked until transformation. The audited
   resource-policy question was not silently changed.
4. Warden locomotion still lacks all directional views. Sweep body art is
   one-sided while the damage indicator is circular; review readability in motion.
   Rejected impact and side-cut candidates were not packaged just to spend credits.
5. Persistent save across launches, final story/menu polish, music and longer
   input/audio-device sessions remain. Consult the existing GCD alignment audit.
   Windows release workflow and actual desktop input were not exercised this pass.

PixelLab: **40/40 generations used, 0 left**, all 18 jobs completed; further
generation paused. Official docs were read and saved in
[`docs/reference/pixellab-mcp-guide.md`](../../reference/pixellab-mcp-guide.md).
TDD guided root-cause regression checks; Ponytail kept this dependency-free and
reused existing sprite tools. Security review guided temporary credential handling.
