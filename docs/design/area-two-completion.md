> Story/cast revision, 2026-10-26: Sedge is now publicly Souei, Sage is Raphael, and the ending resolves Rainoray's modern-world rescue thread. Chapter Two now contains 104 recordings. See [current story](area-one-story.md). The implementation record below predates that rewrite.

# Area Two completion — verified playtest build, 2026-10-26

Complete the Corrupted Forest as a distinct, voiced chapter. Preserve Chapter One, existing saves, groupmate work and current approved character identities. The goal is not complete until all sections below have implementation and direct verification evidence.

## Required outcomes

- Forest enemies: distinct attack behavior, telegraphs and reactions, not Chapter One roles with a palette swap. Rift Imp: sidestep into a planted double-claw combo; Thorn Wolf: grounded approach/bites, with a committed pounce available only every third attack; Mire Shaman: interruptible ground-hazard casting instead of the spitter fan. Mix encounters by role and terrain, rather than repeating the same four-enemy formation.
- Briarheart: a separate forest boss loop built around rooted attacks, changing safe spaces and counterplay, with its own phase escalation, effects, cues and Sage learning. No recycled Warden slam/wave loop.
- Pixel art: inspect actual forest renders and atlases; replace inconsistent character/boss/prop assets at deliberate native resolution, controlled palette, clean silhouettes and binary alpha. Check pixel density against Chapter One and review complete scenes.
- Sage: always visible during live campaign play, including silence, NPC conversations and combat. Quiet idle animation; speech-energy pulse only for Sage. Keep the compact top placement, clear HUD/boss bars, pause and Reduced Effects behavior.
- Story: deliver the stretcher-track thread from Chapter One through Sedge/Ferry Refuge, the forest discoveries, Briarheart and a chapter conclusion. Write natural exchanges and optional context-aware banter. Do not invent that all four areas are complete.
- Voices: use the approved ElevenLabs account via API; preserve identities. Investigate Rainoray variation and reference quality, choose a consistent recording profile/context, correct affected existing performances and record the complete forest script. Verify scripts, speakers, clip timing, loudness, artefacts and in-game mix. Same voice ID alone is not proof of performance consistency.
- Combat voice feedback: varied short Rainoray efforts/reactions mixed with existing attack sounds, paced so they cannot overlap story/Sage speech or play on rejected inputs. Include form/context rules and user volume settings.
- Completion evidence: appropriate regression tests, final packaged asset/voice checks, rendered scene review and a staged whole-chapter progression/retry/save audit. Record remaining subjective limitations honestly; do not use a narrow green test as proof of a broad art/audio requirement.

## Starting evidence and scope

Area count is two. The forest has12 landmarks and27 enemies, but no full story dialogue. Current Imp/Thorn Wolf/Shaman map directly to SCOUT/GUARD/SPITTER in Wisp. Briarheart alternates TARGET/SHOCKWAVE in Guardian. Forest props include recent groupmate imports; do not discard those wholesale. Sage currently draws only while its bark is active. Chapter One has170 recorded lines with matching scripts/hashes, but historical Rainoray batches used multiple settings and retakes. The source clone is an audition from roughly39seconds of extracted speech; reference noise and TTS delivery variation need investigation.

Touched systems will include campaign simulation and persistence, enemy/guardian state and collision helpers, HUD/telegraphs/effects/rendering, dialogue/bark selection and exports, audio mixer/effort playback, art sources/importers/runtime sheets, voice assets/manifests and focused tests. Real saves and the open game session remain protected. No commit/push was requested.

## Progress

- Player-feedback correction: removed the Imp's forward rush and added two visible claw strokes with a safe timing gap; Wolves no longer leap on every distant approach. Forest melee warnings now fill their actual shared damage footprints; landing/bloom rings and root lanes have dark rims, clear boundaries and advancing timing marks.13 focused test programs passed, including102 geometry cases; actual forest warning screens reviewed. No new art/audio generation or save format changes in this correction.

- Implemented all three distinct forest enemy roles, shared damage/telegraph geometry and20 enemies in varied encounters. Corrected three authored spawn collisions with existing props; map flood/clearance test passes.
- Briarheart now roots in place, cycles committed root lanes, seed blooms and an escapable bramble cage, then exposes its heart for3.8seconds. At half HP the next sequence escalates after an enrage transition. No Warden minion waves or cone/wave patterns. Pattern, hazard, hit-geometry, exposure and phase tests pass.
- Generated and imported new tree-boss, three enemy and Sedge keyposes. Native96/48/80px cells, fixed palettes, binary alpha. Reviewed staged scenes; toned down terrain corruption and used organic root veins inside accurate hazard bounds. Groupmate oak/bush/log/stump art retained. These are limited pose animations, not fully hand-animated sets.
- Sage stays at the top in silence, combat and actual NPC conversation screens, with voice-energy pulse only for Sage. Boss offset, pause and Reduced Effects tested.
- Forest story includes arrival, Sedge, three optional discoveries, boss introduction, post-victory dialogue and survivor ending.18 optional exchanges plus location/boss learning/health/warning beats. Normal route, checkpoint death/retry, saved upgrades/knowledge/discoveries and ending verified with temporary saves.
- Installed100 forest dialogue clips, regenerated all126 Rainoray lines with one fixed voice/model/profile and fresh request context, preserved existing Sage/NPC Chapter One takes.270 dialogue clips plus seven combat efforts. All182 changed dialogue clips transcribed; only one invented-name spelling differs. Script, cast/profile, PCM, peaks, timing and SHA-256 checked. Raw takes/metadata preserved; no runtime network use.
- Combat efforts play on accepted blade attack/damage events, rate-limited and subordinate to dialogue; mute, voice/SFX volume and pause tested.
- All112 test programs passed; final packaged voice hashes and270 exported scripts match. Corrected stale fixtures for the new forest sequence/ending, then completed the remaining checks with the same compiled build. Final visible-screen check also restored Sedge's E Talk prompt; four relevant regressions passed after that small rendering correction. Final JAR is `build/B2BJ.jar`. See `docs/testing/area-two-completion-2026-10-26.md`. Human balance and subjective voice delivery remain playtest checks, not claims established by automation.

### Enemy first encounters and spell visuals
Sage names nearby, visible unknown enemy types once per save, with an eight-second gap and one short tactical hint. Existing dialogue and urgent reactions keep priority. Native-pixel claws, jaws, caster charge, release trails and live thorn patches distinguish the forest attacks; ground warnings remain subdued.
