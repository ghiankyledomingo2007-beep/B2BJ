# Player animation recheck — 2026-09-16

Historical pre-repair evidence. See [repair progress](../human-motion-repair/README.md) for the subsequent playback fixes and outstanding artwork.

**Result: human motion still needs repair.** The first audit found inconsistent drawings; this pass also reproduces playback and transition problems. No game code or installed artwork was changed, and no generation credits were spent.

Open [actual playback review](runtime-review.html) for four-direction comparison, normal/quarter/half speed and frame stepping. [Original artwork audit](README.md) covers every installed human action and all 688 source cells.

## Confirmed playback problems

| Priority | Problem | Evidence and cause | Repair target |
|---|---|---|---|
| High | Crescent pose can face away from its projectile. | `cast` in all four directions: click opposite the previous facing without a preceding mouse-move event. The projectile follows the click, but CAST keeps the old row. For example, down target renders row 2 (up). `RuinedOutpostGame.ichorCrescent()` does not set visual aim; the `CRESCENT_CAST` handler only starts the animation. | Give the cast pose the accepted cast direction. Check stationary, moving and buffered casts. |
| High | Planted feet slide during moving casts/charges. | `moving_cast`: 69.3 world pixels between captured CAST samples at 0.3167–0.6500 s. `moving_charge`: 92.9 pixels while CHARGE/HOLD plays at 0.3167–1.1833 s. Same distances in all four directions. Player movement continues under a mostly stationary full-body pose. | Choose intentional movement for these actions and match the feet to it. Reducing frame speed alone cannot fix this. |
| High | Pressing M1 during a dash replaces the dash pose early. | `dash_buffer`: at 0.0767, 0.1100 and 0.1433 s, the trace says CHARGE while `dash=true`, in all four directions. `bladePreparing()` ignores dash state and takes priority over dash animation in `B2BJ.step()`. | Keep dash visuals until the physical dash ends; preserve the input buffer. |
| Medium | Dash makes the body blink out without taking damage. | Human `dash` captured cells 7, 8 and 10 have background/grid only. Slime dash also disappears. `Player.invulnerable()` includes dashing, and `drawPlayer()` applies hurt-style blinking to every invulnerable state. | Separate damage blinking from dash protection. Full-scene afterimages partly mask this; this diagnostic view intentionally omits trails. |
| Medium | Charged hold starts at an arbitrary pose. | `charge` first HOLD sample is frame 3 at 0.3833 s; `charge_late`, after only 0.3 s extra waiting, starts frame 5 at 0.6833 s. Reproduced in all directions. HOLD is sampled using `weatherTime`. | Use elapsed time since entering HOLD and match the preparation endpoint. |
| Medium | Dead human returns to breathing idle. | `death`: HURT returns to IDLE at 0.3500 s while `alive=false`, continuing through the recording. Animation still updates after gameplay has stopped. | Add an intentional terminal pose or freeze the appropriate frame. The real death overlay still appears; this is the body underneath it. |
| Medium | Transformation reveals an already-advanced idle. | All `transform` traces first show normal human IDLE frame 4 at 0.6833 s. Its clock advances behind the morph. The endpoint and idle differ in head/weapon silhouette. | Match the morph endpoint to the normal stance and initialize that stance deliberately. |

Code locations: [render/input flow](../../../src/B2BJ.java), [combat state](../../../src/RuinedOutpostGame.java), [animation timing](../../../src/BladeAnimation.java), [player movement/protection](../../../src/Player.java). Search the method names above; line numbers will change during repairs.

## Drawing and effect findings

- **Run remains the biggest visual problem.** Down/left hands stay bunched, up arms pump together, and right loses the intended bangs/mask design. These are drawings inside the correct direction rows, separate from the previously fixed idle-facing input bug.
- **Start, stop and turns expose the mismatches.** A running turn briefly uses a ready-pose shuffle while movement continues, then enters run frame 0. Stop uses the same authored entry regardless of current stride. Head and sword carry also change between these sheets.
- **The mask still blinks/emotes.** Idle, hurt, charge, combat and transformation drawings change its markings or mouth. A rigid mask must retain its shape throughout the motion.
- **Blue melee effects do render.** Cut, thrust, rise, finisher and charged release are distinguishable. However, several broad crescents float ahead of the body rather than following the drawn sword path. Up thrust is especially mismatched: the effect points up while the sword extends sideways. Align each effect with the corrected contact pose before adding more particles.
- **Combat choreography varies between directions.** Left finisher recovers earlier visually; left charged release turns frontward, right charged release resembles a thrust. These remain art issues even though the correct gameplay action executes.
- **Transformation endpoints need another pass.** Along with human head/weapon changes, the left-facing morph has a right-looking slime endpoint, then switches to the normal left-facing blob. Compare `transform-left` around the return to slime at 1.6500 s.

