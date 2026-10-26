# Guardian armor and room treatment — 2026-10-26

Six Warden recordings retain the existing Callum performance and source MP3s. `tools/audio/warden_voice_fx.py OUTPUT_DIRECTORY` renders the current treatment; `--check` verifies the shipped WAVs. It reuses the Forest generator's standard-library filters and installed FFmpeg for loudness matching. No new TTS or voice pitch processing.

Direct speech stays dominant. A small 145 Hz body band and quiet 530/920/1630 Hz armor modes sit underneath. Filtered stone-room reflections at 32, 61, 107, 157 and 223 ms decay quickly. The final low-health confession uses roughly half the armor emphasis. Full tails are retained; runtime clip-driven timing already accounts for them. Final output is mono PCM16, 22,050 Hz, target -18 LUFS / -4 dBTP.

All six final recordings passed transcription, with only punctuation/lifeform spacing differences. Processing validates finite samples, effect energy below 35% of direct-voice RMS, peaks below 0.65 full scale, preserved speech length and a quiet tail. Metadata and shipped hashes are in `processing.json`; original generation provenance remains under `assets/source/audio/companion-cast-2026-10-26/`.

A local dry/armored comparison was opened for the user. Human listening is still needed to judge the final sound character. No actor re-performance was synthesized in this pass.

Validation: deterministic six-file re-render passed; ChapterOneVoicesTest, CompanionCadenceTest, GameAudioTest and CombatVoiceTest passed against the staged JAR. All 349 voice/effort hashes match the manifest. Only six Warden WAVs and two audio documents changed in the JAR; every class and other asset stayed byte-identical. Installed build SHA-256: `fecc2687ba4a2f9e9394d6dc8da3098deae666abde491905f8e93155890a5aa4`. Full campaign playthrough was not repeated.
