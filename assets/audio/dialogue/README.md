# Recorded dialogue

Current runtime: 331 dialogue recordings (211 Outpost, 120 Forest) plus 18 Rainoray combat efforts. Public cast: Rainoray, Raphael, Shuna, Kaijin, Eren, Rigurd, Souei and the Outpost Warden. Legacy internal speaker IDs stay stable for saves and filenames.

Export the authoritative complete script:

```sh
java -cp build/B2BJ.jar tools/audio/ExportDialogue.java
```

Each TSV row names the WAV, internal speaker, scene and exact subtitle. Final cast settings, source request metadata, hashes and levels are in `docs/art-review/voice-auditions/runtime-voices.json`. Use mono PCM16 at 22,050 Hz. Dialogue targets -18 LUFS / -4 dBTP; short combat efforts are separately peak limited. Source recordings and API credentials do not ship in the JAR.

Rainoray and Raphael retain their approved voice profiles. Kaijin uses an older, seasoned Bill performance; Eren uses youthful Jessica; Souei uses reserved Eric; the Warden uses gravelly Callum with a faint armor resonance and short room echo, without pitch distortion. Shuna and Rigurd retain their existing cast. Raphael alone has the approved quiet chime/echo treatment. See `../CREDITS.md` for provenance.

NPC speech advances with player input and pauses with the game. Exploration exchanges follow each full recording plus a short reply gap. Ordinary combat does not interrupt an ongoing exchange; choosing NPC dialogue, skipping, leaving or death still ends speech. New casual exchanges wait for safe exploration or a settled fight. Fresh lines have priority, with a three-minute per-pair repeat gap during the session. Saved heard flags no longer permanently exhaust the companion bank. Completed exchanges are saved; interrupted exchanges are not marked completed.

The bank has 64 casual pairs across both chapters. Rainoray has six light attack, four heavy attack and eight hurt takes; Raphael has eight hurt responses. Accepted hit reactions wait briefly for Rainoray's effort, respect their own cooldown, and never overlap current dialogue. Warden attack lines follow actual attack releases and leave Raphael's initial analysis first. Boss milestone exchanges wait for the current speech to finish.

Playback is offline. Volume/mute and reduced-effects settings remain respected. Automated transcript, PCM, cast and gameplay timing checks cannot certify acting quality; human listening remains the final performance check.
