# Ruined Outpost — Fable 5.1 handoff pass

Started 2026-09-07. Worktree `.claude/worktrees/outpost-art-fable`, branch `outpost-art-fable` created from `main` at `7d3d2bd` (the worktree's original branch `worktree-outpost-art-fable` pointed at the stale `446420e`; fast-forward and rebase were both refused by the session's permission classifier, so a fresh branch from `main` was created in place instead).

## Environment report (checked first, as instructed)

| Item | Status |
| --- | --- |
| Model | `claude-fable-5-1[1m]` (launcher argument, session model ID `claude-fable-5-1`). No fallback used. |
| Ultracode | Active for this session (`--settings {"ultracode":true}`, confirmed by the session reminder). |
| `PIXELLAB_AUTH_HEADER` | present |
| `B2BJ_PIXELLAB_BACKUP_AUTH` | present |
| `B2BJ_PIXELLAB_PREVIOUS_AUTH` | present |

Presence only was checked; values were not printed, copied or written anywhere. All PixelLab calls go through `tools/pixellab-call.py` with the temporary-token override.

### Balances at start (via helper `get_balance`, no charge)

| Allocation | remaining / used / total |
| --- | --- |
| `PIXELLAB_AUTH_HEADER` (first backup) | 40 / 0 / 40 (trial) |
| `B2BJ_PIXELLAB_BACKUP_AUTH` (newest backup) | 40 / 0 / 40 (trial) |
| `B2BJ_PIXELLAB_PREVIOUS_AUTH` (older) | 3 / 37 / 40 (trial) |

### Immediate finding: offline helper test leaked the live header

`python3 tools/test_pixellab_call.py` failed in `test_private_auth_and_image_encoding_never_leak_to_output` because the test assumes `PIXELLAB_AUTH_HEADER` is unset and lets the real environment through; with the launcher's live variable present, the assertion message printed the live bearer value into the terminal. Fixed by isolating the environment inside that test (`patch.dict(os.environ, …, clear=True)` without the variable). All three offline tests now pass. The value was not written to any file; treat the earlier terminal output of that failure as exposed if the log is retained.

## Work log

### 1. Baseline
- Fast-forwarded the isolated worktree to `main` by creating branch `outpost-art-fable` (see top). Full `build.sh` with `JAVA_TOOL_OPTIONS=-Xmx256m`: all test programs passed, JAR rebuilt. `MapAudit` captures regenerated for all twelve rooms (baseline identical to the audit's overviews plus the foundation checkpoint's Broken Palisade).
- The default PixelLab MCP connection (not one of the three handoff variables) reports 12 remaining / 28 used on its own key; it was only queried, never used.

### 2. Ground and bank foundation (owner: this session)
- **Eight PixelLab texture swatches, all rejected on sight** (`docs/art-review/outpost-fable/batch1-textures.json`, `jobs.json`, PNGs): mud came back as brown dirt with grass tufts (pixflux 32), blue ground with green bushes (pixflux 64) and orange-yellow speckle (pixen); flagstone came back as bright blue-grey bricks with grass (pixflux 32), a brick wall (pixflux 64) and bright cobbles on black (pixen); bank came back as pink soil with green plants (pixflux 64) and dark red-brown stripes with stone rows (pixen). Cost: 8 generations from `PIXELLAB_AUTH_HEADER` (40 to 32). Conclusion: the standard texture/tileset models do not deliver this contract; no further ground generations were bought.
- **Procedural composition instead:** `tools/ComposeGround.java` builds the two 128x128 Wang sheets the renderer already consumes (`WangTileset` cell order, 32px native cells, exact pixels, no resampling): hashed muted mud with sparse pebbles/wet glints, periodic Voronoi flagstones with low-contrast seams, a ragged (periodic-noise) mud/paving boundary with a darker wet seam, and a raised-earth bank texture with embedded stones. Five iterations were reviewed as a mock 10x6 map and in-scene via `MapAudit` (`docs/art-review/outpost-fable/ground/candidate-v3..v5-sample.png`): v1/v2 read as a brick grid and a pinwheel motif, v3 as fish scales, v4 as round cobbles, v5 as uneven squarish worn flagstones (chosen provisionally; an independent three-lens judge panel was run on v3-v5).
- Installed `assets/tilesets/ruined_outpost/outpost_ground_reviewed.png` and `earth_banks.png` from v5. Note for reviewers: these are code-authored sheets built from the reference palette, not PixelLab output; they activate the previously staged terrain and bank paths.

