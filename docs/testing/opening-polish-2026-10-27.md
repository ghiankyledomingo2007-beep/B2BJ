# Opening animation and Japanese voices — 2026-10-27

## Implemented sequence

The owner changed the origin to an overworked Japanese office worker. Four matched office poses show typing, chest pain, bending forward and collapse at the desk, with camera movement, light, window rain and a fade into silence. Raphael's seal assembles in segments and catches an outward resonance pulse, then native slime pixels gather into a body and abilities activate. The human-form hint and close-camera outpost reveal remain; the camera returns to the normal transform before control resumes.

The fifteen opening lines use the selected Japanese Rainoray and Raphael casts with English captions. Nominal duration is now 67.1 seconds. No playback speeding or pitch shifting was applied. Raphael retains the stronger echo treatment. Later dialogue used the English bank at this checkpoint; the separate 2026-10-28 Japanese-bank pass replaced it (see [voice-bank audit](japanese-voice-bank-2026-10-28.md)). Three Japanese opening clips and two English van references were regenerated/transcribed after the office-origin change; both chapters now agree with the new origin.

## Final verification — 2026-10-28

- Full Java 17 build passed all 126 test programs and the packaged-assets check. After the final audio-only chime refinement, repeated opening, seal, sound-channel and packaged-resource tests passed against the refreshed JAR.
- Thirteen original PCM16/22050Hz sound files now cover office typing/fan, weakening heartbeats, desk collapse, Raphael ignition, liquid reconstruction, four distinct skill chimes, human-form glass and waking water. Two files are authored shot mixes; the memory mix ends with 1.8 seconds of digital silence.
- Both stdlib generators pass deterministic `--check` and duration/edge/peak assertions. The dedicated opening channel obeys SFX volume/mute, pauses without losing position, resumes without retriggering, and clears on skip/exit. Generic opening cues and the old heartbeat loop were replaced.
- Claude Opus 5.5 at maximum effort contributed the reviewed outward resonance-wave refinement and layered glass chime function. The parent authored the base renderer, other sounds, arrangement and integration. The first VFX job exceeded the CLI's default output-token cap; the first large audio job was stopped without returning code. Focused max-effort reviews succeeded with a process-local larger output allowance.
- The actual renderer/mixer capture is 67.6 seconds, 1280×720/30fps, H264 plus AAC mono audio. Final office and Raphael frames were inspected. Final mixed PCM peak is -0.88dBFS with zero saturated samples. The in-memory fullscreen preview launches from the office and provides Replay opening without touching the real save.

The office atlas is `assets/ui/opening/office-night-atlas.png`; generated original and exact prompt are retained in `assets/source/ui/opening/office-night.generated.png` and `assets/source/ui/opening/office-night-prompt.txt`. Sound generation: `python3 tools/audio/generate_opening_sfx.py` followed by `python3 tools/audio/assemble_opening_sfx.py` (both support `--check`).

Scripts: [Japanese/English pairs](../design/opening-japanese.tsv). Generation metadata, raw MP3s and transcriptions: `assets/source/audio/opening-ja-2026-10-27/` and the later office revisions in `assets/source/audio/office-origin-2026-10-27/`. The runtime voice manifest records the new hashes, durations, models/settings and per-line scripts.

## Verification before the additional seal VFX pass

- Java17 production and all test sources compile with `-Xlint:all`.
- All 124 test programs passed across the initial full build and resumed checks. The initial run found a pre-existing HUD assertion checking the Blade timer at its old top-centre location. It now verifies the actual panel below the vitals. Production HUD code was not changed.
- The final packaged-assets check passed outside the repository, and `build/B2BJ.jar` was replaced only after passing checks.
- Opening tests cover full/skip save equivalence, Continue behavior, automatic completion, pause/input, voice duration padding, normal/reduced animation, 1280/1920/ultrawide layouts, click-to-skip, music entry and voice cancellation. Added camera checks verify the initial subject position, paused transform, no zoom/pan under Reduced Effects and exact identity before gameplay.
- The shared seal now respects incoming opacity. A regression check covers zero/partial/full opacity and preserving the caller's graphics state.
- All fifteen WAVs are mono PCM16/22050Hz, match manifest SHA256 and have non-clipping peaks from 0.5477 to 0.6408. Every slot exceeds its recording by at least 0.35 seconds. All Japanese lines were transcribed; two unclear short first takes were replaced and checked again.
- Rendered memory, pixel assembly, full slime, human hint and final camera frames were inspected. Fixed the small waking seal being clipped by the top cinema border.
- A 1280x720/30fps MP4 was captured from the actual renderer with the game dialogue/music/effects mixer. It has both H264 video and AAC audio streams. A live `tools/PreviewOpening.java` window uses an in-memory campaign and a Replay opening button; it never reads/writes the normal player save.

## Limits

The office action uses four generated keyframes with transitions and procedural light/rain/camera motion; it is not a fully hand-animated film. The human sprite is the existing game asset. Native-speaker acting/pronunciation review and a continuous two-chapter playthrough were not performed. Voice resemblance remains a listening judgment. No new runtime dependencies or GitHub push.

Staging and evidence: `/home/ghiankylledomingo/tmp/b2bj-opening-polish-2026-10-27/`. Review video: `/home/ghiankylledomingo/Downloads/B2BJ-New-Opening/Opening-preview.mp4`.
