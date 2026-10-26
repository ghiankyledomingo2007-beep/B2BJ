# English voice audition — 2026-10-25

## Complete Chapter One voice coverage

All 170 authored Chapter One dialogue/bark clips are now installed, including all alternate story branches. This final batch adds 93 files: the 86 remaining story/exploration lines and seven Warden fight reactions. Main conversations remain player-paced; normal banter stays suppressed during combat, while four authored Warden moments play short reactions without pausing the fight.

Rainoray/Sage retain their approved voices. NPC casting uses existing ElevenLabs premade voices: Sarah for Mara, Roger for Tobin, Lily for Ilse, George for Venn and Brian for the Warden. NPCs use stability .50, similarity .75, style .15 and speaker boost; Warden stability .65/style0. Warden local processing lowers pitch to .90 with preserved formants and adds quiet37/83ms echoes. No account settings or voices were modified. The batch uses5,448 script characters.

[Chapter One manifest](chapter-one-recordings.json) records all93 new scripts, voice IDs, per-request settings, durations, peaks and hashes. Raw MP3s, intermediate masters and the resumable generator stay outside the project at `~/tmp/b2bj-voices/chapter-one-2026-10-25/`. Runtime files are PCM16 mono22,050Hz. The final mono master below supersedes the original batch levels. Automated transcript results are in [the transcript report](chapter-one-transcript-check.json); listening remains the final judge of performance.

The earlier audition and partial-integration notes below remain as dated history.

## Natural Sage combat dialogue — 2026-10-25

Replaced the bare attack-name announcements with two natural, actionable warnings per learned attack, alternating per pattern. For example: “Back up. Here comes the wave.” / “Get some distance. Another wave.” The defensive instruction comes first; longer explanatory words may continue after the tell. Opening: “Keep moving. I'll analyze its attacks.” After observing a pattern twice, Sage acknowledges “I've got its rhythm. I'll warn you.” during a recovery window before learned warnings become available. Existing saves retain learned patterns and receive the new recognition beat once.

The Warden's half-health enrage gets “Careful. It's getting desperate now.” At25% HP or below, a recovery window can trigger “It's nearly down. Don't get careless.” Health milestones and analysis acknowledgement are once per campaign. The15-second shared boss cooldown remains. Real accepted damage can trigger one of two hit reactions, with a separate25-second cooldown and2.5-second expiry; blocked damage, old hits and speech already in progress do not cause interruptions. All combat lines retain3.6-second slots; all new recordings finish within2.85seconds, leaving reading time without longer conversational pauses.

All16 affected Sage clips were generated with the same approved voice/model/settings and effects. Seven replace old recordings; nine are new, giving **170 runtime clips**. [Generation metadata](natural-sage-recordings.json), [transcript/timing check](natural-sage-transcript.json), and [final runtime hashes](runtime-voices.json). The earlier bare-name timing report is historical. Raw recordings and preserved old runtime clips: `~/tmp/b2bj-voices/natural-sage-2026-10-25/`. No other character voices were regenerated.

## Consistency corrections and combat mix — 2026-10-25

All 161 runtime files were scanned for duration, decoding, clipping, pitch and harmonic/noise outliers. The same voice ID is used for each character; the original three discovery recordings retain their approved audition provenance in `polish-settings.json` and `english-audition-lines.json`. Automatic measurements identify suspect takes, not subjective voice likeness or acting quality. High-pitched question inflections are not automatically treated as defects.

Rainoray's reported buzzy opening was replaced. Twenty further Rainoray lines were regenerated using the same clone, lower similarity, no speaker boost and clean previous-request context; two needed further alternatives. Per-take settings and request IDs are retained in `voice-corrections.json`, `consistency-retakes.json`, `consistency-final-retakes.json` and `consistency-clean-retakes.json`. The newest record for a file supersedes older generation takes. [The final retake transcript check](consistency-transcript-check.json) covers all25 replacement/warning files; its only spelling difference is the invented name Rainoray. “One name off the missing list. Keep looking.” is now voiced by Sage.