### 3. Parallel agents (isolated worktrees)
Spawned five workers at the owner's request for speed: MAP (room layouts, irregular bank depths, new dressing kinds, layout tests), RENDER (exact-shape earth banks with slope/lip/floor shadow, overlay removal when the reviewed sheet is active, uniform environment value shading, removal of the repeated remains stamp, placard-free gates, rest items), PROP-CLEANUP (pixen edits removing grass/sand pedestals, extinguished brazier, dull shield, waypost, slate rubble; primary allocation), LANDMARKS (fifteen new landmark/story props; backup allocation), PREVIEW (headless reference-scene, scale-sheet, viewport and Warden-clearance harness). Their results, balances and rejections are folded into the sections below as they land.

### 4. Results that landed
- **Judge panel on ground candidates** (3 lenses, independent agents): v5 won every lens (material 6/10, repetition 5.5/10, values 7.5/10). Applied their cheap fixes for v7: five larger slabs per 32px cell, earth-toned seams instead of navy grout, one small highlight cluster per large slab, 1px mud grain, no fixed glint cluster, and the tile-row wrap seam dropped from 8.7 to 2.2 luminance steps (interior 4.3). Not applied: per-cell tile variants to break the 64px repeat (needs renderer support; listed under limitations).
- **Prop cleanup (pixen edits, `PIXELLAB_AUTH_HEADER`):** 11 generations (9 targets, 2 retries), allocation 32 to **21 remaining**. Accepted after my own 4x review: `cart-mud`, `tree-mud`, `barrel-mud`, `barricade-mud`, `wall-mud` (grass/sand pedestals replaced by dark mud contact), `brazier-unlit` (cold ash, zero flame pixels), `shield-dull` (iron shield and plain spear, no gold, no sack), `waypost` (mud base), `rubble-slate` (compact slate pile). Ledger and contact sheet: `docs/art-review/outpost-fable/props-cleanup/README.md`. Opaque coverage of five results sits at 24-29% because their sources already did; alpha is real.
- **Renderer:** banks are drawn as one exact-shape earth mass (texture-filled collider union, 6px slope band, 2px lip, 8px floor shadow, union extended past the world border), the dark terrain overlay is skipped when the reviewed sheet is active, every environment sprite gets a uniform exact shade (0.84/0.86/0.92), the repeated remains stamp is deleted, PASS/HOLD placards are gone (locked = timber fence with one dull red-brown bar, open = two end posts), the gold rest strip is gone and the "E" marker is subdued teal and disappears with the consumed dressing. `OutpostFoundationTest` and `DepthRenderingTest` updated with the new contracts; full build green in the agent's worktree before integration.
- **Landmark props (pixflux/pixen, `B2BJ_PIXELLAB_BACKUP_AUTH`):** 29 generations, allocation 40 to **11 remaining**. Accepted by the agent and confirmed on my 4x review: `tower-footing` (ring of slate masonry, broken open, rubble inside), `gate-pier` (tall slate column), `wall-end`, `medical-chest`, `reeds` (pale grey sedge on a dark puddle; paler than wanted), `wheel`, `sack`, `canvas-debris` (timber and slate roof debris). Two candidates the agent rejected only on my over-strict 30% opacity floor were accepted by me after inspection: `bandage` (clean linen roll, 23% opaque) and `signal-mast` (fallen mast with torn navy pennant, 16% opaque); the packaging test only requires more than 20 opaque pixels and less than 85%. Not delivered after two attempts each: bedroll (rolled on a mud patch), lean-to (walled hut on grass), stretcher (moss tufts), watch-platform (intact on a mud island), weapon-rack (grass pedestal). A retry agent then applied one pixen base-cleanup each to the nearest candidates (10 generations, primary allocation 21 to **11 remaining**): accepted `bedroll` (clean but still rolled), `stretcher`, `weapon-rack` (small mud patch), `lean-to` (front-facing canvas roof on two posts), `gate-pier-mud` and `wall-end-mud` (grass tufts removed from the accepted piers). `watch-platform` failed both cleanups (pixen replaced the mud island with a pale slab); for it I accepted the first cleanup candidate after removing only the pale ground-slab pixels programmatically (alpha cleared for the lavender-grey slab range, no pixels repainted; `landmarks-retry/mask-platform.py`, contact sheet beside it). It is an intact deck on four posts with a ladder rather than a collapsed one. Ledger: `docs/art-review/outpost-fable/landmarks-retry/README.md`. Ledger and contact sheets: `docs/art-review/outpost-fable/landmarks/README.md`.
- **Preview harness:** `tools/PreviewReference.java` (reference scene in both forms, scale sheet of every kind beside Slime/Blade, gameplay-viewport captures of all rooms, Warden clearance overlay).
- **Map and layouts:** bank depths now vary 80-144 px per segment from a hash (still 160 px breaches at every exit); nineteen new `Decoration` kinds plus a `Prop.WEAPON_RACK`; per-room authored paving (`rect`/`ellipse`/`roads` helpers) replacing the shared ellipse-plus-spokes stamp; every room re-dressed per the audit table (see "Room by room"). The rest point moved to authored (440,296) so the bedroll/bandage can sit exactly on it while staying 180 px from spawn. `RuinedOutpostMapTest`, `PlayerMovementTest` and `ArenaTwistTest` were re-targeted (not weakened): the cot contact check now uses the infirmary's free-standing cot, the muster cover is the weapon rack with the same 96x48 feet eight combat tests rely on, retired kinds (CART, SHIELD_CACHE, BRAZIER, RUBBLE) are asserted absent. New `OutpostLayoutTest` checks door reachability, enemy/summon slot clearance, spawn distance, standable rest points, a brazier-free gatehouse with a clear guardian floor, and one landmark per room. `RainorayRenderingTest` now injects its own brazier fixture because no room lights one.

