# Claude trailer handoff — 2026-10-28

Owner: Ghian. Project: Blob to Blade / B2BJ. Repository: `/home/ghiankylledomingo/Domynix/gamedev-vault/Projects/B2BJ`.

## Current direction

All spoken character dialogue, boss lines, exploration banter and combat efforts are now Japanese. **Keep English subtitles, menus and UI.** This covers both playable chapters: Ruined Outpost and the forest. The owner is preparing a trailer with Claude; this handoff is an asset/script inventory, not a request to create or publish a trailer yet.

## Read these first

- [Full Japanese voice list](japanese-voice-lines.tsv): all 361 clips, filename, internal speaker ID, displayed character, scene, English subtitle, spoken Japanese, kind and status. 343 dialogue clips + 18 combat efforts.
- [Cast profiles](japanese-voice-cast.json): exact voice IDs, models, settings and seeds. Reuse them; do not silently choose another Rainoray or Raphael voice.
- [Runtime manifest](../art-review/voice-auditions/runtime-voices.json): installed clip hashes, durations, original text, processing and source paths.
- [Chapter One story](area-one-story.md), [Chapter One English script](chapter-one-voice-lines.tsv), [Chapter Two English script](chapter-two-voice-lines.tsv).
- [Opening verification](../testing/opening-polish-2026-10-27.md).

## Availability — Japanese bank installed

All 361 runtime WAVs in `assets/audio/dialogue/` are Japanese: 343 dialogue clips and 18 combat reactions. Twelve additions on 2026-10-28 give Rainoray replies to all seven first-enemy identifications and both boss-analysis announcements, plus three rotating Beelzebub calls. The later Warden revision replaces all six Warden recordings with an original designed stone-golem voice; the other 355 recordings, including both protagonists, remain byte-identical. The 15 approved opening clips are retained. The original Japanese bank passed all126 test programs; subsequent companion and Warden updates passed focused voice, mixer, scene and outside-repository packaged-asset checks. The installed `build/B2BJ.jar` contains all361 clips. A process launched before an update must restart to load it.

Final raw MP3s, generation requests and transcripts are in `assets/source/audio/japanese-bank-2026-10-28/`. The twelve companion additions are in `assets/source/audio/companion-jp-2026-10-28/`; the six replacement Warden takes are in `assets/source/audio/warden-golem-ja-2026-10-28/`. Use each runtime manifest record's source path, since the earlier Warden recordings are superseded. Final script/status is in the 361-row TSV. English masters and rejected takes are kept outside runtime in the local staging backup. Do not use intermediate takes from `/home/ghiankylledomingo/tmp/b2bj-japanese-bank-2026-10-28/` for the trailer.

