# Campaign quality assessment

Final evidence and native-window limitation: [campaign verification](campaign.tdd.md).

Baseline: `cb8c35d`, 2026-09-08. Production sources and the running JAR were not changed by this assessment.

## Integrated campaign verification

Final verification: `rtk proxy ./build.sh` completed with exit code 0. All 64 assertion-based test programs passed, followed by a packaged-asset check launched from `/tmp`. The script compiled with Java 17 compatibility and all lint warnings, then produced `build/B2BJ.jar`. This final run includes boss tuning, options/audio, return-to-title, null-shop handling, performance smoke checks, and the complete campaign route. Independent QA also used isolated classes in `/tmp/b2bj-full-quality.SaobOa`; it did not rebuild the running JAR.

`FullCampaignTest` follows all four 6144 × 4096 biomes, defeats ordinary enemies and all four bosses through real controller attacks, returns through earlier biome portals, and verifies both endings from a saved completed route. It checks disk reload and death at every camp, one-time quest rewards, upgrade currency, controller-level knight shields, safe handling of unreadable saves, explicit new-game replacement, and zero-combat-Ichor ending choices. The route intentionally completes only the outpost's optional quest, so the other optional quests cannot be hidden ending requirements.

Combat fixtures refill health/Ichor and relocate the player to collision-free attack positions. They never directly subtract enemy or boss health. This proves combat/progression integration; it does not measure unassisted difficulty, resource pacing, manual boss balance, or finished art quality. Separate world tests flood-fill the actual player collider to every landmark, camp, boss, and passage.

Actual `B2BJ(false, game)` encounter and boss screenshots were inspected in `/tmp/b2bj-full-quality.SaobOa/campaign-encounter.png` and `campaign-boss.png`: Rainoray, ordinary enemies, the Warden, warnings, and HUD appear in the same scene. Renderer captures also showed readable outpost/catacomb camps, Orin dialogue, and the citadel map without clipping. These are static headless captures, not a live keyboard/mouse playthrough.

Campaign integration exposed a second regression: setting `bossDefeated` stopped the boss's `DEAD` animation clock. `FullCampaignTest` reproduced the frozen visible corpse after 0.8 seconds. The game now continues the dead actor's update without restarting attacks or summons; that regression passes.

Final controller checks proved that unpaused play rejects return-to-title, while pausing permits it and Continue restores the saved camp, upgrades, shards, and quest flags. A missing upgrade key previously threw `NullPointerException` at the camp; the shop now rejects it without throwing. Both fixes have passing regressions in `FullCampaignTest`.

## Rendering performance

`CampaignPerformanceTest` measures the real `B2BJ.step(1/60)` plus `paint` path using temporary, valid progress for all four biomes. Two longer runs used 60 warmup and 120 measured frames per scenario on an Intel Core i5-13420H, Java 25.0.3, Linux amd64, with 12 logical CPUs. The authored population totals 105 enemies; at most 33 are loaded in a measured area. Audio was muted and a health fixture prevented death; active bosses and summons were excluded. The test reports timing and has no brittle timing assertion.

| Scenario | Median frame | 95th percentile |
| --- | --- | --- |
| 1280 × 720 ordinary gameplay | 4.22–5.17 ms | 4.66–6.53 ms |
| Expanded map | 9.95–12.07 ms | Usually 10.29–15.83 ms |
| 640 × 360 scaled output | 2.34–3.21 ms | 2.61–5.21 ms |

The second citadel map run reached a 20.74 ms 95th percentile and 22.39 ms maximum, versus 11.48 ms at the 95th percentile in its first run. Results show variation on the shared host. The smaller output scales the full 1280 × 720 logical scene; it is not a separate 640 × 360 viewport. The isolated 1,536 minimap terrain queries took 0.88–0.91 ms. No rendering cache was added because ordinary measured frames were already below the 16.7 ms budget.

The standard smoke program defaults to 20 warmup and 30 measured frames; pass `120` for the longer run after compiling into temporary classes. These measurements exclude compositor, audible playback, GPU behavior, and a live unassisted playthrough. They do not promise uninterrupted 60 FPS.

## Automated boss economy measurements

`CampaignBalanceTest --measure` ran eight complete fights through the real controller. Each fixture loads a valid temporary campaign save, places the player once at a collision-free 430-pixel approach, and grants 100 Ichor once. Thereafter deterministic inputs move, attack, use Tide Wave and dash, target boss recovery, kill nearby summons, and walk to drops. Ichor is not refilled by the fixture. Health is restored every 0.05 seconds, so these runs measure economy and attack opportunities, not survival or human difficulty.

The Golem initially took 427.7 simulated seconds and 11 transformations with base stats. Extending only its recovery window from 1.5/1.25 seconds to 2.1/1.8 seconds in normal/enraged phases produced the following final measurements. Its 320 health and 3/5-Ichor-per-second aura were unchanged.

| Boss | Base stats: time / transformations | One upgrade in each track: time / transformations |
| --- | --- | --- |
| Outpost Warden | 150.7 s / 4 | 167.9 s / 4 |
| Briarheart | 206.2 s / 5 | 168.4 s / 4 |
| Oathkeeper | 156.1 s / 4 | 117.1 s / 3 |
| Ichor Golem | 208.3 s / 6 | 175.7 s / 3 |

