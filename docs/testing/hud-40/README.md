# HUD, maps and recovery — September 7, 2026

Owner-requested UI overhaul, pulsing orb Ichor, slow passive regeneration and
slime corpse absorption. Ruined Outpost only. This extends the previous
[immersion pass](../immersion-40/README.md), not a finished-game declaration.

## Design and implementation

The GCD's sections 3.2 and 5 were consulted: health and gold Ichor remain together
at top-left, Blade time remains top-centre, current form stays top-right. The
owner's new regeneration and healing request explicitly changes the original
kill-only resource model. Corpse absorption was chosen over random heart drops;
it uses the slime's identity and gives the player a deliberate recovery action.

- Persistent lore/tutorial/control strip removed. Short context prompts remain
  only near interactions. Story opening/death/completion remain intact.
- Generated vitality and ability icons, framed health/Ichor HUD, form portraits,
  real dash/Tide cooldowns, full-meter signal and Blade timer.
- Local minimap projects current terrain, obstacles, nearby enemies, player,
  passages, Warden and final gate. Expanded map shows discovered area layouts,
  neighbouring unknown areas, connecting paths, names and recovery landmarks.
  Live map does not pause; a map opened from pause stays paused and is labelled.
- Ichor uses animated gold energy spheres, not teardrops. The baked ground shadow
  is cropped off without resampling; magnetic movement is unchanged.
- Living Blob regenerates **0.6 Ichor/sec**. No passive gain during Blade,
  reversion, pause, death or story overlays. Blade drain remains unchanged.
- Kills keep their original Ichor drop and leave up to **16** recent remains,
  lasting **20 seconds**. Blob can press **E** within **84px**, with clear terrain,
  to start a **0.8-second** stationary absorption. Movement intent (even against
  a wall), attacks, dash, transform and damage cancel it. No repeated consumption.
- Ordinary remains yield **5 Ichor and 0.5 HP**, bounded by meter/health limits.
  Boss summons give the extra energy but **never health**. Full resources do not
  waste a corpse. Healing feedback fires only when health actually increases.
- Feeding poses use native 48px cells rendered at 2×, with a shared foot row.
  Remains draw inward under the player and fade. Their actual position is retained
  on interruption, so they do not snap back. This is constrained pickup motion,
  not a new soft-body or fragment-physics engine.

The transformation tutorial's safe-room Q gate remains an authored progression
rule, with a compact Q prompt; removing HUD tutorials did not silently bypass it.
Existing synthesized audio cues are reused. No new music/audio mastering claimed.

## Regression evidence

New checks use the existing Java assertion runner; no new dependencies.

| Guarantee | RED observed | Check |
| --- | --- | --- |
| No persistent bottom tutorial panel | Existing panel occupied a clear playfield pixel | `HudOverhaulTest` |
| Full health icons retain their edges | Bright outer pixels were clipped/dimmed | `HudOverhaulTest` |
| Nearly horizontal absorption faces sideways | Sign-only aim converted 80,-1 to a diagonal/back pose | `HudOverhaulTest` |
| Map accessible while paused | Pause overlay obscured map frame | `HudOverhaulTest` |
| Passive gain, interruptible one-time recovery | Missing feature API; old exact-Ichor assumptions | `AbsorptionTest`, `IchorMagnetTest` |
| Real dash cooldown visible | Missing timer getter | `AbsorptionTest` |
| Health-only feedback, not boss-add false healing | No distinct healing event | `AbsorptionTest` |
| Absorption interruption does not snap remains back | Corpse position stayed at its original coordinates | `AbsorptionTest` |
| Correct grids, clean alpha, native frames, no orb shadow | Missing absorption runtime sheets | `RecoveryArtTest` |

TDD supplied runnable RED/GREEN checks; Ponytail kept this dependency-free and
reused the existing sprite pipeline. Security review kept credentials in temporary
process environments; no configuration or source token writes. No checkpoint
commits were made in the pre-existing mixed worktree. No pushes or releases.

Final `bash build.sh`: **37 Java test programs passed**, plus the repeated
packaged-asset loading check from `/tmp`; the runnable JAR was rebuilt.
Credential-helper tests: **2 passed**. PixelLab: the first **40/40** allowance
is exhausted; the backup used **6/40**, leaving **34**. All 27 jobs completed.
See the [finishing-art ledger](../../art-review/hud-finish-40/README.md).
No numeric coverage claim or real desktop/audio-device testing claim.

## Gameplay scenarios

[`AuditRecovery.java`](../../../tools/AuditRecovery.java) produced
[16 recovery observations](recovery-scenarios.tsv). Fixtures initialise actors;
combat then uses normal game actions without health/resource injection.

- Passive empty-to-full time: **166.67 seconds**.
- Under stationary Spitter pressure, starting at 3 HP: no absorption died at
  **6.43s** after 3 hits; an early absorption died at **8.82s** after 4 hits.
- A late channel was interrupted by the hit at **1.65s**; a second attempt
  completed at **2.50s**. This demonstrates a real interruption/counterplay window.
- Boss-add absorption left health at 3 HP; energy was recovered.
- Pause, capacity, expiry, room transition and all action interruptions checked.

[Adaptive Warden fixtures](recovery-boss/scenario-results.tsv), which do not yet
choose corpse absorption: starting at 0 Ichor won at **229.65s**, 3 HP, 4 transforms;
70 Ichor won at **205.51s**, 1 HP, 4 transforms; 100 Ichor died at **214.45s** with
43 boss HP. Every accepted hit occurred during reversion. These are not human
balancing results, and different initial meter timings are not monotonic difficulty.
An existing mild add-overlap observation remains a separate crowd-spacing concern.

## Visual review and reproduction

[`PreviewHud.java`](../../../tools/PreviewHud.java) deliberately stages the UI and
recovery states. Images are art/layout evidence, not an unassisted playthrough:
[HUD](hud.png), [expanded map](map.png), [pause](pause.png), [Blade](blade.png),
[absorption](absorbing.png), [healed](healed.png).

```bash
./build.sh
python3 tools/test_pixellab_call.py
preview_dir=$(mktemp -d /tmp/b2bj-hud-review.XXXXXX)
javac --release 17 -cp build/B2BJ.jar -d "$preview_dir" tools/PreviewHud.java tools/AuditRecovery.java
java -Djava.awt.headless=true -cp "build/B2BJ.jar:$preview_dir" PreviewHud
java -ea -Djava.awt.headless=true -cp "build/B2BJ.jar:$preview_dir" AuditRecovery /tmp/b2bj-recovery-results.tsv
```

Rebuilding does not update an already running game window. Relaunch to test the
new build. Human difficulty tuning, persistent saves, controller/remapping and
music remain unfinished work outside this pass.
