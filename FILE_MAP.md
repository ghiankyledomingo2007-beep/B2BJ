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
- `src/CampaignStory.java` — Outpost camp dialogue and optional discovery text.
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

## Presentation flow

1. Run `./run-game.sh`.
2. Press Enter to begin.
3. Explore the Outpost, defeat its encounters, then reach the east gatehouse.
4. Defeat the Outpost Warden and cross the eastern gate.
5. Use `Menu` or `Esc` to show controls during the presentation.