Companion additions: [Reply/audio verification](../testing/companion-audio-2026-10-28.md). Bank audit: [Japanese voice verification](../testing/japanese-voice-bank-2026-10-28.md). Offline check: `python3 tools/audio/check_japanese_bank.py`. For API field references, see the [ElevenLabs TTS documentation](https://elevenlabs.io/docs/api-reference/text-to-speech/convert) and [shared voice documentation](https://elevenlabs.io/docs/api-reference/voices/voice-library/share).

## Cast and effects

Rainoray: approved Japanese B voice (`kkelxpWuWPklDYfIS9Vf`), Eleven v4, stability0.5, similarity0.9. Raphael/Sage: approved Japanese voice (`Xk94e1zRK34B68WV891i`), Eleven v4, stability0.7, similarity0.85; preserve the approved layered echo/chime effect. Both are synthetic game performances, not original anime recordings or an official production.

Five NPC roles use distinct native Japanese library voices matched to their ages and delivery: Shuna gentle/protective, Eren youthful scout, Kaijin older/husky craftsman, Rigurd mature/dignified, Souei quiet/low. These NPC voices are role casting, **not clones of their anime actors**. See the cast JSON for exact identities.

Outpost Warden now uses the original designed Japanese stone-golem voice `FlgZOCPLervz9WebHOWs`, Eleven v4, stability0.5, similarity0.85, seed281028. Deep bass and deliberate commands come from the voice itself; low stone resonance and a short chamber tail sit underneath clear speech. No dialogue pitch or speed shift. The wounded confession uses less resonance. This replaces the old Taniguchi narrator voice. Candidate2 is the working selection for weight and transcript clarity, pending Ghian's listening judgment. Twelve Warden attack/exertion sounds use the matching updated resonance. [Verification](../testing/warden-golem-audio-2026-10-28.md).

Runtime audio: mono PCM16, 22050Hz. Dialogue target -18LUFS/-4dBTP; combat efforts shorter and separately levelled. Keep voices clear over music. Do not speed or pitch-shift dialogue to fit the edit. Raw MP3 takes are retained for higher-rate trailer mastering. Use each installed manifest record's `source` path; older dated source folders can include superseded English takes.

## Quick trailer audition picks

These are final installed WAVs. The full TSV provides Japanese speech and matching English captions for every file.

| Character | Clip | English caption |
|---|---|---|
| Rainoray | [bark_warden_victory.wav](../../assets/audio/dialogue/bark_warden_victory.wav) | The road's open. |
| Rainoray | [bark_beelzebub_call.wav](../../assets/audio/dialogue/bark_beelzebub_call.wav) | Devour them. Beelzebub! |
| Raphael | [bark_warden_start.wav](../../assets/audio/dialogue/bark_warden_start.wav) | Keep moving. I'll analyze its attacks. |
| Shuna | [camp_09.wav](../../assets/audio/dialogue/camp_09.wav) | Someone called from the storehouse south of the road. You might fit under its fence. |
| Kaijin | [survivor_tobin_06.wav](../../assets/audio/dialogue/survivor_tobin_06.wav) | For a lamp? I'll crawl. Thank you. |
| Eren | [survivor_ilse_01.wav](../../assets/audio/dialogue/survivor_ilse_01.wav) | Stay back. I've got one good arm and I'm not afraid to use it. |
| Rigurd | [survivor_venn_06.wav](../../assets/audio/dialogue/survivor_venn_06.wav) | Shuna. Good. Then I'll go and be shouted at properly. |
| Souei | [forest_sedge_wait_03.wav](../../assets/audio/dialogue/forest_sedge_wait_03.wav) | The rope is ready. Clear the roots and I'll bring them through. |
| Outpost Warden | [warden_01.wav](../../assets/audio/dialogue/warden_01.wav) | CONTAMINATED LIFEFORM. PASSAGE DENIED. |

## Opening and other ready assets

- Current 67.6-second game-rendered opening video: `/home/ghiankylledomingo/Downloads/B2BJ-New-Opening/Opening-preview.mp4` (1280×720/30fps, H264/AAC).
- Selected Rainoray audition/profile: `/home/ghiankylledomingo/Downloads/Rainoray-Japanese-Voice-Test/`.
- Selected Raphael audition/profile: `/home/ghiankylledomingo/Downloads/Sage-Japanese-Voice-Test/`.
- Opening Japanese raw sources: `assets/source/audio/opening-ja-2026-10-27/` and office revisions under `assets/source/audio/office-origin-2026-10-27/`.
- Office keyframe atlas: `assets/ui/opening/office-night-atlas.png`; generated original and prompt under `assets/source/ui/opening/`.
- Opening sound effects: `assets/audio/opening_*.wav`; authored generators under `tools/audio/`.
- Latest verified playable build: `build/B2BJ.jar`. Opening replay helper: `tools/PreviewOpening.java`; this uses an in-memory session and does not touch the real save.

## Downloaded music — 2026-10-28

**Saiseiki / 最盛期** by Hitoshi Fujima (Elements Garden), from *Tensei Shitara Ongaku Datta Ken*, disc2 track29, is installed as the full142.73-second boss track. [Official album catalog](https://catalog.bandainamcomusiclive.co.jp/release/68280/).

- Claude's stereo source copy: `/home/ghiankylledomingo/Downloads/B2BJ-Trailer-Audio/Saiseiki-full-source.wav`.
- Repository source: `assets/source/audio/tensura-music-2026-10-28/saiseiki-source.wav`; provenance and processing are in adjacent `source.json`.
- Game master: `assets/audio/boss_music.wav`, mono PCM16/22050Hz, -18LUFS/-4dBTP target; existing music fade and dialogue ducking remain active. Title, exploration and story music were not replaced.
- Download came from [Nickolas Mariano's community upload](https://soundcloud.com/nickolas-mariano/57a), not the publisher. The official YouTube Topic audio download returned403. Preserve this distinction in credits/provenance. Local school use was requested; public redistribution permission for this recording has not been verified.
- **Nameless Story was not downloaded:** the available Takuma Terashima SoundCloud source returned a DRM-protection error. No bypass attempted. Do not assume that file exists or substitute an unrelated track.
- Latest in-game Warden preview: `/home/ghiankylledomingo/Downloads/B2BJ-Warden-Intro-Preview/Warden-step-and-slam.mp4` (46 seconds). Includes Rainoray approaching, the Warden winding up/slamming, a short airborne knockback and recovery between dialogue lines; also speaker camera movement, floating Raphael, louder intro music and the continuous handoff into combat. Voice/source profiles are unchanged. [Verification](../testing/warden-intro-slam-2026-10-28.md).

- Post-Warden victory scene: `/home/ghiankylledomingo/Downloads/B2BJ-Warden-Victory-Preview/Warden-victory.mp4` (19seconds). Actual water-projectile finishing hit, slower collapse, gate opening camera, Rainoray/Raphael Japanese exchange, story music and return to gameplay. Existing voice profiles and clips unchanged. [Verification](../testing/warden-victory-2026-10-28.md).

## Deferred opening voice transition

The owner wants the pre-reincarnation office worker to have an adult Japanese male voice, followed by a synchronized old/new-voice overlap during one sentence as the slime body finishes forming. Proposed line: “Wait… is that my voice?” Old voice starts, both speak the same syllables briefly, then only the selected Rainoray voice remains. **Planning only; not implemented.** The owner explicitly deferred this while returning to gameplay. Do not describe the existing opening as already having this transition.

## Collaboration boundaries

Read the home `.agents/AGENTS.md` before working. This is Ghian's personal project: update only the canonical personal B2BJ note. Preserve current uncommitted game work. No save replacement, new cast, trailer publishing or remote push without the corresponding owner instruction. Voice API credentials are configured locally; never copy secrets into trailer files, scripts or notes. The owner authorized the existing ElevenLabs API account for game voices; use the API rather than the studio dashboard.
