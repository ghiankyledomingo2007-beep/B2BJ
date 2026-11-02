# Rainoray skill animation draft — 2026-11-02

Preview: http://127.0.0.1:4517/artifact/82498e3c315f/?sound=void2

Three staged concepts: Void Sever replaces Crescent; parry opens a charged punish; Beelzebub stores empowerment for the next heavy hit. The full kit is now implemented in the playable game; these videos remain the original staged audition. Current controls/economy: docs/design/rainoray-kit-draft-2026-11-02.md.

## Animation

The isolated Java17 tool tools/PreviewRainorayKit.java uses current game art and the installed renderer. Ultimate movement/hits, extra empowered damage and the longer parry opening are staged. Six 900×500, 60fps clips: ultimate 261 frames, parry 216, absorption 287, each with a reduced-effects version.

Five contact beats have two silhouette frames and a 33–100ms pose hold while sound continues. Reduced mode omits added silhouettes, camera shake/dimming, ghosts and extra sparks while retaining contact timing. Existing animation/damage/self-check evidence is at /home/ghiankylledomingo/tmp/b2bj-kit-impact-2026-11-02/. This sound revision preserves all six video bitstreams.

## Current sound: void magic

The owner rejected the previous metallic/explosive sound pass after hearing it. Actual Claude Code Opus 5.5 replaced those physical layers with detuned overtone clouds, ring-pitched breath formants, reversed swells, short pressure drops, resonant tears and diffuse tails. Parry is a magical seal catch/release. No new recordings, music or voice synthesis.

Generator: tools/audio/generate_rainoray_skill_sfx.py. Fifteen cue WAVs, three mixes, a manifest and SOUND_DESIGN.md live at assets/source/audio/rainoray-skill-draft-2026-11-02/. Existing helpers are reused; no new dependency. Run --check against that directory to reproduce and verify every file. --selftest uses isolated scratch files and rejects altered/truncated/missing/stray audio and edited manifests (six cases pass).

Exact mono PCM16/22050Hz sample counts: 95917/79380/105472. Determinism, finite samples, clipping/DC, faded edges, contact timing/contrast and quiet tails pass. Source mixes peak at −3dBFS. Encoded at 0.9 gain: ultimate −23.95 LUFS / −3.97dBTP; parry −24.54 / −3.85; absorption −25.19 / −3.93. Normal/reduced variants share audio. Loudness measurements are not a substitute for listening.

Evidence, source/previous-bank backups, Claude log, and mux checks: /home/ghiankylledomingo/tmp/b2bj-void-sound-2026-11-02/. Browser requests ?v=void2 media to avoid the previous cached audio. Use Hear sound to unmute. A prior silent audition was traced to the user's system-muted Firefox stream; that was unmuted after matching the media title/URL. Chrome DOM playback alone does not prove desktop audibility.

## Limits

No installed skill, runtime bank, JAR or save changes. No full gameplay build/playtest, forced restart, commit or push. The installed JAR remains 1ea2ee831b6a6b2eed94ce07fab56bfc580addbc833937c54a0e2dd9d79d8a26. Collision, hit limits, resources, bosses and combat audio overlap need testing during runtime integration. Physical listening quality remains for the owner to judge.
