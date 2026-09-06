# Water combat verification

- RED: added `WaterCombatTest` before gameplay changes. Build failed on missing
  projectile list, secondary skill, cooldown and water-impact APIs (17 compiler errors).
- Replaced obsolete jelly-fist render assertions with detached-projectile, native
  2x pixels, four-direction rendering and RMB-skill checks. Removed punch-pose assertions.
- GREEN, core pass: all 19 assertion-based Java test programs passed, including
  the main route, boss exit, pause/focus, collisions and packaged-JAR asset loading.
- Added cover, closed-breach, boss armor, knockback wall, dash cancellation and death
  cleanup regressions. Those checks also passed before the final art collection.
- Final GREEN: all 19 Java test programs pass after all six sheets are integrated.
  Native dimensions, transparent margins, distinct poses, rear-water fence occlusion,
  basic non-stagger behavior, heavy knockback and external-directory packaging pass.
- PixelLab helper's offline credential-safety test passed. `git diff --check` passed.
- No dependency added. Existing collision, sound synthesis and depth sorting reused
  under Ponytail. Dirty user worktree preserved; no checkpoint commit, reset or push.
- No instrumented coverage percentage claimed. In-game scene fixtures are visual
  checks, not a complete human playthrough or a listening/mastering test.
