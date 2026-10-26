# Reincarnation opening and cast — 2026-10-26

## Result

New Journey runs a 15-beat, approximately 56-second voiced opening: the modern relief-van memory, Raphael's reconstruction and ability notices, human-form hint, and the actual outpost reveal. Gameplay then resumes at its existing spawn. Full dialogue, quest/map labels, both chapter scripts, affected voice recordings and NPC art now use Raphael, Shuna, Kaijin, Eren, Rigurd and Souei. Rainoray keeps his name, mask, hair and approved voice.

Stable scene/speaker enums, voice keys, sprite paths and save flags remain intentionally unchanged. Continue loads old checkpoints and progression without forcing the opening. The current narrative is in [Area One story](../design/area-one-story.md); the original proposal remains a historical design reference.

## Checks

- Production and all test sources compile with Java 17 compatibility and `-Xlint:all`.
- All 116 Java test programs passed across the full regression run and focused reruns after corrections. The initial run exposed old cast-framing assumptions and two story fixtures. Art was fixed to preserve close-up portraits and grounded adult silhouettes; the exploration-hint fixture now excludes enemy identification chatter, and the accessibility assertion checks the current spoken line instead of the old script's opening word.
- `ReincarnationOpeningTest` checks voice duration plus padding against every beat, automatic completion, skip/full-playback save equivalence, pause, invalid elapsed input, frozen player/world state and old-save Continue. It exercises real Enter/Escape/X bindings, clicking Skip at 1280×720, 1920×1080 and 2560×1080, music entry, voice cancellation and static reduced-effects frames versus animated normal frames.
- Final staged JAR checks from `/tmp`: `PackagedAssetLoadingTest`, `ReincarnationOpeningTest`, `ChapterTwoVoicesTest`, `FieldObjectsTest`. The asset test also verifies current portraits, Chapter One voices, audio timing, camp art and the new road image. `PixelConsistencyTest` must run from the project because it walks `assets/`; an attempted isolated run failed for that expected working-directory requirement, then passed from the project. `MaraIdleTest` also passed against the staged JAR.
- All 296 runtime voice WAVs (289 dialogue, seven efforts) match their final manifest SHA-256 values and the JAR bytes. All 289 exported script rows match manifest text, speaker and scene. Source masters and test classes are excluded from the playable JAR.
- All 88 generated/changed lines were transcribed for comparison. Sentences agree; ASR uses phonetic alternatives for Rainoray, Eren, Rigurd and Souei. This is not a human pronunciation or acting review. Mono PCM format, duration and non-clipping peak checks pass.
- Actual renderer frames inspected for the modern memory, reconstruction, human-form hint, outpost reveal, Shuna conversation and survivor close-up portraits. Cast art keeps native cells, hard alpha and bounded palettes. The human hint dims the slime beneath it so two opaque bodies do not overlap.
- `git diff --check` passes. No new runtime dependencies. No commit, push or account/dashboard changes.

## Implementation limits

The road memory is one generated illustration with rain, a push-in and a fade, followed by existing animated sprites and a procedural gold seal. It is not a fully animated multi-shot film. Portraits are static; world NPCs retain the existing idle system. NPC voice actors are the existing premade cast. Human listening and a continuous two-chapter playthrough remain outstanding; automated checks do not certify acting, pronunciation or game balance.

Evidence, before-images, WAV backups, test logs and preview files: `/home/ghiankylledomingo/tmp/b2bj-reincarnation-2026-10-26/`. Voice generation staging: `/home/ghiankylledomingo/tmp/b2bj-voices/reincarnation-2026-10-26/`; durable sources/metadata also live under `assets/source/audio/reincarnation-2026-10-26/`.

## Installed preview

Installed `build/B2BJ.jar` atomically (SHA-256 `cf4254a02b34f4a14017f57cdea95885df96a14b2a3c362ab6b164f3b2bd4b79`). Opened a fullscreen separate-save preview, paused at the first frame; Resume/Escape starts the opening. Existing paused normal campaign window was minimized, not closed. The normal `~/.b2bj/campaign.properties` hash is unchanged. Preview log reports no launch errors. The pinned launcher uses the updated JAR on its next launch; an already-open normal game still uses its earlier loaded classes.

Live 1920×1080 capture after launch shows the preview already past the opening, with the new Shuna sprite at camp and normal gameplay/HUD active. Left that session running; did not rewind or replace its progress. The normal campaign save fingerprint remains unchanged.
