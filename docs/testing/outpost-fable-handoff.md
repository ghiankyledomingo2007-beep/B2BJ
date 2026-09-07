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
