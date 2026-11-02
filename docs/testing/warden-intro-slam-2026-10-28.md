# Warden approach and slam — 2026-10-28

After Rainoray asks to pass (Warden dialogue line 2), advancing the conversation now plays an action beat before the Warden answers. Rainoray approaches at 145 world pixels/second, stopping 112 pixels from the boss when the path allows. The Warden raises his arms, slams, and knocks Rainoray back up to 84 pixels with a 28-pixel hop, ground shadow, stone debris, impact sound, brief shake and landing effect. The sequence takes about 4.3 seconds from the southern preview approach.

The dialogue panel clears during the action and the camera frames both characters. The next Japanese recording waits for the landing/recovery. Raphael's idle seal clears this shot so it cannot cover the raised arms/head; his floating dialogue seal returns normally. Boss music continues throughout. Walking and recoil respect collision. Both slime and human forms work. The authored Warden poses are presentation only: boss AI, attack counters, health, Ichor and transformation duration remain unchanged. Pause freezes the scene. X skips it, cancels pending cinematic cues and returns control through the existing camera transition. Existing seen-intro save flags remain respected.

## Validation

- All production and test sources compiled with Java 17 / `-Xlint:all`.
- Twelve focused entrypoints passed: `WardenIntroSlamTest`, `WardenSceneTest`, `DialoguePresentationTest`, `OutpostDialogueTest`, `SageCombatTest`, `SagePresenceTest`, `InnerVoiceOverlayTest`, `FullscreenViewportTest`, `MusicDialogueMixTest`, `ForestChapterTest`, `ReincarnationOpeningTest`, `BossEffectsTest`.
- New checks cover three approach directions, slime/human forms, 10/50/500ms steps, collision, exact cue order, no health/resource loss, dormant boss state, paused walk/windup/recoil/recovery, skipping those phases, next-line audio gating, clear boss framing and normal combat handoff. Rechecked the new test after the final facing adjustment.
- Packaged asset test passed from `/tmp` against the candidate JAR. All 512 runtime WAVs stayed byte-identical.
- Actual renderer/mixer capture: `/home/ghiankylledomingo/Downloads/B2BJ-Warden-Intro-Preview/Warden-step-and-slam.mp4`, 46 seconds, 1280×720/30fps, H264/AAC. Full six-line intro plus 8.93 seconds of combat. Reviewed windup and airborne frames. PCM peak 26781; zero saturated samples.
- Staging, before-build/source/save copies, test logs, preview evidence and installed process details: `/home/ghiankylledomingo/tmp/b2bj-warden-intro-slam-2026-10-28/`.

Installed verified JAR and played the preview in a visible window with active unmuted/uncorked audio output (exit 0). First reopened game exited after its launcher; relaunched through transient user service `b2bj-warden-playtest-20261028` and verified it remains active independently. Startup log empty. Save hash matched immediately across the first restart; later live save differs from the initial task backup, so no claim of whole-task byte equality. No save restoration or save edits performed. `git diff --check` passed.

No full-suite run, complete manual chapter replay or physical listening judgment. Preview staging uses an in-memory campaign and invulnerability for the post-intro fight; the cinematic health checks use normal players. No dependencies, new recordings, paid generation, commit or push.
