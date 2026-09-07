# Campaign quality assessment

Baseline: `cb8c35d`, 2026-09-08. Production sources and the running JAR were not changed by this assessment.

## Baseline evidence

All 52 existing assertion-based test programs passed after compiling source and tests with `javac --release 17 -Xlint:all` into `/tmp/b2bj-quality-baseline.ByF5Y8`. Tests ran headlessly from the project directory, so image loading could use the existing filesystem assets. This run does not independently verify packaged assets from another working directory.

Existing tests cover outpost route bookkeeping, real final boss strikes, checkpoint restoration, attack timing, water travel, recovery, collision, pause/focus input release, and headless rendering. The route test relocates actors and reduces the Warden to one health; it does not establish an unassisted campaign playthrough or boss difficulty.

## Confirmed defect

- **P2: melee visibility ignores closed gates and earth banks.** At the baseline, `RuinedOutpostMap.clearLine` checks ordinary barriers and dressing only. The same query is used by Blade strikes, dash hits, Riposte, and melee enemy attack eligibility. A room 9 fixture placed the player at `(1800,576)` and a four-health enemy at `(1900,576)`, across the shut eastern gate. Both actors' foot colliders were in valid positions; `clearLine` returned true and `clearWaterLine` returned false. A real Blade attack followed by `update(0.1,0,0)` killed the enemy through the gate. Normal outpost spawns do not put enemies behind that gate, so baseline gameplay exposure is limited; new campaign layouts must not inherit this inconsistency. Fix the shared visibility query and preserve the water query's distinct projectile-radius behavior.

No other severe baseline gameplay failure was confirmed during this source review and test run. This is a scoped result, not a bug-free claim.

## Campaign assumptions requiring replacement

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
