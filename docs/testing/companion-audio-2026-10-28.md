# Companion replies and Beelzebub audio — 2026-10-28

Rainoray now replies after Raphael's seven first-enemy identifications and both boss-pattern discoveries. Each identification uses a different response, with Japanese speech and English captions. Full recording lengths control the turn-taking; the next line begins after the previous line and its tail finish. Mixed enemy groups leave four seconds after a completed exchange before another identification. Earlier saves can hear each expanded identification once; completion is saved after Rainoray finishes, leaving interrupted exchanges available for a later encounter.

Successful human-form Beelzebub casts rotate three short Japanese calls. They only start while the speech channel is free. Invalid, empty-target and cooldown requests stay silent, and a cast during another conversation does not interrupt it or queue an out-of-context shout. The calls announce casting, not guaranteed successful absorption; cancelled casts therefore do not make false success claims.

The original procedural Beelzebub sound now layers an opening tear/thump, accelerating air, low hollow resonance, granular crackle and a closing impact. It retains the existing stoppable channel, 1.15-second length, volume controls and cancellation behavior. No anime recording or new dependency was imported. Raphael's approved echo/chime recordings are unchanged.

## Recordings and handoff

- Added 12 final Japanese Rainoray clips using the selected B profile, existing model/settings/seed, and natural speech speed. All 349 previous runtime WAVs remain byte-identical.
- Final bank: 343 dialogue clips + 18 combat efforts = 361 WAVs, approximately 21.7 minutes. Chapter One inventory219; Chapter Two124; combat efforts18. Beelzebub calls also work in the forest.
- Generated13 takes including one clarification retake: the first remnant reply had ambiguous ASR wording for 間合い. The installed simpler equivalent is なら、少し離れよう。, correctly transcribed. All final additions have Japanese transcripts consistent with their scripts (normal spelling/punctuation differences retained).
- Raw MP3s, requests and transcripts: `assets/source/audio/companion-jp-2026-10-28/`. Installed metadata: `docs/art-review/voice-auditions/runtime-voices.json`.
- Updated `docs/design/japanese-voice-lines.tsv`, both English chapter TSVs, `trailer-voice-handoff.md`, and `claude-trailer-voice-prompt.md`. Every343 runtime dialogue text/speaker matches `tools/audio/ExportDialogue.java`.

## Verification

All production and test sources compiled with Java17/-Xlint:all. Eight focused test programs passed: MobIdentificationTest, BeelzebubTest, CompanionCadenceTest, ChapterOneVoicesTest, ChapterTwoVoicesTest, SageCombatTest, ForestChapterTest and PackagedAssetLoadingTest. The latter two ran against the candidate JAR from outside the repository. Regression coverage includes old-save migration, completion persistence, full two-speaker playback through the mixer, spacing, skill-call rotation/denial and conversation priority.

Offline Japanese cast/language/format/hash/duration audit passed for361 files. All361 candidate-JAR WAV hashes match the manifest; the packaged Beelzebub effect matches its deterministic source and passes duration/peak/edge checks. Diff whitespace check passed.

Actual game-renderer/mixer preview (12seconds,1280x720/60fps,H264/AAC): `/home/ghiankylledomingo/Downloads/B2BJ-Companion-Audio-Preview/Companion-audio-preview.mp4`. Captured Raphael guard identification, Rainoray's answer, then voiced Beelzebub. PCM peak20980/32768; zero saturated samples. Reviewed reply/cast frames. Temporary in-memory session; no player save accessed.

Installed `build/B2BJ.jar` and reopened the regular game fullscreen. Startup stayed running without errors; real saved-campaign bytes unchanged at startup. Previous JAR, save backup, generation/retake logs, capture and owned game PID/log: `/home/ghiankylledomingo/tmp/b2bj-companion-audio-2026-10-28/`.

## Limits

The complete126-program suite was not rerun for this scoped dialogue/audio update. No full manual chapter playthrough, native-speaker acting review or hardware listening review. Subjective performance and mix still need player listening. No commit or push. Deferred adult-office voice transition and trailer production remain unchanged.
