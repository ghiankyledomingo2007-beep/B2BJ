# Placeholder campaign verification — 2026-09-08

## Delivered scope

Source: GCD v1.0, pp. 9–20, plus the owner's explicit continuous-exploration,
Java, water-attack and placeholder-art overrides. See [build plan](../campaign-build-plan.md).
The pre-build state is preserved at `cb8c35d` and branch `checkpoint/pre-campaign-20260908`.
Claude's independent `outpost-art-fable` branch was neither overwritten nor merged.

Four continuous 6144×4096 regions, 48 landmarks, 105 authored enemy placements,
four bosses, optional NPC/memory quests, shard-funded upgrades, death/checkpoints,
atomic save/continue and both final choices are playable. Existing player/combat
assets are reused; new scenery/mobs/bosses/HUD are intentionally placeholders.
No PixelLab or other paid generation ran in this task.

## RED and GREEN

Java 17 `javac --release 17 -Xlint:all` and assertion-main tests are the existing runner.
No runtime dependencies were added; npm/pip audits are not applicable to this Java runtime.

| Guarantee | RED evidence | GREEN check |
|---|---|---|
| All four maps and collision-safe branches | Missing CampaignWorld/map campaign API | CampaignWorldTest; legacy map/collision tests |
| Enemy profiles and distinct boss behavior | Missing CampaignEnemy/Guardian.Profile | CampaignEnemyTest, CampaignBossTest, CampaignBalanceTest |
| Atomic complete progression saves | Missing CampaignSave; malformed/semantically impossible saves accepted during hardening | CampaignSaveTest |
| Four NPC quests and both GCD endings | Missing CampaignStory; shared memoryName missing | CampaignStoryTest, FullCampaignTest |
| Health, gain rate, drain and fourth Blade hit upgrades | Nine missing Player progression methods | PlayerProgressionTest, FullCampaignTest |
| Real desktop component uses campaign controller | Missing B2BJ(boolean, game) | CampaignDesktopTest |
| Full route/death/backtracking/reload/ending flow | Missing campaign() and returnToTitle() | FullCampaignTest |
| Audio settings and single-worker ambience | `Audio control missing: setMasterVolume`; `Campaign options panel missing` | GameAudioTest, CampaignOptionsTest |
| Golem has usable punish window | Missing recovery-duration API/short old recovery | CampaignBalanceTest; optional full economy measurements |
| Routine HUD notices expire | `routine notices must not permanently cover exploration` | CampaignDesktopTest |

Current-task RED checkpoints include `1b45c1a`, `d265320`, `7d3f012`, `4d501b3`,
`40293ef`, `a808997`. Core GREEN integration is `931c9c8`; later fixes are retained
as a separate final GREEN commit, without squashing the evidence.

Final command: `rtk proxy ./build.sh` — **exit 0, all 64 test programs passed**.
The script also reran packaged asset loading from `/tmp` and produced `build/B2BJ.jar`.
Independent QA reran the full 64-test suite from isolated classes and also passed.
No numerical code-coverage claim: this project does not currently instrument coverage.

## Scenario and balance limits

FullCampaignTest deals boss damage through real game attacks, with authored collision,
guard/recovery timing and actual progression. It relocates the player to scenario
positions and replenishes health/Ichor; it proves flow, not human difficulty.
CampaignBalanceTest's optional `--measure` mode uses actual movement, dodges, attacks
and one starting Ichor reservoir, but repeatedly heals the player to measure economy.

Golem recovery changed from 1.5/1.25 seconds to 2.1/1.8 seconds. HP 320 and aura
3/5 Ichor per second are unchanged. That strategy fell from 428 seconds/11 forms
to 208 seconds/6 forms without upgrades, or 176 seconds/3 forms with one upgrade
per track. Other bosses take 3–5 forms in these measured configurations.

CampaignPerformanceTest measures all regions at 1280×720 and half-scaled output.
Typical ordinary-frame p95 was roughly 5–6 ms on this machine; expanded-map
measurements varied and occasionally exceeded 20 ms under shared-host load.
This is not a guaranteed frame-rate claim, GPU benchmark, or long boss soak test.

## Visual/native checks

CampaignRenderingTest and CampaignDesktopTest cover actual Java2D frames and Swing
input bindings. `tools/PreviewCampaign.java` reproduces integrated biome, combat,
title, final-choice and ending screenshots using isolated saves. Full-frame images
were inspected; fixes removed false gaps between minimap wall spans, hidden NPCs,
boss-HUD clipping and status panels over nearby combat.

Native Robot testing was **inconclusive**: this desktop kept the Java window
iconified/unmapped, and attempted focus restoration did not make Enter reach it.
The game stayed at PROLOGUE, not an observed gameplay crash. The optional
`tools/LiveCampaignSmoke.java` now refuses physical input without verified focus
and bounds its focus wait. No native gameplay or audible-speaker success is claimed.
The production JAR launches and its named window/PID were verified; desktop focus
and an unassisted playthrough remain user checks.

## Remaining release work

Final coherent art/animation, authored music polish, controller support, remappable
bindings, scalable/fullscreen presentation, persistent options, long survival and
difficulty testing, accessibility review and measured campaign length remain.
This is a complete start-to-ending **placeholder campaign**, not a claim of a
finished commercial release, 4–6 hours of content, or a bug-free game.
