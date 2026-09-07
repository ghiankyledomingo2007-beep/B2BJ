# Landmark prop candidates — Ruined Outpost art pass (2026-09-07)

Fifteen missing landmark/story props requested at fixed native canvas sizes; every candidate reviewed at 4x on a
navy ground beside `wall.png` / `barricade.png` / `cot.png` / `fallen-arms.png` and a 2x Slime frame
(`contact.png` also carries a 2x Rainoray frame). Acceptance rule: real alpha with 30–85 % opaque pixels, no
near-black fill, top-down 3/4 camera (not isometric, not side view), no grass/sand pedestal, no glow/gold/bright
red-orange, muted values, readable silhouette at 2x, single object. One retry per failing target, no resampling,
no repainting. Accepted files were copied unchanged to `assets/props/outpost/<target>.png`.

## Spending (owner backup allocation `B2BJ_PIXELLAB_BACKUP_AUTH`, trial)

| | remaining / used / total |
| --- | --- |
| Start | 40 / 0 / 40 |
| End | 11 / 29 / 40 |

29 generations spent (15 first attempts + 14 retries) of the 34 budgeted; 5 unspent because each target gets at
most one retry. All calls were pixflux or pixen at 1 generation each; no Pro tools, no `edit_image`, no purchases.
No API errors; every submit returned a job id and every job completed within ~2 minutes.

## Tooling in this directory

- `wave1.json` / `wave2.json` (from `make_batches.py`): first attempts, all `create_image_pixflux`, `view: high
  top-down`, `no_background`, `selective outline`. Stone pieces forced to `palette-stone.png`, timber pieces to
  `palette-timber.png` via `color_image_path`.
- `palette-stone.png` / `palette-timber.png` (from `palette.py`): the colours of `wall.png` / `barricade.png` with
  the saturated grass-green tufts removed, so a forced palette cannot reproduce a grass pedestal.
- `retryA.json` / `retryB.json` (from `make_retries.py`): standing pieces stayed on pixflux + palette and added
  `direction: south` plus "front face square to the camera, not rotated, not isometric"; ground pieces moved to
  `create_image_pixen` with "nothing else in the image" wording.
- `sheet.py` (contact sheets + opaque/near-black stats), `archive.py` (accept with size/alpha assertion, or rename
  to `-rejected-N`), `balance.py` (redacted balance via the helper). Per job: `<name>-request.json`,
  `<name>-submit.json`, `<name>-poll.json`, `<name>-status.json`; ledger `jobs.json`.

## Decisions

