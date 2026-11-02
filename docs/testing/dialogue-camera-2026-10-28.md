# Dialogue camera, floating Raphael and intro music — 2026-10-28

Owner showed the Warden conversation with Rainoray hidden beneath the dialogue strip, requested camera movement and Raphael outside the portrait box, then asked where the music was.

## Changes

- Boss introductions retain the initial reveal, then ease between speaker positions over 0.85 seconds. Rainoray gets a player-focused shot above his subtitles; Warden dialogue and Raphael's analysis frame the boss. This also applies to the forest boss, whose reveal finishes before moving to its first speaker. Native pixel scale is unchanged.
- Pause freezes the pan. Skipping mid-pan returns from the current position over the existing 0.6 seconds and restores the exact gameplay camera before controls resume. Reduced Effects holds a stable player-focused frame instead of panning between speakers.
- During full Raphael dialogue, his animated, voice-reactive gold seal floats above the left edge of the dialogue window. Removed the portrait panel and duplicate top seal, and reclaimed that space for subtitles. During other speakers' boss-intro lines, the idle seal stays at the top without the combat HUD's downward offset, so it clears the Warden's face. Gameplay banter remains unchanged.
- Saiseiki remains the installed boss track. Introduction gain was 0.35, then reduced again by speech ducking; raised it to 0.75 (about 6.6dB higher) while retaining the existing smooth speech duck and continuous intro-to-fight fade. Renamed the settings slider from “Ambient music” to “Music” because it controls every music source. No new music downloads or voice generation; all 512 runtime WAVs are unchanged.

## Verification

The new camera assertion failed against the prior JAR because Rainoray's line left the camera locked on the Warden. The raised-intro-level assertion also failed on the prior JAR. Both pass on the new build.

All production and test sources compiled with Java17 and `-Xlint:all`. Twelve focused entrypoints passed: DialoguePresentationTest, WardenSceneTest, OutpostDialogueTest, SageCombatTest, SagePresenceTest, InnerVoiceOverlayTest, FullscreenViewportTest, CampaignOptionsTest, MusicDialogueMixTest, ForestChapterTest, ReincarnationOpeningTest and PackagedAssetLoadingTest. Final camera composition refinements were rechecked against the affected camera/presence/fullscreen tests and the refreshed packaged JAR.

Checks cover speaker movement and visibility from a southern approach, pause, mid-pan skip, restored gameplay camera, forest reveal, floating/animated seal, absence of a duplicate, reduced effects and Skip hit targets at 1280×720, 1920×1080 and 2560×1080. Packaged assets passed outside the repository; all runtime audio hashes match. `git diff --check` passed.

Captured the actual renderer and mixer for 42 seconds: Warden introduction, all six lines and 9.23 seconds of combat. Reviewed settled Rainoray and Raphael frames. Preview PCM peak26900/32768 with zero saturated samples. Capture uses an in-memory campaign, staged travel and invulnerability; it does not read or write the player save.

- Preview: `/home/ghiankylledomingo/Downloads/B2BJ-Dialogue-Preview/Dialogue-camera-and-music.mp4`.
- Rainoray frame: `line-1.png`; Raphael frame: `line-3.png` in the same folder.
- Sources, previous build/source backups, test logs, save backup, capture and launch evidence: `/home/ghiankylledomingo/tmp/b2bj-dialogue-camera-2026-10-28/`.

Installed verified `build/B2BJ.jar`, played the preview through an active unmuted audio stream, then reopened the regular fullscreen game. Startup log was empty and the campaign save hash was unchanged.

Not checked: full test suite, uninterrupted human chapter playthrough, physical listening judgment. Existing Japanese voices, story text, gameplay balance and save format are unchanged. No commit/push. The adult-office voice transition remains deferred.
