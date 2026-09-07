# Outpost props: Pixen base cleanup (2026-09-07)

Goal: existing runtime props re-based onto dark wet mud (no grass / sand pedestals), an extinguished brazier, a dull shield/spear group, and a corner-free slate rubble pile. Every candidate is one `edit_image_pixen` job (1 generation each) on the source PNG at its native canvas size. No hand recolouring, resampling or stretching. Review: 4x nearest-neighbour contact sheets (`contact.png` = final originals vs accepted files; `contact-pass1.png`, `zoom-*.png` = intermediate looks), PIL stats via `inspect.py` (size, true alpha, edge contact, green/gold/red-orange pixel counts).

Balance (`get_balance`, allocation `PIXELLAB_AUTH_HEADER`): **start 32 remaining / 8 used / 40 total → final 21 remaining / 19 used / 40 total.** 11 generations spent (9 first-pass, 2 retries). One rate-limit refusal (`rubble-slate`, 8/8 concurrent jobs) was not charged and was resubmitted.

## Decisions

| Target (source → runtime file) | Job id | Decision |
| --- | --- | --- |
| `broken-cart.png` 80x80 → `cart-mud.png` | `b9e78bb6-9864-40dd-b051-cb5c423345e7` | **Accepted.** Grass and sand patch gone, thin dark mud shadow under wheels/body, cart and cargo unchanged. 8 residual green pixels are the green cargo bundle (rows 27-30), not grass. |
| `dead-tree.png` 64x112 → `tree-mud.png` | `f1a53d2d-6e42-49e6-8e3a-c0a23fcc7f7f` | **Accepted.** Tuft and mound removed; bare roots on dark mud with small shadow; trunk/branches unchanged. |
| `supply-barrel.png` 48x64 → `barrel-mud.png` | pass 1 `09a19aab-9428-4d24-9a06-43e29200f6df` rejected; retry `882177a3-175b-4f51-b654-487e701572bb` **accepted** | Pass 1 shadow spread to touch the right canvas edge (2 px hard clip). Retry (seed 7, "tight shadow, stays off the edges") is clean, barrel unchanged, opaque fraction identical to source. |
| `barricade.png` 96x64 → `barricade-mud.png` | `6f1ffc9c-7647-4e9f-87ff-9733a958972c` | **Accepted.** Grass/moss removed, dark mud shadow, timber unchanged. |
| `wall.png` 96x64 → `wall-mud.png` | `df0cf82a-ef43-45f4-bf58-410d8814c5db` | **Accepted.** Grass tufts removed, faint dark shadow at the base, stone blocks unchanged. |
| `brazier.png` frame 0 (64x64 crop, `brazier-frame0.png`) → `brazier-unlit.png` | `d8e64a5c-7b11-4a3c-b3da-166e8751e222` | **Accepted.** Zero flame / orange / gold pixels (source frame had 252 red-orange + 180 gold). Cold ash and charcoal in the bowl, dull dark iron, legs kept. The source's teal rim accents are gone; consistent with "tarnished iron, no glow". |
| `shield-cache.png` 64x64 → `shield-dull.png` | `d49d6ed3-194d-4064-a666-8d41d37af872` | **Accepted** (first try). Dented dull iron round shield + plain wooden spear leaning together, gold trim / sack / grass removed, muted greys and browns, small mud shadow. Zero gold or red-orange pixels (source had 62 gold + 37 red-orange). |
| `outpost-batch-27/waypost.png` 48x80 → `waypost.png` | pass 1 `b1eb043a-66b7-4a2d-a37f-1109641b7d88` rejected; retry `cd2c56ca-4a19-4590-a2da-8db07c0313c8` **accepted** | Pass 1 swapped sand for mud but kept pale pebbles. Retry (seed 7, "no pebbles, no pale pixels") is dark mud only; post and boards unchanged. |
| `outpost-foundation/debris-simple.png` 64x64 → `rubble-slate.png` | `a5e207de-4d8d-47d2-88e7-41aea3d27101` | **Accepted** after one edit. Source was already dull grey with no grass/gold, but slate masonry bled into all four corners (top edge 64/64 px opaque) and would clip as hard rectangles as a loose prop, so one edit was spent instead of a straight copy. Result: only the central pile on a dark mud shadow, no edge contact. The pile is a little more compact than the source scatter (silhouette not pixel-identical). |

Nothing was rejected outright; no target needed a second retry.

## Verification (`finalize.py`)

All nine runtime files match their source canvas exactly, are RGBA with binary alpha (no partial-alpha fringe, no near-black fill), and touch no canvas edge. Opaque fraction of canvas (accepted / source): cart 29.0/29.9, tree 23.8/24.7, barrel 51.7/51.7, barricade 54.9/56.3, wall 38.1/37.4, brazier 27.0/31.7, shield 26.4/37.4, waypost 26.5/27.3, rubble 34.5/68.6. Note: the brief's 30-85% opaque band is not met by cart, tree, brazier, shield and waypost, but their *sources* are already below 30% (tall/thin silhouettes with generous canvas), and removing a pedestal can only lower the number; the check was applied as "real alpha, non-empty (>=15%), not a filled canvas (<=85%)" and each file's fraction tracks its source.

## Files here

- `batch1.json`, `batch1b.json`, `batch2.json`: submitted request lists. `<name>-request.json` / `<name>-submit.json` / `<name>-poll.json` / `<name>-status.json`: per-job request, submit reply, get_image request and status reply. `jobs.json`: driver ledger.
- `<name>.png`: raw downloaded candidates (`barrel-mud-r2.png`, `waypost-r2.png` are the accepted retries).
- `brazier-frame0.png`: PIL crop of `assets/props/outpost/brazier.png` frame 0 used as the edit input.
- `sources-4x.png`, `debris-simple-8x.png`, `contact-pass1.png`, `zoom-check.png`, `zoom-rubble.png`, `zoom-retry.png`, `contact.png`: review sheets.
- `inspect.py` (zoom / stats / contact), `finalize.py` (copy + verify + contact).

## Tooling note

`tools/pixellab-call.py` now also maps `image_path` → `image_base64` (the key `edit_image_pixen` takes); `tools/test_pixellab_call.py` covers the mapping and stays green (3 tests). Runtime integration (Java prop enum / placement) is out of scope for this pass and untouched.
