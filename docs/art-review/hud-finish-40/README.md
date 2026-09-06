# HUD finishing assets — September 7, 2026

The backup allowance was used only after the preceding 40 generations were
exhausted. Provider balance after completion: **6 used, 34 remaining of 40**.
Six jobs produced 30 source PNGs. No further jobs are pending. Job IDs and costs
are recorded in [jobs.json](jobs.json); credentials are not stored here.

## Reviewed selections

- `absorb-flow`: native 64×32 stream; frames 0, 2, 6, 8, 10, 12, 14, 16.
  The renderer crops it to the remaining corpse/player gap before rotation.
- `camp-marker` and `heal-marker`: native 32×32 tent and medical satchel for the
  expanded map. No text is embedded in the generated images.
- `heal-motes`: rejected because it included a solid pool/base.
- `heal-sparkles`: accepted as the sparse mint/ivory source for `heal-rise`.
- `heal-rise`: native 48×48 healing motes; frames 0–6 and 8. Frame 7 is blank
  and excluded. The effect fires only when health actually increases, not when
  an energy-only corpse is consumed.

[Packing script](../../../tools/pack-hud.sh) reproduces the runtime sheets and
icons from these reviewed sources without resampling. Rejected source frames
remain here for provenance but are not packaged into the game.

Combined with the [first batch](../hud-40/README.md): **46 generations used**,
27 completed jobs, 161 source PNGs. Remaining allowance is not a requirement to
generate unused assets. Both temporary credential-bearing sessions were closed.

See the [implementation and testing report](../../testing/hud-40/README.md).