## Coverage and observations

19 controlled scenarios × 4 directions = **76 recordings / 3,616 captured frames**. The runner uses actual keyboard/mouse handlers, game updates and player/effect drawing. Visual review combines the complete source-sheet audit with recorded sequences and four-direction playback comparisons.

| Scenarios | Observation |
|---|---|
| Human idle → start → run → stop | Correct directional selection; rejected hand, mask and weapon drawings remain. |
| Running turns | Brief standing shuffle during travel; stride restarts. |
| Dash; M1 during dash | Body blinking and premature CHARGE pose reproduced. |
| Four short M1 clicks | All four distinct moves execute with blue effects. Short presses briefly enter preparation; that alone is not proof of a visible defect. |
| Charge/release; delayed charge | Charge, hold and cleave all execute; HOLD entry phase depends on world time. |
| Crescent with changed aim | Pose/projectile mismatch reproduced in all directions. |
| Moving charge; moving Crescent | Planted-pose sliding reproduced and measured. |
| Riposte guard | Shield effect and recovery render; existing pose/mask inconsistencies remain. No counter-hit encounter was simulated. |
| Human hurt; human death | Hurt plays; dead body returns to idle. Side hurt still resembles a forward dash. |
| Both transformations | Both events execute and settle into the correct form; endpoint art/clock joins need repair. |
| Slime movement/dash | Squash/stretch is more coherent than human locomotion; dash shares unwanted blinking. Side hop becomes a narrow upright silhouette, worth retaining in the next visual comparison. |
| Slime held combo | Directional cast and distinct finisher/projectiles render and recover. No missing action observed in these fixtures. |
| Slime Tide Wave | Wave and cast effects render toward the selected direction and recover. |
| Slime hurt | Damage splash and invulnerability blinking render. The trace names the underlying slime pose, not the separate impact effect. |
| Slime absorption | Stretch, engulf, recovery and heal effect execute. Initial blinking comes from controlled fixture damage used to create a healing deficit. |

## Evidence and reproduction

- Runner: [AuditPlayerPlayback.java](../../../tools/AuditPlayerPlayback.java).
- `runtime/<scenario>-<direction>.png`: packed actual renders, ten cells per row.
- Matching `.json`: time, phase, action, facing, frame, position, dash/alive/form state and Crescent direction.
- `runtime-data.js`: the same traces plus viewer labels; no API or paid service.
- Captured JAR SHA-256: `bbda0d028a82ca2aa85851ce0b173a123e6584405fd8c13e06e4895592543c49`.

Run from the project root with the current built JAR. Capture elsewhere when comparing a repair against this preserved evidence:

```bash
rtk proxy javac --release 17 -Xlint:all -cp build/B2BJ.jar -d /tmp/b2bj-playback-audit tools/AuditPlayerPlayback.java
rtk proxy java -Djava.awt.headless=true -cp build/B2BJ.jar:/tmp/b2bj-playback-audit AuditPlayerPlayback /tmp/b2bj-playback-recheck
```

An optional second argument captures one scenario, for example `transform` or `moving_cast`. No campaign save is loaded or written. The absorption fixture fails explicitly if the real interact action does not begin.

Recorded at 60 Hz update steps (short final steps at phase boundaries), sampled every other update. Rendering is a player-centered diagnostic crop; the world, menus, afterimage trails and other live actors are omitted. Projectiles may leave the crop. This is not a full combat encounter, enemy-animation audit, frame-pacing benchmark or acceptance of every possible input combination. It does not establish that contact VFX align on every enemy.

Validation: runner compiles with Java 17 `-Xlint:all`; all 76 scenarios execute; all trace times increase; both morph directions, absorption, and charge/hold/release states are present in all four directions. Browser playback, direction panels, scenario selection and scrubbing were checked. Gameplay/assets were unchanged, so the earlier 67-test result was not rerun or presented as new visual approval.

## Repair order

1. Correct cast aim, dash visual priority, damage-only blinking and local HOLD timing.
2. Lock one mask/head/outfit and sword carry, then replace the four runs and their transitions.
3. Resolve moving-action footwork, match attack contact poses to effects, and repair morph/death joins.
4. Repeat these recordings against the fixes, then review real combat with enemies and world effects.

No replacement batch was submitted and no fixes are claimed by this report.
