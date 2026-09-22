# B2BJ file map

Ruined Outpost is the current presentation build. Files are grouped by the part of the game they own.

## Start here

- `src/B2BJ.java` — desktop window, input bindings, game loop, rendering order and HUD wiring.
- `src/RuinedOutpostGame.java` — game state, movement, combat, encounters and checkpoint flow.
- `src/RuinedOutpostMap.java` — Outpost terrain, rooms, gates, collision, props and landmarks.
- `src/OutpostStory.java` — Outpost progression: prologue, scouts, Warden, escape, death and completion.

## Player and combat

- `src/Player.java` — Blob movement, Blade transformation, health, Ichor, dash and upgrades.
- `src/Wisp.java` — enemy movement, telegraphs, melee attacks and ranged attacks.
- `src/CampaignEnemy.java` — Outpost enemy profiles and shared enemy combat rules.
- `src/Guardian.java` — Outpost Warden behavior, attack cycles and damage windows.
- `src/WaterProjectile.java` — Water Slash, Tide Wave and finisher projectiles.
- `src/EnemyProjectile.java` — hostile projectile movement and collision.
- `src/GameAudio.java` — the sound mixer: recorded effects in `assets/audio/`, loops, music ducking and volume controls.
- `tools/audio/generate_sfx.py`, `tools/audio/import_free.py` — how the recordings are made (ElevenLabs) or imported (CC0 packs).
- `tools/sprite3d/` — the Blender puppet that renders every human-form sprite sheet (`build_sheets.py --install`).

## Visuals and interface

- `src/CampaignRenderer.java` — Outpost world, scenery, enemy sprites, boss, map and campaign HUD.
- `src/SlimeAnimation.java`, `src/BladeAnimation.java`, `src/WispAnimation.java`, `src/TransformationAnimation.java` — animation timing and sprite frames.
- `src/CombatFeedback.java` — hit stop, camera shake and combat feedback.
- `src/CampaignMenu.java`, `src/CampaignOptions.java`, `src/DebugPanel.java` — pause menu, settings and testing controls.

## Persistence and tests

- `src/CampaignSave.java` — atomic checkpoint save/load at `~/.b2bj/campaign.properties`.
- `src/OutpostStory.java` — Outpost progression, title text, death and completion.
- `test/` — assertion based checks for combat, rendering, map safety, input and packaged assets.
- `build.sh` — Java 17 compilation, test run and packaged JAR creation.
- `run-game.sh` — verifies, builds and launches the game.

## Assets

- `assets/characters/` — player, Warden and Outpost enemy sprites.
- `assets/effects/` — attacks, impacts, telegraphs, rain and pickups.
- `assets/environment/ruined_outpost/` — Outpost environment masks and scenery.
- `assets/props/outpost/` — Outpost props.
- `assets/tilesets/ruined_outpost/` — Wang terrain tiles and banks.
- `assets/ui/` — HUD icons and frames.

## Explaining the human animation and combat

1. `B2BJ` forwards mouse presses/releases and aim to `RuinedOutpostGame`.
   Movement faces travel; stopping keeps that direction. Fresh mouse movement aims
   while standing, and clicks aim attacks. An active strike keeps its aim.
2. `RuinedOutpostGame.primaryPress/primaryRelease` distinguishes short clicks from
   holds. A click advances the four-step combo; a hold charges one cleave on release.
   One short input buffer accepts a click near recovery. Pause, hurt, focus loss and
   form loss cancel stored input. The existing parry and Crescent stay separate skills.
3. `RuinedOutpostGame.resolveStrike()` applies damage once at the contact time.
   Thrust hits the nearest target in a narrow lane; the finisher pushes farther.
   `Player.attackStep()` uses the same wall collision as ordinary movement.
4. `BladeAnimation` selects idle, start, run, stop, turn, dash, cut, charge, hold,
   cleave, cast, guard or hurt. The selected pictures contain hair and clothing motion;
   there is no cloth-physics engine or online generation during gameplay.
5. `B2BJ.drawBlade()` draws native 80×80 pictures at 2× scale. New sheets have four
   directional rows (south, east, north, west). `bladeAttackFrame()` synchronizes the
   contact picture to the game's damage clock. `drawBladeTrail()` paints the blade's
   swoosh from a separate trail sheet; impact sparks happen only on a connected hit.
6. `TransformationAnimation` advances the slime/human transition.

Human pictures live in `assets/characters/blade/rainoray_*.png` (rendered from the
3D puppet in `tools/sprite3d/`, one command rebuilds all of them); transformations
in `assets/effects/transform_*.png`; portrait in `assets/ui/blade.png`.
`test/BladeCombatTest.java` checks click/hold timing, cancellations, charge cost,
combo sequencing, direction, wall collision, single-target thrust and pixel-size effects.
`test/AnimationPlaybackTest.java` checks cast facing, planted action feet, dash buffering
and visibility, local charge timing, death pose and transformation-to-idle timing.
Full-body human actions plant the feet; dash remains available under the existing
cancel rules. Charge animation uses time since the press, and running turns keep
the current stride. Only damage triggers blinking, not dash protection.
`tools/PreviewHumanCombat.java` produces input-driven development screenshots without
changing the campaign save. Art sources and the current plan live in
`docs/art-review/human-motion-fix/`. Its `selection.json` lists every source frame
used in each runtime sheet; `tools/pack-human-motion.py` rebuilds those sheets offline.

## Presentation flow

1. Run `./run-game.sh`.
2. Press Enter to begin.
3. Explore the Outpost, defeat its encounters, then reach the east gatehouse.
4. Defeat the Outpost Warden and cross the eastern gate.
5. Use `Menu` or `Esc` to show controls during the presentation.
