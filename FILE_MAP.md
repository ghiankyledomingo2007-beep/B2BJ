# B2BJ file map

The current campaign contains Ruined Outpost and Corrupted Forest. This map groups the
existing Java files by responsibility; `build.sh` continues to compile `src/*.java`.

## Entry points and world

- `src/B2BJ.java`: desktop entry point, input, frame loop and `--lab6` selection.
- `src/RuinedOutpostGame.java`: live game state, combat integration, encounters and checkpoints.
- `src/CampaignWorld.java`: both regions, landmarks, routes, enemy placement and the forest channel.
- `src/RuinedOutpostMap.java`: terrain, obstacles, region collision and map queries.
- `src/OutpostStory.java`: story text, discoveries, boss progression and completion.
- `src/CampaignSave.java`: atomic save/load at `~/.b2bj/campaign.properties`.

## Player and combat

- `src/Player.java`: health, Ichor, forms, movement, attacks and upgrades.
- `src/Wisp.java`: ordinary enemy movement, attacks and telegraphs.
- `src/CampaignEnemy.java`: region-specific enemy profiles.
- `src/Guardian.java`: boss behavior and attack cycles.
- `src/WaterProjectile.java`, `src/EnemyProjectile.java`: player and enemy projectiles.
- `src/CombatFeedback.java`: hit stop, camera shake and combat feedback.
- `src/GameAudio.java`: sound effects, music, mixer and volume settings.

## Rendering and animation

- `src/CampaignRenderer.java`: world, actors, effects, HUD and map rendering.
- `src/SetPieces.java`: Outpost and Forest scenery, placement and footprints.
- `src/OutpostLights.java`: world lighting and glows.
- `src/WardenFx.java`, `src/MobFx.java`: boss and ordinary-enemy visual effects.
- `src/Telegraphs.java`: attack warnings and hazard shapes.
- `src/SlimeAnimation.java`, `src/BladeAnimation.java`, `src/WispAnimation.java`: actor animation timing.
- `src/TransformationAnimation.java`: slime/Blade transitions.
- `src/WangTileset.java`: legacy terrain tile rendering used by older map paths.

## Menus and interface

- `src/Ui.java`, `src/PixelSwing.java`: shared visual components and Swing presentation.
- `src/TitleScreen.java`, `src/TitleMenu.java`: title presentation and choices.
- `src/LoadingScreen.java`, `src/Splash.java`: loading and startup display.
- `src/CampaignMenu.java`, `src/CampaignOptions.java`: pause menu and settings.
- `src/DebugPanel.java`: campaign testing controls.

## School demonstration

- `src/EnemyAiLab.java`: separate center-based chase demonstration.
- `test/EnemyAiLabTest.java`: detection boundaries, stopping, speed, controls and rendered states.
- `run-lab6.sh`: local Lab 6 launcher after building.
- [Lab 6 guide](docs/testing/lab6/README.md): specification, controls and demonstration order.
- [School document index](School/START-HERE.md): local GDD, reports and submission files.

## Assets and tools

- `assets/characters/`, `assets/effects/`, `assets/ui/`, `assets/audio/`: packaged actor art, effects, interface and sound.
- `assets/props/outpost/`, `assets/props/forest/`: regional scenery.
- `assets/tilesets/outpost/`, `assets/tilesets/forest/`: current regional ground art.
- `assets/source/`: editable art source files, excluded from JAR packaging.
- `assets/environment/ruined_outpost/`, `assets/tilesets/ruined_outpost/`: legacy room-mode art.
- [Art pipeline](tools/art/README.md): generation, editable exports and retouch safeguards.
- `tools/sprite3d/`: human sprite generation and packing pipeline.
- `tools/audio/`: sound generation and import helpers.
- Other files in `tools/` are preview, packing and inspection utilities; earlier helpers may describe historical pipelines.

## Build and verification

- `build.sh`: Java compilation, test execution, packaged-asset checks and JAR creation.
- `run-game.sh`: build and launch the regular game.
- `test/`: all Java assertion-based tests.
- `.github/workflows/`: CI and Windows release definitions.
- `build/`, `out/`, `release/`: generated outputs; excluded from Git.

## Documentation

Start with [docs/README.md](docs/README.md). Current design notes stay in `docs/design/`;
old campaign plans and audits are in `docs/archive/design/`. Art reviews and testing
fixtures keep their existing paths because tools and tests reference them, including
`docs/art-review/motion-polish/`.

The [previous file map](docs/archive/file-map-before-2026-10-21.md) retains the detailed
September combat walkthrough and historical pipeline descriptions.
