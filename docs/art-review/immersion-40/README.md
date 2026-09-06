# Ruined Outpost immersion batch — 2026-09-06/07

Owner requested better hit/hurt effects, particle-style feedback, a distinct enemy,
animated magnetic Ichor, then explicitly requested all credits be used and a bigger
boss. Scope stays inside the Ruined Outpost.

**Final provider balance: 40 used / 40 total / 0 remaining; displayed credits $0.00.**
All **21 jobs completed**, producing **208 downloaded PNG frames**. No further
generation calls were made. The temporary credential-bearing process was closed;
the token was not saved to project files or configuration.

[Job IDs/costs](jobs.json), [initial requests](requests.json),
[effect animation requests](animation-requests.json), [Spitter casts](spitter-requests.json),
[last three requests](final-requests.json). Twenty generations paid for the
12-frame consistent enemy edit; the other twenty jobs cost one each. Generation
units are not dollars; no new subscription or credit purchase was made.

## Review decisions

Only selected frames are packaged. Nine runtime sheets were added: seven effects
and two Spitter sheets. The raw archive includes rejects; it is not a runtime asset
directory. UUID download links identify account assets; do not publish casually.

| Candidate | Decision |
| --- | --- |
| `spitter-edit` | Distinct bronze barrel helmet, violet slit/nozzle and plum jelly base. Only south0, west4 and back10 used as animation references. Other frames drifted in facing or grounding. |
| `spitter-cast-south`, `-west` | Selected anticipation/recoil/recovery poses; retain 48px cells and 2× rendering. West source faces left; mirror only for east. |
| `spitter-cast-north` | Exclude settling0–2 and side-nozzle7–13. Use clean rear views3–6,14–16. Back body remains more compact than front; not a final character-art sign-off. |
| `ichor`, `ichor-loop`, `ichor-absorb` | Gold droplet, moving glint and collapsing pickup burst accepted. Pickup is gold, not the old blue water splash. |
| `pickup` | Rejected: generated a solid coin despite the particle request. |
| `slime-hurt`, `slime-hurt-burst`, `slime-hurt-shear` | Two reviewed teal splash variations. Runtime fade removes residual ripple; neither is treated as an endlessly looping hit. |
| `blade-sparks`, `blade-hit-burst`, `armor-deflect` | Gold strike flashes/shards accepted selectively. Exclude main burst10–16, which reforms a ring after dissipation. Two rows plus horizontal mirroring provide variation. |
| `spit-orb`, `spit-orb-loop` | Violet glob with pulsing body; visually distinct from cyan player projectiles. |
| `spit-impact`, `spit-impact-burst` | Selected first splash and dissipation; skip6–9, which restart the burst. |
| `stone-impact` | Rejected: solid raised disc/crater, not particles. |
| `stone-chips`, `stone-scatter` | Sparse separated chips accepted. **Frame6 is fully opaque and excluded**; blank15–16 omitted from the packed sheet, which fades out in the renderer. |

## Exact integration

[`tools/pack-immersion.sh`](../../../tools/pack-immersion.sh) records every selected
frame and integer floor correction. Reuses `PackFrames`; no AI output is blindly
copied into a runtime directory. `fetch-immersion.mjs` downloads known completed
jobs with PNG/ID validation; it cannot generate or charge for anything.

- Effects: eight poses per row; native 2×. Slime hurt and armor sparks each have two
  rows; one row for Ichor idle/pickup, hostile orb/impact and stone scatter.
- Spitter: three facing rows, eight attack poses and four movement poses per row.
  Movement reuses selected jelly-body pulses, **not** a separate high-end walk cycle.
  South/west register at row 47; north at row 43 plus an 8px screen ground correction.
  No body resampling. Wrong-facing/nozzle artifacts remain excluded.
- Fixed an existing remnant mirror bug exposed by the new scene: `WispAnimation`
  row1 means east, row3 west. Both remnant sheets are canonical west, so only
  row1 mirrors. `RemnantFacingTest` compares actual rendered pixels both ways.
- Warden enlargement requested afterwards: 64px cells now render at integer3×
  (192px canvas, previously128). Ground anchor retained; fitted foot collision
  widened88→112px. Health, damage and attack ranges are unchanged. No generations
  spent merely enlarging already reviewed art.

`ImmersionRenderingTest` checks exact sheet grids, transparency, nonblank and
distinct selected poses, and correct effect selection for Blob/Blade hurt and
pickup. PixelLab-generated particle motion is sprite animation; this pass does
not claim a new per-fragment rigid-body physics engine.

See [gameplay/verification report](../../testing/immersion-40/README.md) and staged
[Spitter volley](../../testing/immersion-40/spitter-volley.png),
[Ichor pull](../../testing/immersion-40/ichor-pulling.png),
[Blob hurt](../../testing/immersion-40/slime-hurt.png),
[larger Warden](../../testing/immersion-40/warden-larger.png).
