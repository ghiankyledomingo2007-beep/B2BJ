# Animation repair — in progress

The playback fixes below are in the rebuilt `build/B2BJ.jar`. **The full repair is not finished: no replacement artwork has passed review or been installed.** Restarting the game loads the rebuilt code; an already running game keeps its old code. No campaign save was used during verification.

## Repaired and verified

- Crescent takes its visual facing from the accepted cast aim, including casts without a preceding mouse-move event.
- Full-body cast, charge, guard and strike poses plant the feet. Strikes retain their short collision-safe attack steps; movement resumes afterward and dash can cancel eligible actions.
- Buffered M1 cannot replace the pose of an active dash; the queued attack still executes.
- Damage blinking is separate from dash invulnerability. Both human and slime remain visible during an undamaged dash.
- HOLD uses elapsed charge time, starts at the same frame regardless of world uptime, and keeps looping at full charge.
- Dead human holds a fixed hurt pose instead of breathing idle. This reuses existing art; it is not a new collapse animation.
- Transformation reveals idle frame zero. Movement interrupts the cosmetic morph, so its stationary silhouette cannot slide through a running action.
- Running direction changes retain stride timing instead of switching to a standing shuffle.
- Blue slash waves begin near the weapon and expand outward, instead of appearing immediately as a distant arc. Matching the drawn sword choreography in every direction still depends on the artwork repair.

Actual playback: [repair viewer](runtime-review.html). It uses the same 76 controlled scenarios as the [pre-repair audit](../human-motion-audit/RECHECK.md), with the same scope limits. JSON traces show **0.0 world pixels** of travel under moving CAST and CHARGE/HOLD in all four directions; both normal and delayed charges first show HOLD frame 0; the human morph first reveals IDLE frame 0. Cast rows, dash priority and dead-body state also pass all four-direction checks.

Validation: all **68 assertion programs**, Java 17 `-Xlint:all`, and packaged assets passed. `test/AnimationPlaybackTest.java` reproduces the actual input/render failures and checks resumed movement, buffered attack release, dash visibility, charge timing and death/morph state. Build hash and capture counts are in [validation.json](validation.json).

## Artwork still outstanding

The rejected original mask/run/transition/combat/morph drawings remain installed. Specifically: hands/arm rhythm, mask expressions and proportions, start/stop head and sword continuity, cast silhouettes, side hurt recoil, charge/hold weapon consistency, direction-specific attack choreography and transformation artwork endpoints. See the full [action-by-action audit](../human-motion-audit/README.md).

The eighth 40-generation trial allocation is saved privately. Final live balance: **3 generations** across eight saved allocations (1 + 1 + 1). This allocation turn spent 41 generations including two leftover credits from earlier keys; the full repair attempt has spent 50. All completed candidates were rejected and no replacement artwork was installed. The first Pro job failed without a charge; its shorter-prompt retry completed and charged20, but produced the wrong facing. No jobs remain pending. Exact per-allocation readings are in [budget.json](budget.json); credentials stay outside the repository.

| Candidate | Result |
|---|---|
| `run-south-v1`, `run-north-v1` (2 generations each) | More separate hands, but insufficient running stride/flight and continued head drift. Rejected. |
| `run-pose-south`, `run-pose-east`, `run-pose-north` (1 each) | East/south usable only as motion studies. North turns sideways instead of keeping its back view. No canonical base accepted. |
| `run-east-v2`, `run-south-v2` (1 each) | East holds much of the same leg silhouette; south rotates sideways and loses weapon consistency. Rejected. |
| `run-east-minimax` | Provider refused: tier 1 subscription or higher required. No job created. Extra trial allocations do not satisfy this tier requirement. |
| `canonical-mask-flash` (5 generations) | 80×80 alpha preserved and all pixels outside the face rectangle unchanged, but the red dot/lower line disappeared. Rejected. |
| `canonical-mask-pixen` (1 generation) | Red dot and side markings improve; lower mask remains an open-mouth shape. Rejected. |
| `mask-reference-grid` (9 generations) | Direct anime-reference edit preserves eight-pose grid, but mask proportions and lower line still vary. Rejected as canonical artwork. |
| `armed-template-rig`, `template-run-six` (1 + 4 generations) | Clearer alternating steps, but side views lose the sword, front hands rise toward the chest and mask/head shape drifts. Rejected; see [animated comparison](run-prototypes.html). |
| `idle-mask-grid-pixen` (1 generation) | Rewrites the mask as different oval faces, removes fringe in several frames and still changes markings. Rejected. |
| `pro-run-south` (quoted20, no charge) | Pro south run failed with provider `unknown error`. No frames returned. Live balance unchanged. |
| `pro-run-south-retry` (20 generations) | Requested south run; output is one south idle plus three east-facing run poses. Sword and mask still drift. Rejected. [Native contact strip](pro-run-south-retry/contact.png). |
| `idle-mask-imagegen/candidate.png` | Built-in image-generation experiment; zero PixelLab cost. Mask design improves, but output is opaque RGB with a painted checkerboard, 1774×887 dimensions and changed body pixels. Rejected; no sprite extraction or installation. |

Requests, sanitized responses and downloaded candidates are retained beside this report. Contact sheets preserve the original frame pixels. The image-generation experiment used the existing idle atlas as its edit target and `human-motion-fix/references/mask-3.png` as mask reference; prompt saved in that candidate's directory.

The owner approved manual pixel corrections on 2026-09-16. Work has started in [the manual editing folder](../human-motion-manual/README.md): four mask bases plus a 48-frame localized run draft, with editable Aseprite files containing original and corrected layers. The local scripts spend no generation credits. Run cleanup and artistic validation remain outstanding; these drafts have not replaced runtime art. Keep the fixed four-direction mask and successful running prototype as the acceptance gate before replacing transitions and the remaining actions. Do not ask for the same manual-editing approval again.

No commit or push was made during this repair.
