# Chapter One flow and Sage recheck — 2026-10-25

Verdict: the implemented Chapter One flow is ready for group playtesting. This is a functional readiness check, not a claim that pacing, difficulty or every voice performance is finished.

## Route checked

A continuous isolated scenario completed: new journey/opening → Mara → first survivor cue and rescue → camp acknowledgement → signal tower/infirmary/supplies → report → cut thorns → remaining rescues → carried-key Continue → cache → three-beds reward → gate checkpoint/death/retry → Warden introduction/fight → camp farewell → gate departure → forest → Continue → return to cleared Outpost.

The scenario used temporary saves. Travel and damage were staged; the Warden ran its actual AI, with invulnerability used to observe a prolonged fight. Damage went through the game's armor/recovery path with deterministic noncritical rolls. The scripted combat duration is deliberately chosen to expose all patterns and is **not** a player completion-time measurement. Existing road traversal, field flood-fill and combat tests supply collision/mechanics checks separately.

Observed outcomes:

- Current objectives lead from Mara through the eastern road. Optional discoveries and rescues do not lock the boss/exit.
- Rescues, cache state, story flags, the one-time camp reward and the carried key survive Continue. Death at the gate checkpoint restores HP/Ichor and preserves progress.
- Warden dialogue acknowledges rescued people. Existing branch tests also cover meeting the Warden before Mara and meeting Mara after victory.
- Sage announces analysis, learns all five attack patterns from released attacks, acknowledges recognition, and gives learned warnings. Phase, <=25% boss-health and victory lines all occurred in the continuous run.
- Focused checks cover critical player health, accepted-hit reactions, warning alternation/cooldowns, learned knowledge after loading, interruption, pause, overlay visibility below the boss bar, and dialogue/boss music.
- All 170 runtime voice files match their recorded SHA-256 manifest. Every exported script and cast assignment also matches the recording manifest and checked-in TSV. Format, clipping bounds, script/clip-key correspondence and bark duration checks pass. This does not replace subjective listening.
- The forest transition, Continue and return to the Outpost retain the open gate and all three rescues.

## Bugs reproduced and fixed

1. A delayed first rescue reaction could say “One name off the missing list. Keep looking.” after all three survivors were already home. The cue now requires that at least one survivor remains missing.
2. A pending damage reaction could freeze through an NPC conversation and play on exit, referring to an old hit. Starting full dialogue now discards that pending reaction.

Regression assertions were run against the prior JAR and failed for both issues, then passed against the corrected code. Existing recording assets and save fields remain compatible.

## Evidence and limits

Continuous scenario source and transcript: `/home/ghiankylledomingo/tmp/b2bj-flow-audit-2026-10-25/ChapterFlowAudit.java` and `results.txt`.

`bash build.sh` passed all 106 test programs and its isolated asset smoke checks. The final `build/B2BJ.jar` also passed SageCombatTest, ChapterOneExperienceTest and the continuous ChapterFlowAudit from `/tmp`, with the JAR first on the classpath. All 170 packaged voice hashes match the manifest; test classes and source-art archives are excluded. No real player save was used, no new voices were generated and no repository changes were committed or pushed.

Next useful check is a new player's uninterrupted run: whether the first rescue is noticed, Blade and armor openings are understood, the gate brazier is found, Sage warns usefully without talking too often, and the final rescue/farewell feels earned. No fresh manual start-to-finish playthrough, measured balance session or listening pass over every performance was performed here.
