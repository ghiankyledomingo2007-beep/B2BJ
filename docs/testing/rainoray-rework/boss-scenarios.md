# Headless Rainoray boss comparison

## Setup and limits

`tools/AuditRainoray.java` reuses the existing `AuditOutpost` diagnostic approach: fixed 0.01-second steps, nearest-add targeting, orbit/drop collection, eight-direction collision-aware steering, and reactions to committed enemy tells. Both policies also recognize the new fissure lane. Each fight is capped at 360 simulated seconds.

These are **initial boss fixtures**, not fresh runs through the outpost: room 9, full 5 HP, player 280 units west of the Guardian, initial Ichor 0/70/100, full-health Guardian and no initial adds. After setup, only normal movement/attack/tide/transform/dash/skill inputs are used. No health/resource refill, teleport, forced damage, or mid-combat actor edits. Normal boss add spawning, magnet pickups and passive Blob regeneration remain active. Neither policy consumes remains.

Baseline uses existing attacks and dodge policy. Skills policy additionally casts Crescent when an eligible target is 145–330 units away with more than 24 Ichor, and attempts Riposte against an imminent visible threat. It briefly stops attacking to reserve the shared attack recovery for guard. Guarded hits do not trigger a dodge that would cancel the guard.

Runs were entirely headless; no game window or audio device was opened. Java heap was capped at 256 MB. This is deterministic input-policy evidence, not a human playtest or a difficulty recommendation.

## Results

| Policy | Initial Ichor | Outcome | Time | Final HP | Boss HP | Phase 2 reached | Crescent casts | Guards / successful parries | Fissure impacts |
|---|---:|---|---:|---:|---:|---:|---:|---:|---:|
| Baseline | 0 | Win | 237.96 s | 3 | 0 | 126.12 s | 0 | 0 / 0 | 10 |
| Baseline | 70 | Death | 177.61 s | 0 | 53 | 96.23 s | 0 | 0 / 0 | 7 |
| Baseline | 100 | Win | 234.52 s | 3 | 0 | 104.49 s | 0 | 0 / 0 | 11 |
| Skills | 0 | Win | 231.29 s | 5 | 0 | 129.22 s | 10 | 0 / 0 | 9 |
| Skills | 70 | Win | 213.46 s | 2 | 0 | 110.29 s | 10 | 1 / 1 | 9 |
| Skills | 100 | Win | 216.23 s | 1 | 0 | 98.81 s | 9 | 0 / 0 | 10 |

Machine-readable counts, including primary/tide casts, transformations, damage hits and time without boss damage: [gameplay.tsv](gameplay.tsv).

## Findings

- All six fights reached phase 2. Five ended in victory and opened the gate; one ended in player death. No timeout or observed softlock. Longest interval without boss damage was 19.77 seconds, not indefinite stagnation.
- New skills are not an automatic survivability upgrade: starting at 100 Ichor, the skills policy won 18.29 seconds faster but finished at 1 HP instead of 3. More aggressive skill spending changes when the vulnerable reversion occurs.
- Reversion remains the main risk in this sample: 4 of 5 baseline damage events and 3 of 4 skills-policy damage events occurred while recovering. This records state when the hit event is observed, not a separate proof that reversion alone caused each hit.
- Riposte is lightly exercised by the adaptive policy (one successful use across three fights) because walking/dashing escapes most tells first. The focused `RainoraySkillsTest` separately exercises real Wisp, hostile-shot and Guardian damage gateways. These scenarios do **not** establish that Riposte is easy for a human to time.
- Crescent appeared 9–10 times per skills fight, remained finite and did not prevent phase progression. Fissures appeared in all six runs. No production balance values were changed in response to this small deterministic sample.
- Near-future manual testing should concentrate on skill readability, reversion timing and whether F offers enough value beyond the already-frequent dash. Boss fight length here remains roughly 3.5–4 minutes; this policy is not optimized for fast damage.

## Reproduction and verification

From the project root:

```bash
rtk proxy bash -c 'audit_build=$(mktemp -d /tmp/b2bj-rainoray-audit.XXXXXX); audit_sources=(); for f in src/*.java; do if [[ "$f" != src/B2BJ.java ]]; then audit_sources+=("$f"); fi; done; javac --release 17 -Xlint:all -d "$audit_build" "${audit_sources[@]}" tools/AuditRainoray.java && java -ea -Djava.awt.headless=true -Xmx256m -cp "$audit_build" AuditRainoray'
```

Executed once to produce the TSV, then compiled and repeated to a temporary `repeat.tsv`. `cmp docs/testing/rainoray-rework/gameplay.tsv <temporary-repeat.tsv>` returned exit 0: all six result rows were byte-identical. Assertions kept each fight in room 9 and verified finite bounded Ichor and HP throughout. Non-UI compilation avoids unrelated generated-art requirements; final packaged game verification remains separate.

### Final gameplay-only rerun

On 2026-09-07 at 01:54 Philippine time, recompiled the current non-UI production sources at repository HEAD `676f41a` with `javac --release 17 -Xlint:all`; the gameplay/map/Guardian files had no uncommitted diff. Ran `ArenaTwistTest`, `RainoraySkillsTest`, then `AuditRainoray`, each with `-ea -Djava.awt.headless=true -Xmx256m`. Both test programs passed and all six scenario rows remained exactly the values above: **5 wins, 1 death, 0 timeouts**, all reached phase 2.

The new dressing footprints were active during this run. Source inspection confirmed `waterBlocked()` includes `dressingObstacles`, `isBlocked()` delegates to it, and both combat line tests respect those feet. `ArenaTwistTest.verifyDressing()` exercised every room: upright cart/brazier feet block actors and projectiles, their upper silhouettes are not invisible walls, and flat shield/rubble dressing stays walkable. In room 9 specifically, the two brazier bases are solid; the shield cache and rubble are intentionally not. Its fissure escape-path checks passed with those footprints present.

No new gameplay defect or softlock was reproduced by this bounded rerun. Highest remaining risk remains vulnerable reversion and the weak human usability evidence for Riposte; the outcomes do not justify automatic retuning. No GUI, audio device, or visual-completion claim is part of this check. Art review and final packaged integration remain pending outside this report.
