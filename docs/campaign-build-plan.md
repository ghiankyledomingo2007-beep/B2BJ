# Complete placeholder campaign

Checkpoint: `cb8c35d`, branch `checkpoint/pre-campaign-20260908`.
Claude's separately committed `outpost-art-fable` remains untouched.

Source: GCD v1.0, especially pp. 9–20. Current user overrides its room-lock layout with connected exploration and requests no new art. Keep Java and the approved water-blade slime attacks.

## Playable scope

Four contiguous regions: Ruined Outpost, Corrupted Forest, Demon Catacombs, Rift Citadel. Each has authored landmarks, optional exploration, a safe NPC/upgrade/checkpoint hub, distinct encounters and a boss. Normal encounters never lock exploration. Defeating a boss permanently unlocks the next region. Final Ichor Golem opens two GCD endings: close the rift and remain slime, or reclaim human shape while maintaining a seal.

Reuse tested Player/Wisp/Guardian/projectile physics. The desktop starts campaign mode; legacy outpost constructors remain regression fixtures. No separate combat engine. Save progression atomically using Java standard library; corrupt saves are preserved and reported. Shards fund Vitality, Capacity, Efficiency and Edge. No XP grind or random morph system.

## Ownership

- World: CampaignWorld, RuinedOutpostMap and geometry tests.
- Story: CampaignStory, story tests and narrative.
- Enemies: CampaignEnemy, Wisp/Guardian profiles and combat tests.
- Save: CampaignSave and storage tests.
- Presentation: CampaignRenderer and render tests.
- Quality: FullCampaignTest and independent audit.
- Root: RuinedOutpostGame integration, Player progression, B2BJ desktop wiring and final verification.

## Acceptance

New Game through both endings; backtracking; four real boss encounters; distinct mob behavior; NPC memory quests; all four upgrade effects; death preserves unlocks/upgrades; save/continue survives process restart; corrupt save never silently overwritten; map and physics agree; existing combat regressions remain green. Automated scenarios complement real desktop input/render checks. Placeholder art and unmeasured full-campaign balance remain clearly identified rather than claimed finished polish.

## TDD evidence

Java 17 assertion-main tests are the existing runner. Initial baseline: 52 tests passed in an isolated temporary build. CampaignEnemyTest and CampaignSaveTest produced intended missing-class compile failures before implementation. Root records integration/progression RED/GREEN with the final verification report. No new runtime dependencies; npm/pip audits do not apply to this Java-only runtime.
