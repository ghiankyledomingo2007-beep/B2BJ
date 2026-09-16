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
- `src/GameAudio.java` — synthesized sound effects, music and volume controls.

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

## Explaining the human animation

1. `B2BJ.step()` reads movement/actions and updates `BladeAnimation` with elapsed time.
2. `BladeAnimation.java` chooses idle, run, dash, slash, cast, guard or hurt. It remembers
   which way the player faces and selects a picture from that action's sprite sheet.
3. `B2BJ.drawBlade()` draws the chosen 80×80 picture at 2× size. Sheet rows mean
   south, east and north; west uses a mirrored east picture. The run has 16 pictures;
   idle and other actions have eight. Hair and clothing motion are drawn into the pictures.
4. `B2BJ.sideAttackFrame()` places the contact picture at the exact combat windup time.
   `RuinedOutpostGame` still decides whether the hit connects and how much damage it does.
5. `TransformationAnimation.java` advances the 16-picture slime/human transition.

`assets/characters/blade/rainoray_*.png` holds the human pictures,
`assets/effects/transform_*.png` holds transformations, and `assets/ui/blade.png`
is the portrait. `tools/pack-rimuru.sh` joins reviewed source pictures into these sheets;
it is a development tool, not part of the running game. See
`docs/art-review/rimuru-human/README.md` for sources, review and timing.

## Presentation flow

1. Run `./run-game.sh`.
2. Press Enter to begin.
3. Explore the Outpost, defeat its encounters, then reach the east gatehouse.
4. Defeat the Outpost Warden and cross the eastern gate.
5. Use `Menu` or `Esc` to show controls during the presentation.
