# Ruined Outpost motion/effect pass

Status: final selected art packed; full build and all 48 Java test programs passed on 2026-09-07. The refreshed artifact is `build/B2BJ.jar`. Ready for live playtesting, not a claim that the entire game is finished.

## Scope

- Side engulf uses a sustained low spreading body followed by reformation; collision position does not slide with the artwork.
- Rainoray keeps native 80px cells and 2× drawing. Running cadence is 14 FPS without changing movement speed, collision, skill damage or cooldowns.
- Guardian uses seven eight-pose rows at native 64px / 3× drawing: idle, approach, windup, strike, recovery, charge and death. Living damage blink must not hide the death sequence.
- Tide has a source burst beneath actors, directional moving water, foam wake, collision splash and quieter natural expiry. Actual projectile release/expiry produce their own events. Source feedback stays at the event position, not attached to the moving player.
- New release/impact audio cues reuse the existing synthesizer; no new runtime dependencies.

## Regression checkpoints

- `c6174b5`: missing Tide release/expiry events reproduced; `f03ed83`: event fix.
- `fc2efe5`: missing eight-frame Guardian animation API reproduced; `66a3221`: state mapping.
- `adc82e4`: cadence, side-body coverage and missing presentation assets reproduced.
- `981d6b6`: repeated sword lift and death-blink presentation checks.
- `4c4a11c`: opaque Tide release hiding the player reproduced.
- `82315ab`: actual projectile kill reproduces the real-loop death freeze. The fix continues ticking the dead Guardian but returns before attack, damage and summon logic. Tests cover all eight poses, single clock ticks, expiry, pause and seven seconds without new attacks/adds.

## Art selection and reproducibility

PixelLab request files and accepted job IDs are under `docs/art-review/motion-polish/`. Rejected candidates remain there as evidence, not automatically selected for runtime. `tools/pack-motion-polish.sh` identifies the selected sources and packs native pixels without resampling. Loop endpoints are not duplicated in the final eight-frame strips.

Rejected findings included an upright neck during side feeding, repeated sword draw/salute during running, an ocean-strip projectile, a portal-like hit effect and a half-buried boss death pose. A generated image being complete was not considered visual approval.

The initial running metric (white tip movement <=5 absolute pixels) was replaced with the actual visual requirement: blade tip remains above the hood throughout the loop. Natural wrist movement is allowed; it was not mislabeled torso bob. All 24 selected poses pass, while the original north redraw has five lowered poses and the rejected locked-north candidate has three. [Review details and pixel measurements](README.md) record the explicit criterion change.

## Final verification

- Ten focused Java checks passed, including Tide layering/events, Guardian presentation/mobility/pressure, water combat, Rainoray skills, arena mechanics and pixel-preserving packing.
- Three offline PixelLab helper tests passed; temporary credentials are redacted and do not replace private config.
- Shell syntax, Node syntax and `git diff --check` passed.
- Scoped credential-pattern scan found only the deliberately fake test secret in `tools/test_pixellab_call.py`.
- Final `rtk proxy env JAVA_TOOL_OPTIONS=-Xmx256m bash build.sh` passed after all selected art and the real-loop death fix: Java 17 compilation with `-Xlint:all`, all 48 assertion-based Java test programs, JAR packaging, and a second packaged-asset check from outside the project directory. No compiler warnings were emitted.
- Complete generation ledger validated: 39 unique jobs costing 40 generations, with all 287 returned frames downloaded. Every selected strip has eight distinct poses; side engulf has sixteen per facing. The duplicated pinned loop endpoint is not repeated in an eight-pose strip.
- Final headless review covers 197 captures: 36 run, 38 side-engulf, 63 boss and 60 Tide. Six real Tide hits preserve two damage; all twelve hit/miss projectiles end. Detailed visual limitations are in the review README.

## Generation budget

PixelLab's final response confirmed `generations_used: 40`, `generations_remaining: 0`, `generations_total: 40`, `credits: $0.00`. No jobs remain pending. The temporary credential environment was unset and its shell closed after verification; private config was not changed. No further generation was submitted after this allocation was exhausted.

## Remaining polish / live checks

- Tide reads as a pulsing water bolt rather than a rolling ground wave. The final scattered droplets have somewhat regular spacing; they are short-lived and faded by the existing effect timer.
- A downward miss can finish behind the HUD. UI occlusion is expected at the screen boundary, but full-speed readability still needs a human playtest.
- Running preserves a raised blade with natural wrist variation. Frame checks do not certify perfect full-speed foot planting.
- Guardian death now tips onto its side; its final generated palette is simpler than the standing frames. A four-native-pixel maximum packing offset grounds the prone endpoint at the existing row56 anchor without rescaling.

## Boundaries

Headless snapshots and assertions test real input/event paths where described in [the visual review](README.md); reflected boss pose fixtures are explicitly not complete live fights. They do not prove live animation feel, audio quality on the laptop, input latency, or whole-biome completion. No coverage percentage is claimed. No npm/pip dependency audit applies to this Java/stdlib-only change.

Ponytail kept existing renderer, effects queue and audio paths instead of adding a new framework. TDD and verification checks caught and constrained the fixes. No Fable, external agent dispatch, GitHub push, credential/config change, app closure or game restart was performed in this continuation.