### 5. Integration, tests and evidence
- Props switched to the cleaned variants (`barricade-mud`, `wall-mud`, `barrel-mud`, `tree-mud`, `gate-pier-mud`, `wall-end-mud`); originals stay in the folder and in the JAR for archive, unused by any kind.
- `env JAVA_TOOL_OPTIONS=-Xmx256m bash build.sh` on the integrated tree: **all 52 test programs passed**, Java 17 `-Xlint:all` clean, JAR rebuilt, packaged loading verified from outside the project (`docs/testing/outpost-fable/build-log.txt`). One assertion written by the renderer worker hardcoded a bank edge at y=96 and broke once the map worker varied bank depths; it now derives the edge from the collider shape.
- Evidence in `docs/testing/outpost-fable/`: twelve half-size overviews (`NN-*-overview.png`), the reference scene as Blob and Blade plus the Blade-behind-fence occlusion check, the gatehouse with the Warden active, the gatehouse clearance overlay (guardian start r260 and all six summon slots clear of solids), the native-2x `scale-sheet.png` of every prop and dressing kind beside both player forms, and three viewport captures (Camp, Infirmary, Signal Tower). Fixtures, not an unassisted playthrough.

### 6. PixelLab spending summary (verified with `get_balance` at the end)

| Allocation | start | end | spent this pass | on |
| --- | --- | --- | --- | --- |
| `PIXELLAB_AUTH_HEADER` | 40 | **11 remaining / 29 used** | 29 | 8 texture swatches (all rejected), 11 prop base cleanups, 10 landmark retry edits |
| `B2BJ_PIXELLAB_BACKUP_AUTH` | 40 | **11 remaining / 29 used** | 29 | 15 landmark targets, first and second attempts |
| `B2BJ_PIXELLAB_PREVIOUS_AUTH` | 3 | 3 remaining / 37 used | 0 | untouched |

