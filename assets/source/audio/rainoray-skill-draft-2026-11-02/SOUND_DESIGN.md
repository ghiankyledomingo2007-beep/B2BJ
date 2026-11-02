# Rainoray void sound pass

## Sound direction
The old bank was physical: thumps, stone/metal cracks, pebbles, debris, falling sub booms, metal clangs.
**All of those layers are deleted** (no `thump`, `crack`, `knock`, `churn`, `whoosh`, `glint`, `grains`,
`boom` or saturation left in the generator). Every cue now uses these synthesis building blocks:

| helper | sound |
|---|---|
| `cloud` | detuned partial pairs (chorus beating), phase-continuous FM against a sub-octave modulator (dark, unstable sidebands that settle), `squeeze` pulls the overtones in toward the fundamental: the chord **folds inward** |
| `breath` / `choir` | noise through moving vowel formants ("oh" → "oo"), partially ring-modulated so it takes a hollow pitch: a wordless dark chorus, no speech |
| `inhale` | a struck cloud + breath run through haze and **reversed**: air and overtones get sucked into the hit, then cut off sharply (the pressure drop, about 40 ms) |
| `tear` | the contact: a resonant noise band snapping down from about 5 kHz, ring-modulated so it's inharmonic and doesn't sound like any real object, with a 0.4 ms front |
| `haze` | 4 comb + 2 allpass diffuse tail, darkened: the lingering spectral afterglow |

Per cue:
- **void_charge / empower_charge(_2)**: a rising detuned void chord with FM getting more unstable, an inhaled ring-pitched whisper, and a reverse shimmer swell (in void_charge). It still drops to silence before release; take 2 keeps its quickening throb.
- **void_dash**: a ghostly breath slip into a small tear, with shimmer overtones folding inward.
- **void_rupture**: a reverse inhale, then the pressure drop, then the space-tear, then a 55 Hz FM void chord collapsing inward, a dark choir breath, a 440 Hz fifth-based ghost shimmer and a 1.4 s haze.
- **seam_fracture(_2,_3)**: small tears with hollow inward-folding rings. Take 3 adds a low chord and a breathing undertone.
- **parry_clash(_2)**: the seal draws energy in (reverse swell), then catches it as a glassy near-harmonic ring in haze, with no metal.
- **counter_cut(_2)**: the seal releases: a bright breath slip, a light tear and a rising shimmer.
- **heavy_cleave(_2)**: a ghost swing, a reverse inhale and the drop, then a space-tear, a hollow FM chord and choir breath. Take 2 is deeper and longer.
- **devour_cast**: a ring-pitched throat while the overtones collapse to one pitch, closing with a glassy blink.

Midrange for laptops comes from the formants (about 300–900 Hz) and the cloud partials, not from sub boom.

## Unchanged
Cue names, the 15 files and their variant roles, sync offsets, 22050 Hz mono PCM16, frame counts, timelines,
ducks, contacts and the `--out/--check/--selftest` CLI.

## Checks
- Removed: the "ultimate aftermath ≥ 1 dB louder" cross-track assertion (that was a taste guess, not a technical check). The manifest still reports `tail_energy_rms_dbfs`, labelled as energy only.
- Kept, and all pass: peak ≤ 0.78, no hard clipping, DC, faded edges, exact duration, deterministic bytes and manifest, silence at loop boundaries, `quiet_by` tails, major/minor contact level and pre-dip contrast, punish heavier than catch, devour quieter than the hit.
- Contact contrast comes from the reverse swells ending 40 ms before each hit; no test was weakened. Parry cue level went 0.76 → 0.78 (the ceiling) to clear the 0.45 major-contact peak with margin.
- `--out output`, `--check` and `--selftest` (6/6) all pass.

## Reproduction and delivery — 2026-11-02

Claude Code Opus 5.5 authored this revision. Run from the repository:

```sh
rtk proxy python3 tools/audio/generate_rainoray_skill_sfx.py --check --out assets/source/audio/rainoray-skill-draft-2026-11-02
rtk proxy python3 tools/audio/generate_rainoray_skill_sfx.py --selftest --out assets/source/audio/rainoray-skill-draft-2026-11-02
```

To regenerate, omit --check. Standard library only, reusing generate_forest_sfx.py. Selftest uses a fresh temporary directory and preserves the output bank.

All six preview clips keep the same video bitstreams. AAC uses 0.9 gain: ultimate −23.95 LUFS / −3.97 dBTP, parry −24.54 LUFS / −3.85 dBTP, absorption −25.19 LUFS / −3.93 dBTP. Normal/reduced variants share identical audio. These measurements establish level and clipping headroom, not subjective quality. The owner still needs to audition the result; no physical listening claim is made.

Preview only. No runtime sound bank, installed JAR or campaign save was changed. Evidence and prior bank backups: /home/ghiankylledomingo/tmp/b2bj-void-sound-2026-11-02/.
