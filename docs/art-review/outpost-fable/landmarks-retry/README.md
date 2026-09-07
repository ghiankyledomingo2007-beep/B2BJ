# Landmark retry — Ruined Outpost art pass (2026-09-07)

Second pass on the landmark targets that the first pass could not deliver, plus mud-based variants of two accepted
stone pieces. Every candidate is one `edit_image_pixen` job on the named source at its native canvas (the lean-to
also had two fresh `create_image_pixflux` attempts). No hand recolouring, resampling or stretching. Review: 4x
nearest-neighbour contact sheets (`sources-4x.png`, `contact-wave1a.png`, `contact-wave1b.png`, `contact-wave2.png`,
final `contact.png` = source beside accepted file), PIL stats via `sheet.py stats` (size, opaque count/%, edge
contact, green/gold/red-orange counts) and `colors.py` (top colours of the ground band, to judge shadow value).

Acceptance: exact canvas size, real alpha (40 px ≤ opaque ≤ 85 % of canvas), silhouette unchanged (or unrolled as
intended), no grass/sand/bright green, no glow/gold/red-orange, muted values. One retry per failing target.
Accepted candidates were copied unchanged to `assets/props/outpost/<target>.png` by `finalize.py` (asserts size,
alpha band, and refuses to overwrite).

## Spending (allocation `PIXELLAB_AUTH_HEADER`, trial)

| | remaining / used / total |
| --- | --- |
| Start | 21 / 19 / 40 |
| End | 11 / 29 / 40 |

10 generations spent of the 12 budgeted (7 first attempts, 3 retries). All pixen or pixflux at 1 generation each;
no Pro tools, no `edit_image`, no purchases. No API errors or rate-limit refusals; every job completed within one
30 s poll.

## Decisions

| Target (native) → file | Attempt | Job id | Decision |
| --- | --- | --- | --- |
| bedroll (64x48) → `bedroll.png` | 1 pixen on `landmarks/bedroll-rejected-2.png` | `be571cce-0d12-4e73-b7d1-3314e79f0fe3` | **Accepted.** Brown mud patch gone, thin dark contact shadow only, bedroll and straps unchanged. Still rolled, not unrolled — accepted per brief (rolled is acceptable). 25.3 % opaque (776 px). |
| stretcher (96x48) → `stretcher.png` | 1 pixen on `landmarks/stretcher-rejected-2.png` | `2bf008c7-f014-4e22-bba9-cc853dd78d75` | **Accepted.** All moss tufts removed, two poles and canvas wraps identical, thin dark shadow under the poles. 29.6 % opaque. |
| watch-platform (128x96) → — | 1 pixen on `landmarks/watch-platform-rejected-2.png` | `8bafd06e-6046-4a4b-adc7-4da45f1772dd` | Rejected (`watch-platform-rejected-1.png`): mud island gone and platform/ladder identical, but the ground is a large flat pale lavender-grey slab (104,103,116), ~700 px — lighter than the mud ground, not a thin dark shadow. |
| | 2 pixen, seed 7, "very small, very dark, near-black … no large grey ground shape" | `d55087f9-8f91-4eb1-a51e-8c85ac4a2d2e` | Rejected (`watch-platform-rejected-2.png`): worse — a light grey (158,158,158) slab under the deck plus pure-black blobs at the post feet. **Not delivered.** Closest candidate is `watch-platform-rejected-1.png` (correct silhouette, only the shadow value is wrong); darkening its ~700 slab pixels is a trivial PIL edit if the owner permits hand recolouring. |
| weapon-rack (64x64) → `weapon-rack.png` | 1 pixen on `landmarks/weapon-rack-rejected-2.png` | `c6f20b1c-00ab-4c53-bb05-0c0d1eb252dc` | **Accepted.** Grass/dirt pedestal gone, rack and spears identical, replaced by a dark grey-brown mud patch (lum 53–77, same range as the accepted `cart-mud.png` shadow). Caveat: the patch is broader than "thin". 31.2 % opaque. |
| gate-pier (64x112) → `gate-pier-mud.png` | 1 pixen on `assets/props/outpost/gate-pier.png` | `9a000d9a-4b01-4cd1-9993-0990beff0e29` | **Accepted.** Base tufts removed, dark contact shadow, masonry identical. 41.4 % opaque (source 41.3). |
| wall-end (48x64) → `wall-end-mud.png` | 1 pixen on `assets/props/outpost/wall-end.png` | `f17d0acb-9754-4178-92fe-124acde85cc0` | **Accepted.** Tufts removed, dark shadow at the base, masonry identical. 40.6 % opaque (source 39.4). 3 px touch the right edge (source touched 2). |
| lean-to (96x80) → `lean-to.png` | 1 pixflux, `direction: south`, barricade palette | `3d05bc99-2b07-4384-92c6-8c5cd91a5698` | Rejected (`lean-to-rejected-1.png`): structure is exactly right — front-facing, two posts, sagging canvas roof, dark interior — but on a dirt patch with olive grass tufts (89,106,49). |
| | 2 pixflux, seed 7, "floating on a fully transparent background with no ground drawn at all" | `fb602805-ced1-4383-b861-ba807296404b` | Rejected (`lean-to-rejected-2.png`): grass and dirt patch again. |
| | 2b pixen on `lean-to-rejected-1.png` (this dir), "remove the grass tufts and dirt patch, thin dark shadow under the two front posts" | `1f76c6ea-9e98-45a2-80be-4864a2ce85ec` | **Accepted** (`lean-to-fix.png`). Grass and dirt gone, posts / roof / interior identical, thin dark shadow only at the post feet. Front-facing, not corner-on, so the fallback edit of `landmarks/lean-to-rejected-1.png` was not needed. 39.1 % opaque. |

Note on the lean-to: the pixflux retry and the pixen cleanup of attempt 1 were submitted in the same wave (one extra
generation) because pixflux pads ground objects with scenery every time and the pixen "remove the base" edit has
been the reliable fix throughout this pass. The pixen cleanup is the delivered file.

## Detector caveat

`sheet.py`'s green counter (`g > r+25 and g > b+25`) reports 0 on both lean-to pixflux candidates although they
have obvious grass: the tufts are olive-yellow (124,134,49). Do not trust that column alone; look at the sheet.

## Files here

- `wave1.json` / `wave2.json` (from `make_wave1.py` / `make_wave2.py`): submitted request lists. Per job:
  `<name>-request.json`, `<name>-submit.json`, `<name>-poll.json`, `<name>-status.json`; ledger `jobs.json`.
- Raw candidates: `bedroll.png`, `stretcher.png`, `weapon-rack.png`, `gate-pier-mud.png`, `wall-end-mud.png`,
  `lean-to-fix.png` (accepted, copied unchanged to `assets/props/outpost/`); `watch-platform-rejected-1.png`,
  `watch-platform-rejected-2.png`, `lean-to-rejected-1.png`, `lean-to-rejected-2.png` (rejected).
- Review sheets: `sources-4x.png`, `contact-wave1a.png`, `contact-wave1b.png`, `contact-wave2.png`, `contact.png`.
- Tools: `sheet.py` (stats / contact sheet), `colors.py` (ground-band colour histogram), `finalize.py` (copy +
  assert).

Runtime integration (Java prop enum / placement) is out of scope and untouched.
