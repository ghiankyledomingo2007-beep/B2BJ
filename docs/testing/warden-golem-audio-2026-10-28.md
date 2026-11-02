# Warden golem voice and battle music — 2026-10-28

Owner requested a voice that fits the bulky Outpost Warden, richer attack sound, and actual Tensura music. Scope: six Warden lines, their existing shared effects, boss music, cast/source records and Claude's trailer handoff. Both protagonists, NPC voices, story scripts and player saves are preserved.

## Warden

The previous calm Japanese narrator voice is replaced by an original Japanese stone-golem Voice Design profile. Three candidates were generated. Candidate1 had the lowest rough pitch estimate but two unclear phrases in transcription; candidate2 retained bass weight and a correct full Japanese transcript; candidate3 was higher. Candidate2 is the working selection, not an owner-approved audition or an anime actor clone.

- Profile: `FlgZOCPLervz9WebHOWs`, Eleven v4, stability0.5, similarity0.85, seed281028. Exact design prompt, creation metadata, selected audition and generation requests are under `assets/source/audio/warden-golem-ja-2026-10-28/`.
- Replaced `warden_01`, `warden_03`, and `bark_warden_voice_{stand,line,gate,order}`. All six final Japanese transcripts match the intended lines. No dialogue pitch/speed manipulation.
- Existing processor now adds a105Hz body band,190/390/760Hz stone resonances and quiet41–247ms reflections, with260ms tail. The vulnerable confession retains the same voice with less resonance. Direct speech stays dominant; runtime masters target-18LUFS/-4dBTP.
- The shared treatment also changes twelve procedural Warden sounds: windup, sweep, rush, fracture, strain and slam, each with two variants. Regenerated those files; other effect families are unchanged.
- Warden fallback subtitle slots increase to4.2seconds for the3.70second confession. Normal playback already waits for the full recording.
- Bank remains361 clips (343 dialogue +18 combat efforts). All355 non-Warden runtime WAVs are byte-identical to the before-task hash inventory, including the selected Rainoray and Raphael voices/effects.

## Downloaded soundtrack

Installed the full142.73-second **Saiseiki / 最盛期** boss track, credited to Hitoshi Fujima (Elements Garden), disc2 track29 of *Tensei Shitara Ongaku Datta Ken*. Track/album identity was checked against the [official Bandai Namco Music Live catalog](https://catalog.bandainamcomusiclive.co.jp/release/68280/).

The corrected official YouTube Topic URL is `https://www.youtube.com/watch?v=Zw1O9arSL8U`; its audio download returned403. A public [SoundCloud community upload by Nickolas Mariano](https://soundcloud.com/nickolas-mariano/57a) downloaded successfully. This is not a publisher download. Raw stereo source, source/runtime hashes, URLs and exact normalization filter are in `assets/source/audio/tensura-music-2026-10-28/` and its `source.json`.

The game master is mono PCM16/22050Hz, -18LUFS/-4dBTP target, with30ms start and350ms end fades. Boss music gain changes from3.0 to1.8; existing user music volume, fight fade and speech ducking remain active. This is the full track repeating at its normal ending, not a newly composed seamless loop. Title, story and exploration music are unchanged.

The available official-artist SoundCloud source for **Nameless Story** returned a DRM-protection error. No bypass attempted and no Nameless Story file installed. Searches for quieter tracks did not yield a reliable source; no unrelated replacements were added. Local school use was requested; public redistribution permission for the downloaded recording was not verified.

## Verification

- All production and test sources compiled with Java17 and `-Xlint:all`.
- Seven focused test entrypoints passed: `ChapterOneVoicesTest`, `GameAudioTest`, `CompanionCadenceTest`, `SageCombatTest`, `BossSfxTest`, `WardenSceneTest`, `PackagedAssetLoadingTest`. Packaged checks ran outside the repository against the candidate JAR and include nested art/dialogue/overlay checks.
- `warden_voice_fx.py --check`, `generate_boss_sfx.py --check` and `check_japanese_bank.py` passed. All361 runtime voice hashes, Warden effect files and boss music match the candidate JAR. `git diff --check` passed.
- Captured16seconds of the actual campaign renderer and audio mixer, including Warden dialogue and boss music. Preview PCM peak23026/32768, zero saturated samples, RMS-19.92dBFS. Reviewed the dialogue frame. Capture used an in-memory session without reading/writing the player's save.
- Preview: `/home/ghiankylledomingo/Downloads/B2BJ-Warden-Preview/Warden-voice-and-music.mp4`. Claude's stereo music copy: `/home/ghiankylledomingo/Downloads/B2BJ-Trailer-Audio/Saiseiki-full-source.wav`.
- Installed the verified candidate as `build/B2BJ.jar`, keeping the previous JAR and save in `/home/ghiankylledomingo/tmp/b2bj-warden-golem-2026-10-28/`. Direct ffplay preview had an active, uncorked, unmuted audio stream with an empty error log. Reopened the normal game fullscreen; it remained running after startup, with an empty error log and the campaign save unchanged. Launch evidence is in that folder's `delivery.json` and logs.

Not checked: full126-program suite, continuous manual chapter playthrough, native-speaker acting judgment, or physical speaker/headphone listening. Automated transcription is not proof of natural acting. No commit, push or trailer publication. Adult-office-to-slime voice transition remains deferred.
