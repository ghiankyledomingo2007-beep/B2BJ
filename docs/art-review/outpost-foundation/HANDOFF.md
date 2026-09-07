# Ruined Outpost environment handoff

Owner requested continuation in **Claude Code, Fable 5.1, Ultracode** on 2026-09-07.

## Ownership and scope

You own the remaining Ruined Outpost map/environment pass: `src/RuinedOutpostMap.java`, relevant environment rendering in `src/B2BJ.java`, matching tests, environment assets and review documentation. You are not alone in the codebase. Preserve others' changes; never reset or revert unrelated work. Work in the isolated handoff worktree. Do not auto-merge or push to GitHub; deliver verified local commits and a report for review.

Read `docs/testing/map-art-audit/README.md` and its twelve screenshots first. It is the room-by-room plan. Preserve the Java17 game and twelve connected outdoor sections. No later biome, unrelated combat rebalance, new UI overhaul or character regeneration in this assignment.

## Completed technical slice

- Broken Palisade now has two aligned fence ends and a broad breach, a gently bending road rather than the ellipse stamp, two fallen-timber groups and one dull fallen shield/spear prop. Removed that room's generic gold cache/rubble and repeated procedural equipment stamps.
- Selected native PixelLab art: `assets/props/outpost/fallen-arms.png` (64x48) and `splinters.png` (64x64). Exact 2x rendering, ground-layer drawing, no new colliders. Inspect again next to both player forms when refining the scene.
- Terrain's authored path preserves all sixteen Wang transitions. **It is intentionally NOT active yet**: none of the generated ground candidates passed visual review. `loadEnvironmentTiles()` retains the stable legacy ground until an approved `assets/tilesets/ruined_outpost/outpost_ground_reviewed.png` is supplied.
- Authored bank rendering is ready, clipped to the cached union of real bank colliders, using cached corner masks. It falls back to the old boundary graphics until an approved `assets/tilesets/ruined_outpost/earth_banks.png` exists. No terrain candidate currently lives at either activation path.
- `OutpostFoundationTest` tests the injected bank-art path, absent-art fallback, aligned fence gap, non-solid debris, and sprite transparency/dimensions. Full suite has 51 Java test programs.
- Full tests caught debris inside the 180px spawn-clearance margin; moved it outward without relaxing the rule. `PlayerMovementTest` now approaches the actual fence coordinates rather than obsolete hardcoded positions. Decoration-kind coverage now uses the enum length (six kinds), retaining all collision and escape checks.

## What is NOT finished

This is a safe technical checkpoint, **not an approved visual foundation or a finished biome**. Default ground/borders remain the old blockout. The rest of the maps still need the planned art/layout revisions. Finish one strong reference scene before spreading a new look across all twelve.

Review `jobs.json`, request JSONs and raw PNGs here before more generation. Do not integrate rejected art because it passed dimensions/transparency tests. First terrain attempts produced bricks or engraved panels; banks produced bright brown/orange rims. Wide short Pixflux debris canvases produced sheet/icon layouts. Short square Pixen prompts worked better for loose props.

`create_topdown_tileset(mode="pro")` returned **Tier 1 is required for this** on the fresh trial key and was not charged. Do not repeatedly retry Pro or buy/upgrade a subscription. Current available standard workflows need better style/reference control. Consider generating/referencing approved base material tiles and then connected transitions, rather than long negative prompt lists. Verify the tool contract before using a new tool; reuse `tools/pixellab-call.py` for temporary-token access.

## PixelLab authority and credentials

Owner authorizes needed asset-generation spending and supplies keys in 40-generation batches. Notify the owner when an allocation runs out. Do not purchase credits or subscriptions. Never put bearer values in prompts, logs, config, tracked files or reports.

The launcher supplies credentials through process environment only. Check **presence only**, not values:

- `PIXELLAB_AUTH_HEADER`: first backup; last verified 40 remaining after the failed Pro request.
- `B2BJ_PIXELLAB_BACKUP_AUTH`: additional owner-supplied backup, not checked or used by the parent.
- `B2BJ_PIXELLAB_PREVIOUS_AUTH`: older allocation; last verified 3 remaining /37 used. This foundation pass spent 24 from it; earlier rain work spent 13.

Recheck balances before paid work because account state can change. Use the existing helper's `PIXELLAB_AUTH_HEADER` override; do not use the stale default MCP connection (it reported 0 remaining). If the background launcher did not preserve these variables, report that and ask the owner to supply them securely; do not search private histories/configs for keys.

## Required verification and reporting

Use `rtk` for shell commands. Use the existing native Java assertion tests and `build.sh`, no new framework. Run a full build with `JAVA_TOOL_OPTIONS=-Xmx256m`; save screenshot evidence and test results. Follow image review and test-first discipline. Do not claim 80% coverage: no coverage instrumentation is installed.

Maintain player-scale references, native 2x art, subdued environment values, consistent top-down 3/4 camera, true alpha, ground anchors and depth order. Red-orange stays danger; bright gold stays Ichor. Keep open fight floors, visible gate state, reachable exits, and Warden dodge/summon paths. All generated objects must have a gameplay, navigation or narrative purpose.

Do not close/restart the running game, terminals, other agents or unrelated apps. Do not change audio/system settings. Do not disable safeguards. Continue using the installed model `claude-fable-5-1[1m]` and session-only Ultracode; report any unavailable mode or permission block rather than silently substituting another model.

Capture script: `docs/testing/map-art-audit/MapAudit.java` (headless fixtures, not a playthrough). Report progress in `docs/testing/outpost-fable-handoff.md` with what actually changed, rejected candidates, credit balances, screenshots, tests and remaining limitations.