| Target (native) | Attempt | Job id | Decision |
| --- | --- | --- | --- |
| tower-footing (128x128) | 1 pixflux, stone palette | `8bfb5d9c-5896-48d4-8a34-e7cead288b07` | **Accepted.** Slate ring two courses high, broken open on the south side, rubble inside; high top-down, 62 % opaque, palette matches `wall.png`. |
| gate-pier (64x112) | 1 pixflux, stone palette | `6d1e6cc0-1517-4e16-9a03-a3a6b7a15098` | Rejected (`gate-pier-rejected-1.png`): drawn corner-on (isometric read), mud/rubble patch and moss at the base. |
| | 2 pixflux, stone palette, direction south | `d90807d3-3f94-4cb3-93c7-a7bdf39aba90` | **Accepted.** Straight-on column in the wall palette, broken top, 41 % opaque; base tufts match the existing `wall.png` treatment, no island. |
| wall-end (48x64) | 1 pixflux, stone palette | `731c4c0f-848d-486b-afc5-dd913bdbff6b` | Rejected (`wall-end-rejected-1.png`): an arched gateway with a portcullis, 86 % opaque. |
| | 2 pixflux, stone palette, direction south | `678a79b3-e18f-452b-b787-0bfe452a7af2` | **Accepted.** Three-course stub with a stepped crumbling top, same masonry as `wall.png`, 39 % opaque. |
| medical-chest (48x48) | 1 pixflux, timber palette | `77c0cf06-3cb6-4fae-8209-2c0f16aa05c8` | Rejected (`medical-chest-rejected-1.png`): corner-on isometric box. |
| | 2 pixflux, timber palette, direction south | `d95026fb-c196-4ca2-8672-b285d55f5d70` | **Accepted.** Front-facing battered chest with iron straps, no pedestal, 49 % opaque. |
| reeds (64x48) | 1 pixflux, no palette | `7aaf9eee-da32-4f13-81bc-68a36ac7c86d` | Rejected (`reeds-rejected-1.png`): vivid saturated green on a water ring. |
| | 2 pixflux, stone palette forced | `8ac03a1b-02bb-4673-a639-255b298de3aa` | **Accepted.** Pale grey-blue sedge in a dark mud puddle, 38 % opaque; the puddle is the subject's own mud and sits close to ground value. Caveat: reeds read pale grey rather than grey-green. |
| wheel (32x32) | 1 pixflux, timber palette | `520065e6-04ba-4c1e-8d46-2ab1c7505f3c` | Rejected (`wheel-rejected-1.png`): three grass tufts and pebbles around the wheel. |
| | 2 pixen | `9b163419-989b-47ca-a15d-fd7a26b1bb49` | **Accepted.** Wheel only, from above, muted brown, 58 % opaque. |
| sack (48x32) | 1 pixflux, no palette | `8404715d-9794-4252-82eb-bfab08d03380` | Rejected (`sack-rejected-1.png`): bright orange grain (danger colour), grass, dark fill. |
| | 2 pixen | `bf1ba5ef-727f-40f9-99e0-e25af511df95` | **Accepted with a flagged deviation.** Clean grey sack with dull spilled grain, transparent, muted, single object; 26 % opaque is 4 points under the 30 % floor. Copied because everything substantive passes; drop it if the floor is strict. |
| canvas-debris (64x48) | 1 pixflux, timber palette | `8025f059-374c-41ae-9ecf-3b63ab37e4af` | Rejected (`canvas-debris-rejected-1.png`): a whole scene with a fence behind it, 73 % opaque. |
| | 2 pixen | `10c12386-4c42-489f-82ab-be86df423df8` | **Accepted with caveat.** Loose flat pile of broken roof timbers and slate, muted, 38 % opaque; no visible canvas, so it reads as roof debris rather than torn canvas. |
| bedroll (64x48) | 1 pixflux, no palette | `500a0efc-542e-43f0-ab5d-55869943f9c1` | Rejected (`bedroll-rejected-1.png`): bedroll is fine but a fence rail and grass tufts were added around it. |
| | 2 pixen | `118006b1-347c-4a52-adc9-555b8a3d799b` | Rejected (`bedroll-rejected-2.png`): rolled up instead of unrolled flat, on a brown mud island. **Not delivered.** |
| lean-to (96x80) | 1 pixflux, timber palette | `5d514a63-03e8-4942-b7c9-a50171453f8f` | Rejected (`lean-to-rejected-1.png`): plank back/side walls, corner-on, flagstone pedestal. |
| | 2 pixflux, timber palette, direction south | `eaa86336-9473-48ea-8be8-4651cefbd879` | Rejected (`lean-to-rejected-2.png`): a walled hut with a pitched roof on a grass island. **Not delivered.** |
| stretcher (96x48) | 1 pixflux, timber palette | `c29fd48e-feb2-455a-824e-1bbf82a1f895` | Rejected (`stretcher-rejected-1.png`): dark navy fill behind it, moss clumps in the corners. |
| | 2 pixen | `a3d2eb30-d906-44e3-b336-5e5490ce91fc` | Rejected (`stretcher-rejected-2.png`): two poles with cloth wraps and green moss between them, no canvas. **Not delivered.** |
| bandage (32x32) | 1 pixflux, no palette | `496353c5-55ee-475f-9b05-a7c36f54ebf9` | Rejected (`bandage-rejected-1.png`): clean roll but only 23 % opaque. |
| | 2 pixen | `b8b8bf3e-3451-4253-92bc-d5328da3603e` | Rejected (`bandage-rejected-2.png`): pixen ignored `no_background`, full cobblestone scene, 100 % opaque. **Not delivered.** |
| watch-platform (128x96) | 1 pixflux, timber palette | `ea30ccf2-763e-4abd-b137-70a0b8a7fae8` | Rejected (`watch-platform-rejected-1.png`): isometric diamond deck, intact. |
| | 2 pixflux, timber palette, direction south | `37e41415-89cd-41e5-9868-f6285d17952f` | Rejected (`watch-platform-rejected-2.png`): now front-facing and in palette, but intact (not collapsed) and on a wide brown mud island. **Not delivered**; closest candidate if the owner accepts an intact platform. |
| signal-mast (96x64) | 1 pixflux, no palette | `b24fb40b-36dc-4c34-b49e-dad8ff5a1a84` | Rejected (`signal-mast-rejected-1.png`): mast on a large grass/rock island, no pennant. |
| | 2 pixen | `bac9b388-45b1-4238-a723-3bd3d70073b0` | Rejected (`signal-mast-rejected-2.png`): pennant and crossbar right, but 16 % opaque and on an olive mud mound. **Not delivered.** |
| weapon-rack (64x64) | 1 pixflux, timber palette | `52f93874-12c2-4625-8731-e93e2ce52560` | Rejected (`weapon-rack-rejected-1.png`): grass-diamond pedestal, isometric, no spears. |
| | 2 pixflux, timber palette, direction south | `b79aaa6c-7f54-496c-834e-d702e43cc6b2` | Rejected (`weapon-rack-rejected-2.png`): rack reads, but on a grass/mud island. **Not delivered.** |

## What worked and what did not

- Forced palettes from the accepted references (`color_image_path`) reliably matched `wall.png` / `barricade.png`
  values; every stone piece landed in the wall palette.
- pixflux `high top-down` draws standing objects corner-on unless `direction: south` and straight-on wording are
  given; with them, pier, wall-end and chest came out front-facing.
- pixflux pads ground objects with scenery (grass, fences, mud islands). pixen fixed wheel, sack and debris but
  still added mud mounds/moss to thin objects (mast, stretcher, bedroll) and once ignored `no_background` (bandage).
- Wide thin objects (mast 96x64, stretcher 96x48) cannot reach the 30 % opaque floor; a future request should use a
  tighter canvas or a stricter review rule for thin props.
- Remaining seven targets need a different approach (Pro with reference images, or hand-authored from the timber
  and stone modules) rather than more pixflux/pixen retries.
