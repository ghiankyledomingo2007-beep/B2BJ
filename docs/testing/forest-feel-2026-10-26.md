# Forest combat feel — 2026-10-26

User requested less chaotic attack indicators, slightly larger Chapter Two enemies, more readable attacks, easier combat and a meaningful dash cooldown. Current values and presentation rules are in [attack feel](../design/attack-feel.md#2026-10-26--forest-readability-and-easier-pacing).

Changes cover Player dash cadence; Wisp anticipation, pursuit and recovery; CampaignEnemy damage/hazard lifetime; RuinedOutpostGame attack slots and damage; Guardian pattern gaps; B2BJ sprite scale, body motion, bars, hover and overlay calls; MobFx claw motion and pre-cast thorns; Telegraphs organic root/seed effects. Runtime assets, voices, world layout, save schema and progression remain unchanged.

Validation:

- Java 17 lint compile passed. `ForestFeelTest` checks both-form dash spacing, pause and vulnerability, larger silhouettes with exact 2×2 pixels, head-aligned health bars and shaman preparation at its fixed target, including Reduced Effects.
- 31 distinct focused test programs passed across the initial and follow-up runs: forest attacks, mob effects, attack-slot fairness, warning/damage geometry, Briarheart, forest story/learning/save flow, enemy facing, Outpost approach, pursuit, movement, Blade/Slime combos, projectiles, absorption, Beelzebub, reversion, stats, identification, viewport, art, audio, cheats and animation.
- Two existing tests initially failed because their fixtures expected the old dash cooldown (`BladeCombatTest`, `AbsorptionTest`); the same timing assumption in `DashVariantsTest` was updated before execution. Updated the expected wait and kept assertions for vulnerability, pause and combo behavior. Final runs passed. No gameplay exception was bypassed.
- Final staged JAR passed `ForestFeelTest`, `DashVariantsTest`, `BladeCombatTest`, `BeelzebubTest`, `AbsorptionTest` and `PackagedAssetLoadingTest` from `/tmp`. All 604 runtime asset files match the previous JAR byte-for-byte; test classes and source-art archives are excluded.
- Reviewed actual game captures of wolf preparation/contact, shaman roots, Briarheart preparation and full-screen preview. `git diff --check` passed.

Installed JAR SHA256: `5fa21d48a32cea61dbefc184d7a671f327cee66f3eb1b27f337beeee3cdbe334`.

Full-screen preview PID217429 starts paused in Chapter Two at Ferry Refuge. Esc resumes. It uses an in-memory testing session, with saving disabled and god mode off. Previous game sessions were minimized and retained. Normal save fingerprint was checked through installation/launch.

Evidence, scoped before/after diff, previous JAR, logs, captures and preview runner:
`/home/ghiankylledomingo/tmp/b2bj-forest-feel-2026-10-26/`.

No full regression rerun, continuous human difficulty test, fresh audio generation, commit or push. Difficulty and the preference for physical cues over geometric overlays still need user playtesting.
