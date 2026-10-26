# Outpost Warden pacing — 2026-10-25

Chapter One only. Reuses the existing Warden motion/action sheets, effects and awakening audio.

- Target slam reaches its raised release pose earlier and holds it for the final 28% of the warning. Other attacks retain their distinct authored tells and committed geometry.
- Recovery is 1.5 seconds after target/sweep, 2.0 after charge, 1.85 after shockwave and 1.9 after fissure. Phase two subtracts 0.2 seconds only after its transition completes.
- Crossing half health preserves the current attack, hit area and full recovery. A stationary, harmless 1.8-second power-up follows: inward motes, cyan body light, ground dust and the existing low awakening cue.
- Phase two starts with the full 40-mote/10-debris burst, a brief white-to-cyan flash, impact audio and a 0.35-second camera kick plus cyan screen pulse and a fissure with its full 1.4-second warning. No new summons during the transition; existing enemies remain active. Pause freezes the transition; death cancels it.

## Evidence

`recovery.png`, `phase-transition.png`, `phase-two-fissure.png` are actual 1280x720 B2BJ framebuffer captures. The optional WardenPacingTest capture fixture positions the player, waits out the encounter title and sets boss state/health to inspect the sequence; these are controlled captures, not a recorded manual fight. New summons are suppressed while waiting out the title in this fixture only.

WardenPacingTest checks threshold crossings in every attack/state, preserved target and radius, recovery timing, a single harmless transition, full first fissure warning, pause, summon grace, death and unchanged phase rules for other bosses. Guardian animation, mobility, presentation, effects and arena checks also pass. All 98 test programs passed across the build and resumed run after correcting an older enemy-cue fixture. Final JAR asset loading passed outside the repository. Logs are in `/home/ghiankylledomingo/tmp/b2bj-warden-pacing/`.

Not checked: full manual combat balance or Windows playthrough. Longer heavy-attack recovery intentionally makes those attacks more punishable. Changes remain local and unpushed.

## Impact restoration

User requested the immersive shake/effects restored. The phase release now emits GUARDIAN_PHASE_TWO once, separately from damaging attacks. B2BJ routes it through the existing slam shake and cyan feedback; normal slams retain their original shake. The previous pass had reduced the phase burst to 24 motes/6 debris and added no release shake; those reductions are reverted. Reduced-effects rendering continues to suppress camera shake and screen flashes.

Six focused checks passed after this correction: WardenPacing, CombatFeedback, WardenFx, B2BJIntegration, CampaignOptions and WardenScene. The campaign integration check verifies the release shake, pulse, burst count, expiry and ordinary slam shake. The preceding 98-test result applies to the prior pass; the full suite was not repeated for this feedback-only correction.

Portrait correction: Warden dialogue now samples the complete first 128px motion cell into the 64px portrait, replacing an obsolete 64px crop that cut off the enlarged sprite. Reviewed `warden-conversation.png`; OutpostDialogueTest passed against classes and the final JAR from /tmp.

## Armored summons

Chapter One Warden now summons OUTPOST_GUARD instead of OUTPOST_SCOUT. Uses existing armored guard art, directional shield-bash animation and normal guard stats (80 HP rather than 64; contact damage remains 20). Counts, cadence, spawn safety, phase-transition grace and summon rewards/cleanup remain unchanged. Other biomes and legacy arena fixtures retain their prior summon type. The remnant sheet is itself slime-shaped; no renderer change was made.

Five focused checks passed: CombatAudit, SummonSafety, OutpostVariety, WardenPacing and AbsorbedTrait. CombatAudit now checks the actual campaign summon kind/role and verifies killing/cleanup over three spawn cycles; it passed again from the rebuilt JAR outside the repo. `guard-summon.png` is a controlled actual-game capture after a real timed summon, with player healing/repositioning for review. Full balance and Windows runs were not repeated.