**Final playback source of truth:** [runtime-voices.json](runtime-voices.json) lists all 170 shipped WAV hashes, durations and peaks after mastering at **-18 LUFS / -4 dBTP in mono**, avoiding the level drop from mastering stereo before mono conversion. Earlier generation-manifest hashes describe intermediate masters and are historical. Clean Rainoray has no added robot effect. Sage retains the approved chime, quiet detuning and echo. Speech remains independent of the SFX slider; active audible speech lowers effects by half and music to 35%, restoring both after the line ends. Original takes and intermediate audio remain outside the game.

Five short Sage attack names join the four Warden story reactions. Sage observes two nearby releases of each attack before naming it at a subsequent telegraph; knowledge persists with existing campaign unlocks. A shared 15-second cooldown prevents constant warnings. Tactical recordings omit the usual 190 ms Sage lead-in. [Word-timing checks](sage-warning-timing.json) place the spoken name endings at0.58–0.83seconds, before their1.0–1.4second attack tells end. The small gold seal remains visible beneath the boss health bar during combat.

Ordinary dialogue uses the existing original “Through the Rain” score, with one-second entry/exit fades. The Warden introduction uses the boss theme. Pause, mute and music volume remain respected. No additional music was purchased or generated.

## Installed Rainoray/Sage conversations

The approved voices now cover the original three-line survivor discovery plus 30 new optional exchanges (60 new recordings). Contexts: 12 fights, four close calls, ten exploration and four camp reflections. Rainoray stays clean; Sage retains the synthesized chime, quiet detuned layers and filtered echo in [polish-settings.json](polish-settings.json). Generation used the existing authorized account through the API only; no dashboard, account settings or voice clones were changed.

[Recording manifest](banter-recordings.json) records the exact text, request IDs, settings, durations, peaks and SHA-256 hashes. The new scripts total 1,917 characters. Source MP3s and stereo masters are outside the repository in `~/tmp/b2bj-voices/banter-2026-10-25/`; only mono 22,050 Hz PCM16 playback files enter the game. All 60 recordings are shorter than the 3.6-second line interval. The [transcript check](banter-transcript-check.json) matches the scripts except for the natural contraction “want to” → “wanna.” Automated checks do not replace listening to voice delivery.

