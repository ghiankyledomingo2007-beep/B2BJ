# Guardian native preview and checkpoint — 2026-11-02

The previous browser Fight flow used simplified player shots. It is now explicitly archived for movement/VFX study; its damage figures are not evidence of balance against the real skill kit.

`tools/guardian-preview/` builds a separate real-engine practice window. Original Rainoray/slime attacks, rendering, animation, sound, parry, dash, Void Sever and Beelzebub are retained. The in-memory campaign has no save path. Both forms/phases are selectable. Unlimited blade duration is the training concession; ultimate readiness and corpse absorption are earned normally. The window starts paused; Esc resumes.

The moving Guardian has a committed stepping fist, frontal backhand and shoulder rush, then detached fists and a turning chest beam. Movement and beam damage/rendering respect the actual map. Core recovery allows real combo timing. Half-health transition is safe; attack effects use the character's violet/gold/cyan palette. Old move-specific Raphael warnings are suppressed in the draft because they describe the previous attacks.

## Claude sound bank

Actual Claude Code `claude-opus-5-5`, max effort, authored an original deterministic Python generator with sixteen cues and a grouped audition. CLI render permission blocked execution; the parent rendered locally and fixed three failing envelope/loudness checks without loosening assertions. Final `--check`: 203/203 pass. `--selftest`: seven positive/negative cases pass. See `tools/guardian-preview/sound/SOUND_DESIGN.md` and the manifest/check report.

The preview reads authored family gains from the manifest and retains the game sample gain. Fixed pitch preserves contact timing. Two old windup/strain alternate files are omitted from this separate JAR to prevent mixed old/new takes. Fist impacts and camera shake synchronize at .20/.68 seconds; phase bloom at 1.2 seconds. Player sounds/voices/music remain intact.

## Verification

- Java 17 lint compile of the separate preview succeeds.
- GuardianDraftTest passes pursuit, lock/commitment, hit geometry, parry, transition, 60-second terrain/leash simulation, original input paths, no-save isolation, and contact/phase-shake timing.
- GuardianAudioTest passes 16 packaged takes, authored duration within half a PCM sample, fixed pitch, non-silence, peak/DC/edge checks, immediate attack onset and mute.
- Thirteen original combat/audio/terrain regression programs pass against the preview.
- Ten actual-render fixture PNGs generated; human beam and slime backhand visually inspected. Title card suppressed in this practice view; beam stops at cover.
- 742 existing packaged assets stay byte-identical. Eight existing audio files replaced, eight new files added, two stale alternate cues omitted in the preview only.
- Installed `build/B2BJ.jar` remains SHA256 `4732544b457972069e0e2188742302fef667b8fb04737997572e6c028a7890fe`.
- Chrome audition reached readyState4, playing unmuted at .8 volume; both images loaded and no horizontal overflow. No claim of physical speaker/headphone listening.
- Native launch initially lacked DISPLAY; relaunched with the user's existing desktop environment. Process alive and startup log empty. No native physical-input playthrough.

## GitHub checkpoint scope

Owner requested commit/push of pending game work. Current source and all test sources compile with Java17. Running all142 test programs against newly compiled source plus the installed asset JAR gives140 passes and two previously documented failures: RuinedOutpostGameTest (route target never hittable) and TideLayerTest (moving-player impact anchor). Neither is changed as part of this preview. No fresh release or Windows installer built. The existing Windows workflow also needs the opening decoder classpath when making a future installer.

The checkpoint includes pending Japanese voice/music assets, opening, menu/HUD art, player-combat polish, tools/tests and this isolated Guardian draft. Source text scan found no credential-pattern matches; no npm/Python package manifests exist here. No forced merge to main, deploy, tag or release.

Preview: http://127.0.0.1:4517/artifact/3833bd09a217/native.html
Runtime: /home/ghiankylledomingo/tmp/b2bj-guardian-native-2026-11-02/runtime/Guardian-preview.jar
Evidence: /home/ghiankylledomingo/tmp/b2bj-guardian-native-2026-11-02/

Remaining: human balance, subjective audio audition and approval before installing this Guardian encounter over the main game. Existing installed player kit is unchanged by the draft.
