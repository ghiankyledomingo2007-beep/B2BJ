# Outdoor outpost verification — 2026-09-06

Scope: restore an open military-compound feel while retaining section-based
combat and progression. Integrate only technically clean PixelLab hit frames.
Follow-up: fix barricade depth ordering and integrate four reviewed environment props.

## Results

- Java 17-compatible compile with `-Xlint:all`: pass, no warnings.
- Seventeen assertion-based test programs: pass.
- Runtime assets loaded from outside the repository: pass.
- Shell syntax and `git diff --check`: pass.
- JAR inventory: main classes and active character/terrain/UI/effect assets plus
  the four curated `assets/props/outpost` images only.
  No tests, source files, rejected pilots, concept sheets, or unused shrine props.
- Headless final-JAR render: inspected outdoor ground, character scale,
  camera, HUD and a real resolved Blade hit with the curated gold impact.
- PixelLab latest settled balance: 27 trial generations remain, $0 extra credit.
  Owner authorizes needed generation without a project budget cap.

## Regressions checked

- Twelve-section graph connected; every road endpoint reachable with the
  player's foot collision radius. Sections exceed the viewport dimensions.
- Open breach crossed by actual movement without E; active encounter gate
  physically blocks passage; section arrival is inset from the edge.
- Walking and dash cannot tunnel into barricades. Enemy lunges cannot cross them.
- The original depth regression failed against the pre-fix renderer and passes
  with ground-contact sorting. Both Blob and Blade are occluded behind a palisade
  and draw over it from the front. Afterimages share the same depth queue.
- All four generated prop types stop player movement at their fitted footprint
  from north/south/east/west, without penetration or an oversized canvas collider.
- Native 2× palisade front/rear Blob and Blade renders, barracks cot, standard yard
  and North Watch wall inspected. Screenshots are under `docs/art-review/outpost-props`.
- Mouse-facing combat uses delayed impact, cooldown and damage invulnerability.
- Pause freezes simulation; held map does not. Overlapping D/right-arrow
  input survives release of one key. Focus loss pauses and clears input.
- Checkpoint death recovery retains cleared/visited sections.
- GCD Ichor drain, Blade armor and hit cost, pickup extension and recovery.
- Warden telegraph target stays fixed, hits do not cancel attacks, and the
  final strike releases its body collision and the eastern exit.
- The three selected 32×32 effect frames are distinct and predominantly
  transparent; visible pixels decrease from 276 to 225 to 132.
  Two generated frames failed transparency validation and were excluded.

## Limits

The route test uses deliberate actor placement and a nearly defeated boss
fixture. No claim of a complete unassisted live playthrough, 3–5-cycle boss
balance, line coverage, perceptual audio mastering, Windows installer testing,
or finished art. See the [current audit](../gcd-alignment-audit.md).

Existing running game was left untouched. Reopen `build/B2BJ.jar` or execute
`run-game.sh` to load this build. Progress is currently session-only.
