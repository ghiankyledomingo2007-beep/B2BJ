# Campaign guard animation integration — 2026-09-08

RED `f59f133`: `CampaignGuardArtTest` failed on the actual renderer: “guard must
render reviewed row 16 frame 0 at native 2x / fixed feet”. The reviewed atlas was
present but the campaign still rendered its blockout.

GREEN: the shared Wisp phase clock selects 8 anticipation, 4 contact and 4 recovery
frames. March and hurt use their own four-direction rows. Locked facing survives
re-aiming. Native 88px export cells render at integer 2x with fixed foot anchor;
the nominal 64px generation size is not used as a crop.

The edible corpse owns the 12-frame, 0.6s collapse and holds its last pose. Its
identity/facing survive death. It uses the existing absorption movement/fade,
healing rules and lifetime; no second live-body render, resurrection loop, or
new combat timer. Unfinished kinds retain their existing fallback.

Checks: exact opaque source pixels at 2x through all four directions and combat
phases; alpha/occupancy of exported frames; retained corpse identity; terminal
pose; no duplicate dying guard; existing outpost-variety and absorption tests.
Full `./build.sh` runs the complete assertion-main suite, including 50 admin
scenarios and packaged assets outside the repo. Numerical coverage is not measured.

Visual review: native contact sheets for march/bash/hurt/collapse plus actual
1280x720 campaign captures beside the slime. Guard is larger than the slime,
with boots aligned to its shadow and its health bar above the helmet. Cardinal
attack poses are accepted; three extra diagonal candidate rows remain unused.
No claim of a full human playthrough or final campaign art.

PixelLab provenance is included with `assets/characters/campaign/outpost_guard.json`.
Character `2b93b012-320b-47cc-a43e-732499e48695`. The connected allowance now reports
0 remaining (provider reports 41 used / 40); attribution of shared usage is not
assumed. Three owner-supplied backups were individually verified at 40 each and
stored only in the private config directory. The first backup is now funding the
next reviewed wolf/knight batch. No credentials or private config edits are committed.

Rejected candidate: Thorn Wolf `dca4b687-ae41-449a-a4d0-9018994d07d7`, too dark
and poorly readable on forest terrain. Its generated pounce is not integrated.
