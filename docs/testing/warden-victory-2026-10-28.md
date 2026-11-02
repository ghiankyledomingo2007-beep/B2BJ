# Warden victory cutscene — 2026-10-28

Defeating Chapter One's Warden now starts a short automatic aftermath. The camera moves onto the boss while all eight death poses play over two seconds, accompanied by the existing debris/energy effects and a stone fracture sound. It then pans to the eastern gate, holds for its opening animation and creak, and returns to Rainoray. Rainoray says “The road's open.” Raphael answers “And you are still here.” Their existing Japanese recordings play in full with English subtitles; a sentence already being spoken may finish first. Queued combat replies are discarded. Combat music fades into the existing story theme.

Gameplay inputs and nearby enemies wait during the sequence. Live projectiles/hazards are retired; the scene consumes no health, Ichor or transformation time. Camera returns smoothly to gameplay. X or the top-right Skip target exits safely; Escape pauses. Reduced Effects omits the gate pan and shake. Location labels and the idle seal stay clear of the collapse shot. Both player forms work.

Boss reward and cleared-boss save are recorded immediately, so closing/returning to title mid-scene retains the open route. Skip does not duplicate rewards. Continuing an already-cleared save does not replay the scene. Gate inspection and travel into the forest retain their original dialogue/progression. The forest boss and legacy laboratory mode keep their existing victory behavior.

## Verification

All production/test sources compiled under Java17 with `-Xlint:all`. Thirteen focused entrypoints passed: `WardenVictorySceneTest`, `CampaignVictoryTest`, `AttackSlotsTest`, `ChapterOneVoicesTest`, `WardenIntroSlamTest`, `DialoguePresentationTest`, `GuardianDeathLoopTest`, `ForestChapterTest`, `CompanionCadenceTest`, `MusicDialogueMixTest`, `SageCombatTest`, `OutpostDialogueTest`, `FullscreenViewportTest`. New coverage includes all death poses, gate timing, complete recordings, both forms' resource preservation, pause/skip at six phases, keyboard/mouse skip, Reduced Effects, rewards, duplicate impacts, Continue and forest travel. Two existing flow fixtures now skip the new scene before testing subsequent interactions.

The actual renderer/mixer preview uses a real water-projectile finishing hit against a staged one-HP Warden: `/home/ghiankylledomingo/Downloads/B2BJ-Warden-Victory-Preview/Warden-victory.mp4`. 19 seconds, 1280×720/30fps, H264/AAC; victory sequence lasts10.9 seconds with the full voice recordings. God mode is used only for capture setup, not the model resource assertions. Reviewed collapse, gate and Raphael frames. All512 runtime WAVs unchanged.

Staging, before-source/build/save copies, scoped diff, logs and delivery evidence: `/home/ghiankylledomingo/tmp/b2bj-warden-victory-2026-10-28/`.

Packaged asset verification passed from `/tmp` against the candidate JAR. Capture PCM peak28017, zero saturated samples. `git diff --check` passed. Installed the verified JAR, played the preview in a visible window with active unmuted/uncorked output (exit0), then reopened the regular fullscreen game through transient user service `b2bj-victory-playtest-20261028`. Verified active after launcher exit, empty startup log and identical save hash across the restart. No saved progress was restored or rewritten by tools.

No full-suite or full manual campaign replay, physical listening judgment, new audio generation, commit or push.
