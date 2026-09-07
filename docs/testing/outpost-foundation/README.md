# Foundation checkpoint — 2026-09-07

The technical slice is ready for handoff; the visual foundation is **not complete**. [Current breach preview](breach-checkpoint.png). [Full map audit](../map-art-audit/README.md). [Claude handoff and remaining work](../../art-review/outpost-foundation/HANDOFF.md).

## Changes and tests

- Broken Palisade: aligned fence ends, broad breach, curved approach, grouped native fallen equipment/timber, no gold corner-cache/rubble or repeated procedural remains. Debris is ground-layer and walkable; kept 180px spawn/door clearance.
- Prepared native Wang terrain and clipped bank-art paths, with cached bank geometry/masks. Unapproved terrain remains archived. The stable original terrain and procedural bank presentation stay active until better art is accepted.
- `2f94ecf` RED helper rejects tileset tools; `08dc936` GREEN three offline credential/redaction tests.
- `142adff` RED authored transitions discarded; `213c022` GREEN native transition preservation. That path is staged behind the reviewed-asset filename; the legacy visual fallback is explicit, not a claim of completed art.
- `24871f5` RED unaligned breach; `0bebe7f` GREEN aligned layout, injected native bank rendering and missing-art fallback, reachability and depth checks.
- `caa2395` RED missing bank asset. **Visual approval failed, so mandatory bank activation was deferred**, not waived through with a rejected sprite. The final check verifies dimensions when an approved bank exists and explicitly exercises both injected-art and missing-art rendering. Selected debris presence/real-alpha requirements remain mandatory.
- Full test run caught debris too close to spawn: moved it outward, retaining clearance assertions. Updated the obsolete four-decoration count to enum coverage and changed fixed-coordinate fence tests to use the actual fence anchor. Collision tolerances and dash checks remain intact.
- Final `rtk proxy env JAVA_TOOL_OPTIONS=-Xmx256m bash build.sh`: all **51 Java test programs passed**, Java17 `-Xlint:all`, rebuilt JAR and packaged loading from outside the project. Three offline Python helper tests passed. No new dependencies or measured coverage percentage.
- Headless whole-world fixtures rendered for all twelve maps; inspected the final Broken Palisade checkpoint. Earlier audit inspected all twelve before changes. This is not a fresh unassisted playthrough, live frame-pacing benchmark or full art approval.

## Art and spending

Used the owner's requested **PixelLab** service with the image-generation review workflow. Prompts and all job IDs/results are in `docs/art-review/outpost-foundation/`. Selected `remains.png` becomes `assets/props/outpost/fallen-arms.png`; `logs-simple.png` becomes `assets/props/outpost/splinters.png`. Native sizes retained at 2x, no resampling or manual repainting.

Rejected terrain included brick-like floors, engraved panels and bright repeating brown/orange bank rims. Rejected debris included a rock tower, intact fence and icon-like sheets. Pro terrain failed with `Tier 1 is required for this` and charged nothing. No rejected bank/ground asset remains active; the temporary bank activation file was moved back to the art-review archive and the original terrain PNG restored from Git. All candidates remain recoverable here.

Older key: **37 used /3 remaining** (24 spent in this pass,13 in prior rain work); provider reported no active jobs. First backup: **40 remaining**, verified after the failed Pro request. Newest backup: not queried or used. No secret values written to project/config files. Credentials are passed only through the handoff process environment.

Ponytail kept the existing Java renderer and stdlib test tools; TDD exposed a real terrain-path issue and placement/test assumptions. Still needed: visually approved shared terrain/banks, remaining room landmarks and prop consistency, weather shelter masks, both-form art checks, live performance and gameplay review. No GitHub push or game restart.
