# Guardian sound pass — 2026-11-02

Original procedural bank written by actual local Claude Code, `claude-opus-5-5`, max effort. Sixteen 22050 Hz mono PCM16 takes and a 20.9-second grouped audition. No new music, voice generation or sampled anime audio.

Phase I uses inharmonic armor plates, a resonant hollow cavity, harmonics carrying body weight above sub-bass, and a restrained cyan core chord. Backhand shears across that cavity; rush displaces pressure. Phase II detaches two armor pieces, lands two tuned fist contacts 0.48 seconds apart, and releases a dense harmonic core beam. The beam sustains through 0.9 seconds and dissipates over 0.25 seconds. Phase strain blooms at 1.2 seconds. Quiet footfalls and the recovery chime remain below attacks.

The generator uses deterministic seeds, modal/comb resonance, additive partials, filtered noise, reversed reverb swells, controlled chorus, tapered edges and DC correction. Distinct fingerprints and laptop-band energy checks are engineering proxies, not a subjective listening assessment. Every final cue peaks below -1 dBFS. See output/manifest.json for the layers, timing, gain, seed, waveform metrics and SHA256 of each take.

Claude's CLI could write the generator but its shell render request required approval. The parent agent stopped that run, rendered locally, and corrected three failed checks: windup fades by .735 seconds, beam charge fades by 1.14 seconds, and core-open loudness target is -20.5 dB. The tests were preserved. All 203 checks then passed. The untouched Claude generator/logs remain in the task's temporary directory, outside Git.

## Runtime integration

The separate preview embeds only the final 16 cues. Old alternate windup/strain samples are removed from that preview so random take selection cannot play the previous bank. Pitch stays fixed; sample variants supply variation. Cue-family gains come from the manifest, multiplied by the existing game master sample gain of .8. Player sounds, voices and music keep their existing paths.

Preparation/release/recovery cues trigger from real Guardian state changes. Fist impacts occur .20 and .68 seconds into their active state, each with matching VFX and camera shake. Phase bloom/shake occurs 1.2 seconds into the safe transition. Beam starts on release and retains its authored tail; cover truncates the beam visual and damage. No broad fullscreen flashes.

## Reproduce

```sh
rtk proxy python3 tools/guardian-preview/sound/generate_guardian_sfx.py
rtk proxy python3 tools/guardian-preview/sound/generate_guardian_sfx.py --check
rtk proxy python3 tools/guardian-preview/sound/generate_guardian_sfx.py --selftest
```

--check re-renders in memory and compares every output/manifest. --selftest mutates only a fresh temporary directory to prove altered, truncated, missing, stray, edited and clipped output is rejected. No physical listening claim is made; use native.html for the audition and the separate game window for the mixed fight.
