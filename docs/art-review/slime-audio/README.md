# Rainoray slime audio — 2026-10-25

Historical review: on 2026-10-26 the attack, splash, dash and hurt families were replaced by original coded sounds from `tools/audio/generate_combat_sfx.py`. Movement remains as described here. Use the new generator for current combat takes; the remaster command below rebuilds the older versions.

Locally remastered the existing licensed recordings; no new service calls. Original takes live in `assets/source/audio/slime-before-2026-10-25/`; rebuild with `python3 tools/audio/remaster_slime.py` (ffmpeg required). Existing credits are retained and updated.

- Four movement squishes: shorter (~0.13s), softer peak (.23 rather than .40), reduced low thump.
- Three basic attacks: tighter liquid snaps (~0.22s rather than ~0.42s).
- Two water impacts: shorter splashes (~0.24s).
- Two dash takes: dedicated wet rush, now actually routed to SLIME_DASH instead of reusing WATER_SPLASH.
- Two new slime hurt takes: membrane crumple with a quiet body knock; blade form keeps HURT.
- Basic attack, water hit and slime dash retain the synthesized magic layer at gain .32 instead of 1.8. Other spells, enemy audio, blade sounds and music unchanged.

`before.wav` and `after.wav` use actual GameAudio.voicePcm output, unit session volume, fixed pitch and the same schedule: four hops, three shots, one dash, one hurt. These are isolated comparisons, not live speaker recordings. The before sample uses the previous packaged game's cue routing; after uses the updated player cues.

Six focused tests passed: GameAudio, B2BJIntegration, CampaignOptions, SlimeCasting, SlimeCombo and TideFeedback. GameAudio now checks short varied slime takes, headroom, quiet movement relative to attacks, plus its existing complete cue/format/fade/mixer/volume/mute checks. Repeated remaster produces identical bytes for all 13 outputs. All sources/tests compile with Java 17 -Xlint:all. Final JAR passed GameAudioTest from /tmp; git diff --check clean.

Full suite not repeated after the previous 99-test pass. No subjective speaker/headphone audition or full live combat mix assessment claimed; the assistant cannot hear the tool's audio output. User audition still needed. Changes remain local and unpushed.
