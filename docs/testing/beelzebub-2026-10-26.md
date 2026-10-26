# Beelzebub verification — 2026-10-26

Implemented [human-form mass absorption](../design/beelzebub.md). Source compiled with Java 17 compatibility and lint checks. New `BeelzebubTest` passed, along with these 16 focused regression programs:

`AbsorptionTest`, `AbsorbedTraitTest`, `RainoraySkillsTest`, `BladeCombatTest`, `SlimeCastingTest`, `ReversionSafetyTest`, `PassiveHealingTest`, `PlayerCheatsTest`, `FieldObjectsTest`, `CampaignMenuTest`, `FullscreenViewportTest`, `HudOverhaulTest`, `SageCombatTest`, `ReincarnationOpeningTest`, `GameAudioTest`, `CombatVoiceTest`.

The new assertions cover multi-body movement and exact rewards, range and walls, empty/live/slime rejection, cooldown, boss-add healing exclusion, corpse expiry, movement/dash/attack/charge/skill/hit/death/reversion interruption, dialogue, reset, pause, R key latching, cast animation, loop stopping and Reduced Effects. Renderer captures checked at 1280×720 and 1920×1080. A staged actual Outpost scene with six corpses verifies the effect, HUD and completed harvest; this uses unlimited Blade and disabled saves, not a normal progression playthrough.

Staged JAR passed `BeelzebubTest` and `PackagedAssetLoadingTest` from `/tmp`, with the JAR first on the classpath. All 604 runtime asset files, including existing voices, are byte-identical to the previous JAR. No test classes or source-art archives are packaged. `git diff --check` passed. Full regression suite was not repeated; this pass used the focused checks above.

Installed `build/B2BJ.jar`, SHA256 `dc7b1fc5ff9983c9ab8f9e18135b3554837e88637bbebaad4d6083531119b727`. Pinned launcher uses this artifact on its next launch. Earlier game processes retain their loaded classes and were minimized, not replaced.

Evidence, previous JAR, source snapshots, regression log and separate preview runner:
`/home/ghiankylledomingo/tmp/b2bj-beelzebub-2026-10-26/`.
Preview is paused with six prepared corpses; Esc resumes, then R casts. It is an in-memory testing session with saving disabled and does not load the normal campaign save. Existing saves and old sessions remain intact.

Remaining checks: human judgment of timing, effect strength and balance during ordinary combat. No new voice line was generated, and no commit or push was made.
