# PixelLab rain art pass — 2026-09-07

Generated native pixel sprites now replace the procedural shapes when assets are available; the tested time-based weather lifecycle, world anchors, ground-first layering and reduced-effects behavior remain unchanged. The procedural shapes remain a missing-asset fallback.

## Selected art

- `assets/effects/rain_streaks.png`: two native32px canvases, rendered at2× without resampling. Fine and near streaks have real alpha and thin silver-blue silhouettes. Packing translates the authored bottom tip to row31; it does not redraw the image.
- `assets/effects/rain_splashes.png`: two rows of six distinct native16px poses, rendered at2× with a short opacity fade. The droplet sequence selects source frames0,1,2,3,5,8, ending transparent; the ripple selects1,2,3,4,5,6. Opaque splash frames4/6, a duplicate empty tail and the ripple's late reformation are deliberately excluded.
- Reproduce with `rtk proxy bash tools/pack-rain.sh`. Exact prompts, returned job IDs and all raw sources are in `docs/art-review/rain-weather/` (`requests.json`, `refinements.json`, `ripple-animation.json`, `final-requests.json`, `streak-pixflux.json`, `jobs.json`). Generation used the user's requested PixelLab service, not the default built-in image generator.

Rejected candidates included a cloud/weather icon, a bubble, orange splash, wrong-direction diagonal streaks and baked background/checker patterns. They are archived, not loaded into the game. The initial16px streak and eight-frame strip test expectations were updated to the reviewed native32px streaks and six clean splash poses; no opaque or repeated frames were waived through validation.

## Evidence

- `140e741`: RED — helper rejects the scoped Pixen generation tool. `e15381f`: GREEN — one allowlist entry added, all three offline credential/redaction tests pass.
- `8c09701`: RED — generated-weather renderer fields missing. `RainSpriteTest` now proves actual sprite consumption with synthetic markers, missing-asset fallback, exact atlas dimensions, distinct frames and transparent backgrounds/tails.
- `RainRenderingTest` passes with final sprites: equal motion at30/60 updates, pause/resume, native2× pixels, world/camera anchoring and reduced-effects density/contact behavior.
- Full `rtk proxy env JAVA_TOOL_OPTIONS=-Xmx256m bash build.sh` passed all50 Java test programs, Java17 `-Xlint:all` compilation and packaged loading. Three offline Python helper tests, Node/shell syntax and `git diff --check` also passed. No measured coverage percentage is claimed.
- `RainSpriteTest` also passed against `build/B2BJ.jar` from `/tmp`, verifying the generated weather assets load outside the project working directory.
- Five headless scene captures from `tools/PreviewRain.java`: four quarter-second samples and reduced-effects mode. Inspected player/HUD readability, varied streaks and small ground ripples. These are staged screenshots, not a live frame-pacing or audio playtest.
- Scoped credential-pattern scan found only existing deliberately fake credentials in helper tests. No new dependency, private config edit, GitHub push, game restart or external agent dispatch.

## Budget and limits

PixelLab confirmed13 used,27 remaining of this40-generation allocation; no active jobs remained. Temporary credential environment was unset and its shell closed. The token was not written to project files or configuration.

Rain streaks are close to vertical while drifting mildly with wind. Ground variations are cosmetic, not a wetness simulation; the outpost currently has no authored roof/weather shelter mask. Live subjective quality still needs the user's playtest. Image-generation review rejected unsuitable outputs, while Ponytail kept the existing lightweight renderer and TDD constrained integration.