The [story notes](../../design/area-one-story.md#rainoray-and-sage-while-exploring) describe quiet-time rules, cooldowns and save persistence. The existing preview tool supports `--banter`, with Replay and Next conversation buttons, using an in-memory session. Main story and NPC recordings were completed in the subsequent batch above. Earlier sections below describe the audition stages before this integration.

[Play the Raphael / inner-voice audition](raphael-english.mp3).

This is AI-generated prototype dialogue, not a recording of an original performance. User states performer permission for the personal project and explicitly authorized the Inovision ElevenLabs account. Created through instant voice cloning; provider returned `requires_verification=false`. No public voice sharing, account-plan changes or voice deletions were performed.

Script: “Notice. A voice was detected south of the road. Someone is still alive. Recommendation: investigate before approaching the gate.”

Reference: [Great Sage Evolution to Ultimate Skill Raphael](https://www.youtube.com/watch?v=wsHj3UjRMH0), approximately 60 seconds. Scribe detected English and one speaker. Reference converted to mono 24 kHz; clone background-noise removal enabled. Generation used `eleven_multilingual_v2`, stability .70, similarity .75, style 0, speaker boost enabled. Reference audio and API credentials are not shipped in this repository.

Generated audio decoded successfully and its English transcription matches the intended words. Human listening is still needed for resemblance, artifacts and acting quality; no subjective audition is claimed. This audition is longer than the game's six-second exploration subtitle window and is not installed as a runtime line. No companion scene has been added yet.

## Resumed English auditions — 2026-10-25

- [Rainoray and inner-voice exchange](rainoray-inner-voice-exchange.mp3), approximately 7.2 seconds. Three original B2BJ lines: Rainoray hears a voice south of the road, the inner voice confirms a survivor, and Rainoray decides to help.
- [Rainoray solo](rainoray-english.mp3), approximately 3.2 seconds, using the existing `wake_03` script: “No hands. No boots. I seem to have kept the exhaustion.”
- The original [Raphael solo](raphael-english.mp3) remains available above.

The English Rimuru source was successfully downloaded from [Rimuru Vs Ogres — English Dubbed](https://www.youtube.com/watch?v=RwmYfVztdGk) using an isolated current yt-dlp version, 2026.10.23.233055. The older July version returned HTTP403; no login, cookies, private-source access or account settings were changed. English speech was confirmed by Scribe. Selected 17 Rimuru dialogue intervals totaling39.09 seconds including short gaps; other characters and long action/music gaps were excluded. Initial full-clip diarization attributed these to speaker_2; the concatenated sample splits the “true identity” phrase as a second diarization label despite it being the same character's line. A second downloaded scene (`aZFD3AcHK_E`) contained other characters discussing Rimuru and was excluded.

Instant clone creation used background-noise removal and returned `requires_verification=false`. Clone source is shorter and noisier than ideal studio reference, so this is an audition-quality prototype. Original sources and selected reference WAVs are kept outside the repository in `~/tmp/b2bj-voices/` and `~/Downloads/B2BJ Voice Samples/`. They are not packaged with the game.

Rimuru generation: `eleven_multilingual_v2`, stability .45, similarity .80, style .10, speaker boost enabled. Raphael settings remain .70/.75/0. Four generated takes; per-take scripts/settings are recorded in `english-audition-lines.json`. Review audio normalized to -19 LUFS / -3 dBTP and assembled with 350ms gaps; no music or added effects. Decoded WAV duration/peak checks passed. Scribe matched the generated English words (punctuation differs) and detected two alternating speakers in the exchange. No subjective listening or likeness assessment is claimed.

Both clones now exist in the approved Inovision account. No runtime dialogue clips or new companion scenes were installed in this audition pass. No public voice sharing, plan changes or voice deletions.

Chapter One dialogue playback is already implemented; see [recording instructions](../../../assets/audio/dialogue/README.md). Finish auditions before generating the full script.

## Voice polish comparison — 2026-10-25

User liked both voices, described Rainoray as sounding retro, and requested effects for the inner voice. [Open the VLC comparison playlist](voice-polish-comparison.m3u): Rainoray original then revised take; inner voice original then effects; revised three-line exchange. The small HTML player follows the same order. Original MP3s are preserved.

Rainoray uses the same approved clone with stability .50, similarity .68, style 0, and speaker boost off. Three short lines were regenerated through the API at 192 kbps, then mastered to stereo PCM WAV. There was no intentional retro/bitcrush filter in the original audition. Source artifacts are a possible cause, not a verified diagnosis: ElevenLabs notes that high similarity can reproduce imperfections from reference audio. Lowering similarity is an audition tradeoff and may change likeness. See the [voice-setting guidance](https://elevenlabs.io/docs/eleven-creative/playground/text-to-speech) and [output-format documentation](https://elevenlabs.io/docs/api-reference/text-to-speech/convert).

The existing inner-voice performance is retained. Local processing adds a short synthesized bell at the start, two quiet detuned layers (pitch ratios 1.008/.992), and a short filtered stereo echo. The central dry voice remains strongest. This is an original effects treatment, not extracted anime sound effects. Settings and the complete FFmpeg filter graph are recorded in [polish-settings.json](polish-settings.json).

Files use 44.1 kHz stereo PCM16 WAV, mastered toward -19 LUFS with a -3 dBTP ceiling. Decoding, duration, non-silence, peak headroom and stereo-layer checks passed. Effects solo measured -18.9 LUFS and -4.0 dB true peak. Subjective improvement and effect strength need user listening; no in-game mix or subtitle timing test has been done. No runtime dialogue, save data, account configuration, or game code changed.
