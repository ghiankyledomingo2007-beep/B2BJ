# Slime combat and VFX — 2026-11-02

The owner reported that slime combat rewarded repeated attacks while retreating, then approved a combo revision and asked that existing attacks also retain strong VFX.

## Implemented

- First and second water cuts curve in opposite directions (20px lateral bend). Their travel range is 180 world pixels, previously 340. Light impulse drops from 140 to 55, keeping targets within combo reach.
- Recovery is 0.40s / 0.44s / 0.68s across the three hits. Wind-ups are 0.08s / 0.10s / 0.22s. The third hit plants movement through 0.34s and advances 24px during 0.16–0.30s through the existing collision-safe movement function. Movement resumes during the remainder of recovery.
- Finisher damage rises from 32 to 40, impulse from 300 to 360, and range becomes 200px. It retains one-target collision, stagger and hit pause. Tide retains 32 damage, 280px range, broad multi-target collision and its 4.5s skill cooldown. Counter retains its existing reverse-dash qualification and 300px range, with 0.36s recovery.
- Slime casting frames now follow simulation release/recovery, including hit pause. Native-pixel body squash/stretch supplies anticipation and release, keeping the face and ground anchor together.
- Existing water sprites receive short foam arcs and moving droplets; basic, finisher and Tide impacts receive different-sized spray bursts. Finisher release adds a low water wake. Existing dash trail and authored art remain. Reduced effects suppress extra droplets and impact spray. No new ground indicators.
- Dash, damage and transformation cancel pending slime casts and their active pose. Pausing freezes the commitment. Holding LMB still works; no new binding or dependency.

Scope: four production files (B2BJ, RuinedOutpostGame, WaterProjectile, SlimeAnimation). No enemy AI, new charged skill, perfect-dodge qualification, new voice/audio asset, HUD, save-format or art-asset changes.

## Verification

Java17 scoped compilation with `--release 17 -Xlint:all -implicit:none` passes. Twenty-four focused test entrypoints pass across slime casting/combo/action/rendering, water/Tide collisions, blade combat, integration/stats, dash, sound hooks, motion/depth, HUD, campaign menus/victory and fullscreen. PackagedAssetLoadingTest also passes outside the repository.

New SlimeActionTest covers close-range distinction, planted wind-up, exact forward travel, pause, wall collision, recovery, damage/dash/transform interruption, simulation pose timing and invalid timing. It fails against the previous JAR at the range distinction assertion. Existing tests were adjusted for the intentional timing/damage changes and moved two rendering/travel fixtures away from banks that the wider curved path now correctly touches. Terrain-blocking checks remain.

Nine final packaged checks pass from `/tmp`: SlimeActionTest, SlimeComboTest, SlimeAttackRenderingTest, SlimeDepthRenderingTest, TideCrowdTest, WaterCombatTest, BladeCombatTest, HudOverhaulTest and PackagedAssetLoadingTest.

Actual renderer produced 276 frames across combo, Tide, counter and dash. Preview uses a fixed campaign camera and passive targets with increased health; real simulation damage/knockback, sprites and effects remain. Combo dealt 76 damage; Tide hit three targets once for 32 each; counter and dash each dealt 18. Preview video is silent; game audio is unchanged. Browser verified all four videos decoded and playing at 720×420, with no horizontal overflow. Browser automation timed out on the shared pause-button click and screenshot, so those browser interactions are not verified. Individual video controls remain available.

Preview: `docs/art-review/slime-action-2026-11-02/index.html`, open and retained in Chrome tab1863068438 at `http://127.0.0.1:4517/artifact/8ef436118e17/`.

## Installation and limits

Installed launcher build `build/B2BJ.jar` atomically after confirming the previous build still matched the backup. SHA256: `104d0717b966ae339144f077450de63fe0d86bb0aacb84466a47afd72740d091`. ZIP audit: only14 class entries across the four scoped families changed; all asset/audio entries byte-identical; changed classes target major61. The desktop shortcut points to this build.

User save hash stayed `d90b2c68f96a8fd1396a0046f728391a4395257b79721872652431e915d5f26d`, unchanged from task start through installation. Backup, exact diff, test logs, render helper/frames, candidate and audit JSON: `/home/ghiankylledomingo/tmp/b2bj-slime-action-2026-11-01/` (work crossed midnight).

No forced native-game restart, manual full combat playthrough, full-suite run, commit or push. Restart loads the installed changes. Human timing/balance still needs playtesting; this improves the existing combo's decisions and feedback but does not change enemy pursuit or eliminate every kiting strategy.
