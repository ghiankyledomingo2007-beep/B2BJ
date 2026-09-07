# Rain rendering repair

User journey: rain should read as moving weather rather than identical sliding bars, without obscuring combat or slowing down with frame rate.

The old renderer used 35 identical rectangles and advanced their positions with `frameCounter`. The new renderer reuses the existing world paint path: staggered deterministic drop lifetimes, three streak depths, fading stepped 2× pixel tails, world-anchored landing points and short ground splashes beneath actors. Reduced effects draws one quarter of the drops and omits splashes. No particle queues, new dependencies, generated assets or paid jobs were added.

RED checkpoint `82b97a8` reproduced the timing failure: `rain must move by elapsed time, not frame count`. The same test passes after the fix. Its pause fixture was corrected to begin gameplay before requesting pause; prologue does not accept gameplay pause.

`RainRenderingTest` checks identical output after one second at 30/60 updates, pause/resume, native 2× pixels, depth variation, reduced clutter, world/camera anchoring and ground contact. Verification uses Java 17 assertions and the project's `build.sh`; no coverage percentage is claimed.

GREEN: `rtk proxy env JAVA_TOOL_OPTIONS=-Xmx256m bash build.sh` passed all 49 Java test programs, Java17 `-Xlint:all` compilation and packaged loading from outside the project. The final added resume assertion also passed against the built JAR. `git diff --check` passed. Java/stdlib-only changes introduce no npm/pip dependencies to audit.

Inspected matching staged room-zero screenshots: [before](rain-before.png), [after](rain-after.png). These are headless scene fixtures, not a live-motion or frame-pacing playtest. Custom PixelLab rain/splash art remains a possible next pass when the new allocation is configured; the preceding allocation is exhausted. No game restart or account/config change was performed.

Ponytail kept the fix inside the renderer; TDD supplied the timing and readability regressions.
