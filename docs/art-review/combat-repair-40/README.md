# Ruined Outpost: combat animation batch — 2026-09-06

Owner authorized this entire 40-generation allowance. Final PixelLab balance observed
after completion: **40 used / 40 total / 0 remaining**, displayed credits **$0.00**,
subscription `trial`. Generation stopped. These are generation units, not $40 spent.
All **18 jobs completed** and their frames were downloaded and visually reviewed.

[Job IDs and costs](jobs.json), [first animation requests](requests.json),
[remaining requests](remaining-requests.json). Early receipts in the ledger retain
their original `processing` submission status; completion was checked separately.
Download UUIDs identify private-account assets; do not publish this archive casually.
Credentials are not stored here.

Cost reconciliation: remnant edit 20; seven Warden animations 7; three padded
Blade animations 6; three Slime casts 3; three remnant attacks 3; impact candidate 1.
Rejected generations still consumed their stated allowance.

## Review and selection

Indices below refer to downloaded `frame-N.png`, including the reference frame at 0.
Raw candidates and contact sheets remain in each named subdirectory; only reviewed
selections are packaged. Fourteen jobs contribute to **nine runtime PNG sheets**.

| Candidate | Decision and selected frames |
| --- | --- |
| `remnant-edit` | Helmeted purple remnant, cyan eyes, no player star. Walk rows south/west/north: `0,1,2,3 / 4,5,6,7 / 8,9,10,9`. North frame 11 turns toward the camera: rejected. |
| `remnant-attack-south`, `-west`, `-north` | Accepted body-compression attack in each facing: `0,2,3,5,6,8,13,16`. |
| `guardian-walk` | Accepted grounded weight transfer, frames `1–8`. |
| `guardian-charge` | Accepted front-facing brace, `0–7`; later side-turn frames excluded. |
| `guardian-slam` | Not integrated: weaker arm raise than the existing readable slam tell. Retain existing art. |
| `guardian-sweep` | Accepted torso/arm sweep, `0,2,4,6,7,8,14,16`. |
| `guardian-shockwave` | Accepted braced pulse, `0,1,2,3,4,8,10,16`; damaging ring remains explicit runtime geometry. |
| `guardian-hurt` | Staged only. Do not interrupt committed boss attacks to insert a flinch. Existing damage flash retained. |
| `guardian-death` | Accepted weighty collapse, `0,2,4,7,9,10,11,12,13,14,15,16`; no shrinking the sprite. |
| `blade-side-cut` | Rejected: turns front-facing and changes weapon presentation. |
| `blade-side-return` | Selected controlled downward cut `0,1,3,5,10,14,15,16`; repetitive holds removed. Used for combo hits 1 and 3. |
| `blade-side-thrust` | Accepted forward thrust `0,7,8,9,10,11,12,16`, used for combo hit 2. |
| `slime-cast-south`, `-east`, `-north` | Accepted body-ripple casting: each `0,2,4,7,10,12,14,16`. No punching arm. South sparkle frame 8 excluded. The job named `east` actually supplies the canonical west-facing row; flip it for east at render time. |
| `warden-impact` | Rejected: solid circular dirt plate, not loose transparent stone chips. |

## Runtime contract

- Slime and remnants: 48×48 cells, rendered at native 2×. Cast/attack sheets have
  eight frames in each south/west/north row. Remnant walk has four frames per row.
- Warden: 64×64 cells, rendered at 128×128. Eight frames per selected action,
  twelve death frames. Walk is a front-facing weight-transfer animation, **not**
  a complete directional locomotion set.
- Blade side attacks: 64×80 padded cells at 2×. Existing body scale and ground
  origin are retained; extra canvas is weapon margin, not a larger character.
- Ground registration uses integer translation, no sprite resampling: Slime and
  remnant attack bottom pixel 47 (up to 1 and 3 px correction respectively), Blade
  bottom pixel 71 (up to 1 px correction). Warden walk keeps its native baseline.
- Blade contact pose begins at the production strike windup; anticipation and
  recovery are separate. Boss poses follow tell/active/recovery states, not a
  decorative loop independent of damage.
- `CombatArtTest` checks dimensions, transparent/nonblank frames, minimum unique
  frames per row and Blade strike-pose timing. It does not prove animation feel.

Tools reused: `CropSprite`, `ReviewAnimation`, `PackFrames`. Downloads can be
recovered with `tools/fetch-combat-repair.sh` without submitting new paid jobs.
See the [combat test report](../../testing/combat-repair-2026-09-06/README.md)
and [saved official PixelLab guide](../../reference/pixellab-mcp-guide.md).