No allocation is exhausted; no purchase, subscription change, Pro tool or `edit_image` (20-40 cost) was used. The workspace `CLAUDE.md` now documents `~/.config/pixellab/swap <prefix>` for rotating the MCP key when an account reaches 0; it was not needed and not run. Every job id, prompt and decision is in the three ledgers under `docs/art-review/outpost-fable/`.

### 7. Remaining limitations (honest)
- **Ground and bank sheets are code-authored, not generated.** They pass the audit's material/palette intent in-scene, but the 16-cell Wang scheme repeats every 64 world px (the judge panel measured this); breaking it needs renderer support for alternate full-stone/mud cells. The stepped bank edge follows the collider rectangles in 8-px steps; it reads as cut earth, not a natural slope.
- **Owner visual approval is still required** for the sheets and for every new prop; my review and the agents' reviews are not the owner's.
- **Prop caveats:** `bedroll` is rolled, not unrolled; `reeds` are paler grey than the grey-green intended; `watch-platform` is an intact deck (the pale generated ground slab was removed by an alpha-only mask, no repaint); `canvas-debris` shows timber and slate rather than canvas; `weapon-rack` and `lean-to` keep a small dark mud patch; `wall-end-mud` came back slightly darker than `wall-mud`.
- **Locked passages** are still the procedural stake fence (now timber-toned with one dull red-brown bar); no gate sprite was generated. Open passages show two posts. Room-name UI still exists; the "hide names and still read purpose" gate was not run with a player.
- **No live playthrough, no frame-pacing measurement on the owner's laptop, no reduced-effects pass, no Warden fight with the new piers beyond the headless clearance overlay** (guardian start r260 and all six summon slots clear of solids). `PreviewReference` and `MapAudit` are staged fixtures.
- **Weather shelter:** the lean-tos imply dry space but rain still falls through them (no occlusion mask).
- **Test coverage:** 52 assertion programs pass; no coverage instrumentation exists, so no percentage is claimed.
- The worktree branch is `outpost-art-fable` (from `main` 7d3d2bd); nothing was pushed or merged. The five agent worktrees under `.claude/worktrees/agent-*` hold the same commits and can be deleted after review.

### Room by room (what the captures show)
| Room | Now |
| --- | --- |
| 00 Rain Ditch | Two bare trees, four reed clumps on a diagonal depression line, one wheel by a narrow worn track to the east exit; no cart, no rubble. |
| 01 Broken Palisade | Reference scene unchanged in layout: aligned fence ends with a broad breach, splinters and fallen arms, bending road; fences now on mud bases. |
| 02 Muster Ground | Brick blockouts gone; damaged weapon rack (96x48 cover) with fallen arms at the NW margin, waypost at the infirmary fork, rounded parade ground. |
| 03 Barracks | Broken-L foundation (wall + wall ends) in the SE with two cots inside, a spear rack on the wall line, canvas/roof debris, one fallen-arms group, one cold brazier. |
| 04 Standard Yard | Standard on an earth footing inside a paved ring, fallen arms and canvas facing it; no brazier, no cache. |
| 05 Supply Lane | Cart beside a staggered barrel group on a paved pull-off, torn sack on the lane, wheel by the east road. |
| 06 North Watch | Timber watch platform at the north boundary on a footing pad, parapet wall line with a broken end, cold brazier. |
| 07 Captain Camp | Lean-to with the bedroll on the rest point, the tree it leans on, cold brazier, fallen arms; blockout gone. |
| 08 Inner Court | Wall line with broken end in the NE facing the gatehouse, arms and splinters east, paving widening east. |
| 09 East Gatehouse | Two ruined gate piers flanking the east gate, slate rubble SW, fallen arms SE; no braziers; open floor. |
| 10 Field Infirmary | Cot, lean-to, cot on one line over a shelter pad, bandage on the rest point (hidden once used), medical chest, stretcher. |
| 11 Signal Tower | Collapsed tower footing at the north with rubble, fallen signal mast, cold beacon basket; round footing pad. |
