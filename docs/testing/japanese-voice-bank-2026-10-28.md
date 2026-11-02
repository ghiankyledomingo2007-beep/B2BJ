# Japanese voice-bank audit — 2026-10-28

## Scope and installation

Owner chose Japanese voices with English menus/subtitles. All 349 character WAVs are now Japanese: 331 dialogue clips and 18 combat efforts, 21.3 minutes in total. This includes both chapters, story branches, exploration conversations, first-encounter identifications, boss reactions/warnings and hit/attack efforts. The 15 existing Japanese opening clips were preserved byte-for-byte. The adult-office-to-Rainoray layered handoff remains planning only and deferred.

Rainoray and Raphael keep the selected Japanese voice IDs/models/settings. Shuna, Eren, Kaijin, Rigurd, Souei and Warden use six fixed native Japanese library profiles. NPC casting is age/role matched; these are not the anime actors' clones. See [cast](../design/japanese-voice-cast.json) and [complete script](../design/japanese-voice-lines.tsv).

## Generation and audit

- 334 initial replacement takes, 40 retakes for shorter/clearer lines, 8 further refinements; 382 new generations in total. Final requests/raw MP3s/transcripts under `assets/source/audio/japanese-bank-2026-10-28/`.
- All 334 final replacement clips transcribed with Scribe. Reviewed flagged differences; Rainoray's Japanese name can be transcribed as a homophone, and nonverbal combat sounds get unreliable language labels. One exact Japanese phrase (“今の、見た？”) was labeled Chinese by automatic detection; the Japanese text matches the script. Exceptions retained in `transcript-review-exceptions.json`.
- Every installed script/speaker key matches the actual game's dialogue exporter. All 334 replacement recordings fit their current playback windows; no speed changes were used to force timing.
- Offline audit passed all 349 filenames, Japanese script fields, fixed voice IDs/models/settings, manifest hashes, PCM encoding, durations and non-clipping levels. Mono PCM16 / 22050 Hz; dialogue target -18 LUFS / -4 dBTP; efforts peak approximately -7.51 dBFS.
- Raphael retains the selected echo/chime treatment. Warden uses the existing unpitched direct voice plus restrained armor/room treatment. The Warden remaster tool now reproduces the Japanese sources.
- Full Java 17 build passed all 126 test programs plus the outside-repository packaged-assets check. Both chapter voice suites, dialogue gain/ducking/pause, companion cadence, combat efforts, Sage presence and opening tests passed. All 349 WAV hashes inside the final JAR match the installed Japanese manifest. Warden remaster `--check` reproduces all six current Japanese files. `git diff --check` passed.

## Handoff and recovery

[Claude trailer handoff](../design/trailer-voice-handoff.md) is the entry point for the separate trailer work. No trailer creation/publishing or Git push was performed. Existing real save and current running game process were not changed.

Previous 349 runtime WAVs and manifest are backed up at `/home/ghiankylledomingo/tmp/b2bj-japanese-bank-2026-10-28/previous-runtime/` and `runtime-voices-before.json`. Rejected takes and generation/build logs stay in that staging directory. Runtime replacements were installed only after the final-recording audit.

## Limits

ASR/script review is not a native-speaker performance/listening review. Voice resemblance and acting remain subjective. Japanese uses natural phrasing, including shorter tactical calls, rather than a word-for-word English reading. Current source/build audio loads on the next game launch; already-running processes can retain the previous bank.