The base Golem run killed 36 summons and received 35.5 health of damage; the modest-upgrade run killed 19 summons and received 45 health of damage. Continuous fixture healing is therefore a material limitation. The final modest-upgrade runs meet the targeted three-to-five transformation windows under this strategy, while the base Golem still takes six. The fixed strategy does not establish that every upgrade makes every fight faster: changed attack/resource timing made its upgraded Warden run slightly slower.

## Baseline evidence

All 52 existing assertion-based test programs passed after compiling source and tests with `javac --release 17 -Xlint:all` into `/tmp/b2bj-quality-baseline.ByF5Y8`. Tests ran headlessly from the project directory, so image loading could use the existing filesystem assets. This run does not independently verify packaged assets from another working directory.

Existing tests cover outpost route bookkeeping, real final boss strikes, checkpoint restoration, attack timing, water travel, recovery, collision, pause/focus input release, and headless rendering. The route test relocates actors and reduces the Warden to one health; it does not establish an unassisted campaign playthrough or boss difficulty.

## Confirmed defect

- **P2: melee visibility ignores closed gates and earth banks.** At the baseline, `RuinedOutpostMap.clearLine` checks ordinary barriers and dressing only. The same query is used by Blade strikes, dash hits, Riposte, and melee enemy attack eligibility. A room 9 fixture placed the player at `(1800,576)` and a four-health enemy at `(1900,576)`, across the shut eastern gate. Both actors' foot colliders were in valid positions; `clearLine` returned true and `clearWaterLine` returned false. A real Blade attack followed by `update(0.1,0,0)` killed the enemy through the gate. Normal outpost spawns do not put enemies behind that gate, so baseline gameplay exposure is limited; new campaign layouts must not inherit this inconsistency. Fix the shared visibility query and preserve the water query's distinct projectile-radius behavior.

This defect is fixed in the campaign change: the shared visibility query now uses the same complete terrain/gate obstruction set with a one-pixel ray radius. `RuinedOutpostMapTest` contains the closed-gate regression; the combined legacy suite passes.

No other severe baseline gameplay failure was confirmed during this source review and test run. This is a scoped result, not a bug-free claim.

## Baseline campaign assumptions

These identified constraints guided the new campaign path; the old outpost constructor remains available for its existing tests.

- **Progress arrays:** `RuinedOutpostGame` allocates `cleared` and `visited` for exactly 12 rooms.
- **Boss identity:** room 9 owns activation, AI updates, collision damage, every player damage path, clear bookkeeping, the exit gate, and boss drawing/HUD. One global `bossDefeated` flag cannot represent multiple independent bosses.
- **Checkpoints and supplies:** death recovery checks `checkpoint == 7`; room 7 heals on entry; room 10 owns one global dressing flag; room 4 owns the transformation tutorial and room 5 its locked destination.
- **Encounter capacity:** room spawning indexes five fixed positions. A room description with more than five enemies throws an index exception.
- **Dimensions and maps:** bank masks use a fixed `18 × 30` array; overview/local-map rendering also assumes 30 columns and 18 rows. The overview's screen positions fit only the original outpost grid.
- **Story termination:** exiting the first boss area calls the only terminal completion transition. Story text, Warden banners, window title, map labels, and scout count are outpost-specific.
- **Transient state:** room changes clear projectiles, corpses, drops, pending strikes, and boss summons. Campaign travel must retain durable story/quest/boss/checkpoint state while still clearing these combat objects.

## Compact campaign acceptance suite

1. Traverse the complete room graph from the awakening area; assert all authored areas are reachable, every connection has a reverse arrival, and each biome has larger-than-viewport areas and optional exploration. Flood-fill using the actual player's foot radius to prove arrival, open exits, NPCs, memories, and camps are reachable without teleporting through collision.
2. Follow every required biome transition. Encounters and story gates reject early travel; clearing ordinary enemies releases their gates; each boss takes a real final player strike and releases only its own progression gate. Defeated bosses stay defeated after backtracking.
3. Visit every checkpoint, die during the following encounter, and restart. Restore living Blob form at a collision-free camp; retain earlier clears, bosses, and story flags; reset the active encounter and transient attacks. Death before any checkpoint and restart after completion reset the appropriate state.
4. Exercise NPC dialogue and optional quest order both before and after finding its clue. Dialogue freezes combat when required, consumes interaction input once, and grants each reward once. Required main-route progress remains possible if optional quests are skipped.
5. Reach both endings through explicit final choices. `CLOSE_RIFT` spends the story core and preserves Rainoray as a living slime; `HUMAN_FORM` keeps the core and seals the rift. Neither depends on optional quest completion or current combat Ichor.
6. Verify water, Blade, dash, and Riposte against ordinary enemies and each boss type; check real collision barriers, zero-aim rejection, pause, death, reversion, and no delayed strike surviving a room transition.
7. Render each biome, each boss, NPC dialogue, the full explored map, and both endings headlessly. Check live window interaction and a packaged launch from an unrelated directory separately; report automated fixtures separately from human or automated live playthrough evidence.
