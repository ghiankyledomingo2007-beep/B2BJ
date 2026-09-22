# Human animation and combat specification — 2026-09-16

Implementation authorized by owner. Mask references and expanded motion/combat scope
are confirmed. Current status and remaining art review: [PROGRESS.md](PROGRESS.md).
The specification below records the intended behavior; verification is recorded
separately so proposals are not mistaken for finished work.

Seven allocations of 40 generations were supplied (280 nominal total). Live balances
are recorded in `budget.json`; pending jobs may not yet be billed. Credentials stay
outside this repository. The earlier 80–120 estimate preceded rebuilding every
supporting action with the newly requested mask and extra weapon-pose corrections.

## Character baseline

Owner update: wear the original ivory Rimuru mask shown in `references/mask-1.png` through `mask-3.png`: dark curved markings, red center dot, pale blue bangs over the mask. Keep blue coat, fur collar and katana.
Keep body proportions, palette, sword length and ground anchor consistent.
Use clearly readable front, right profile, back and left profile views.

## Motion character

Calm, confident idle; light, athletic running; fast, decisive sword work. Body
movement establishes the action, then hair, scarf and coat follow slightly later.
Keep the face and sword readable at actual game size. Large effects are brief.

## Locomotion and transitions

| State | Required motion | Initial pose/timing target |
| --- | --- | --- |
| Idle | Gentle chest/shoulder breathing and soft hair-tip and coat settling; planted feet | 8 poses, slow loop; the mask stays fixed |
| Run start | Lower hips, lean into travel, push off the rear foot; free arm leads | 2–3 poses; movement responds immediately |
| Run loop | Forward torso lean, alternating foot contacts, compression, strong push-off, flight, raised passing knee and arm counter-swing | 12 distinct poses for one complete left/right stride; initially 18–22 fps, then tune against travel speed |
| Stop | Front foot plants, knees absorb weight, torso settles, hair/coat overshoot then settle | 3–4 poses; player position stops when input stops |
| Turn | Short planted pivot with shoulders leading and coat following | 2–3 poses when needed; must not delay steering or attacks |
| Dash | Compressed launch, stretched forward burst, trailing hair/coat and blue afterimages, controlled landing | Fit existing dash duration and immunity window |

Movement faces travel. While standing, face mouse aim. Attacks hold their committed
direction until recovery. Demonstrate front, right profile, back and left profile
through actual inputs. Do not rotate the whole sprite as a substitute for a new view.

Start, stop and pivot poses are interruptible visual transitions. They must never
add input delay or change movement/collision rules. Ground lift comes from the
authored flight poses; planted poses must not skate.

## Click-by-click M1 combo

M1 means left mouse button. The previous version repeated attacks while M1 was held and used a three-step
combo (four with Edge). The new input uses separate short clicks for each step.

Default chain has four visibly different moves:

| Step | Move | Role and motion |
| --- | --- | --- |
| 1 | Quick diagonal cut | Fast opener; hips coil then release through a small forward step |
| 2 | Straight thrust | Direct single-target attack; front knee bends, sword extends firmly along aim |
| 3 | Turning rising cut | Pivot into a broader close sweep, reversing body rotation; no airborne enemy/juggle system |
| 4 | Two-handed overhead finisher | Longer preparation, strong downward cut, brief heavier recovery and collision-safe pushback on susceptible enemies |

The fourth hit is available without buying Edge. Preserve the existing
Edge save value and its +2 finisher damage benefit, and update the camp description
from "fourth hit" to "stronger finisher". This preserves progression compatibility while making the four-step chain available immediately. Boss/armored stagger resistance remains meaningful.

### Input rules

- Each short M1 press/release requests exactly one normal move. Clicks one through
  four advance opener, thrust, rising cut and finisher. A deliberate hold selects
  the separate charged cleave below; it never repeats normal attacks.
- A subsequent click within the combo window continues the chain. Waiting past
  that window returns to the opener. After the finisher's distinct recovery, the
  next accepted click starts a fresh chain; there is no automatic restart.
- Start with a roughly 0.5-second continuation window after a move recovers and a
  roughly 0.12-second input buffer at the end of recovery; tune in the prototype.
- Buffer at most one intentional next short click near recovery. That click may
  execute once after the button is released; holding never adds repeated requests. Rapid
  extra clicks cannot skip moves, bypass recovery or build a backlog of attacks.
- Mouse aim is sampled for each new move and locked through its active portion.
  The player can redirect the next move without twisting an already committed hit.
- Link the end of each move into the next preparation. Return to idle or movement
  only when the chain stops. Use snappy early moves and a readable finisher pause.
- Dash can cancel recovery once the strike is over, not bypass its contact timing.
  Taking a real hurt reaction, changing form, death, opening a menu or losing focus
  clears queued attacks and resets the chain. No attack fires unexpectedly on resume.
- Keep existing Ichor costs and skill cooldowns. Each move checks targets at its
  authored contact moment; animation frames never apply repeated damage.

Target 8–12 purposeful poses per attack. The thrust must read differently from a
slash. Preparation and recovery get time to read; contact itself is fast. Keep the
blade rigid, constant-length and attached to the hand, with coordinated hips,
shoulder, elbow and wrist motion. Step timing may be tuned with the combo prototype;
the damage moment and animation must always agree.

## Hold-and-release M1: charged cleave

Differentiate by press duration, using these initial prototype targets:

| Input | Result |
| --- | --- |
| Release before roughly 0.18 seconds | One normal combo step |
| Hold beyond roughly 0.18 seconds | Enter a braced charge stance; do not also execute a normal hit |
| Release while charging | One heavy horizontal blue cleave, with strength based on charge |
| Reach roughly 0.8 seconds total hold | Full-charge visual pulse; power stops increasing, attack still waits for release |

Use button-down for immediate preparation feedback, without committing damage.
Short-click release starts the normal attack. Test the threshold with actual input
before fixing it: ordinary clicking must remain responsive and must not accidentally
become charging. One press/release must never produce both a combo hit and a cleave.

Charge poses: widen stance, lower hips and draw the sword back near the hip. The
free hand braces the grip. A restrained blue highlight builds along the blade;
hair/scarf lift slightly and settle into a short hold loop. Full charge gives one
brief pulse and a clear sound, not continuous flashing. No automatic release.

On release, rotate hips and shoulders into a wide horizontal cleave, then show a
heavier settling recovery. Use a thicker tapered blue arc and short afterimage.
The attack remains local melee; RMB Crescent keeps its travelling ranged role.
Damage and stagger scale to the capped charge, with normal enemy/boss resistance.
Keep the displayed reach honest; a larger glow must not imply an invisible range buff.

Permit slower repositioning and aiming while charging; lock the attack direction
when released. Dash or an actual hurt reaction cancels charge. Pause, focus loss,
form loss, death and scene changes clear it; releasing afterward cannot fire a
stored attack. Cancelled or invalid releases do not spend attack energy.

Keep charging free of extra resource drain beyond the normal human-form timer.
A valid release checks and spends a dedicated Ichor cost once; determine the cost
with damage/recovery tuning in the prototype. Reaching full charge does not grant
extra immunity or repeated damage. The cleave ends/resets the normal combo, so the
next short click starts the opener. Charge input cannot bypass another move's active
hit or recovery; begin it only when the player can legally prepare another attack.

Prototype the tap/hold distinction after the ordinary four-click chain works,
before generating a full set of directional charge poses.

## Move combinations

Keep combinations on existing buttons, with no new menu or long input sequences:

- **M1, M1, RMB:** replace the next basic move with Ichor Crescent, using the
  existing skill cost/cooldown. This ends the basic chain. A deliberate RMB request
  replaces a buffered basic attack at the next legal attack window.
- **Dash, M1:** a buffered M1 at dash recovery produces a lunging thrust opener,
  then continues into the normal chain. No extra invulnerability or wall bypass;
  dash movement ends before the attack can damage a target.
- **Successful F parry, M1:** let the existing counter resolve, then flow into a
  fresh M1 chain. Do not award a second automatic counter hit.

First implement and review the base chain. Add these follow-ups individually once
individual clicks, input buffering, finisher reset and interruptions are reliable.

## Blue slash layers

- A small blue-white blade highlight at release.
- A tapered cyan core and deeper blue outer arc following the sword sweep.
- A short transparent after-trail that breaks into a few fading motes.
- A small directional impact burst only when a hit actually connects.

The opening and rising cuts use distinct arcs, the thrust uses a narrow blue
streak along the blade, and the finisher gets a broader descending accent. Each
effect peaks at its move's damage moment and fades over roughly 0.12–0.18 seconds.
It remains visible on misses, while
impact sparks and existing hit stop require contact. Keep the effect's apparent
reach consistent with melee range; it must not resemble a travelling projectile.
Respect reduced-effects mode and foreground occlusion. Avoid persistent glow,
full-screen flashes or effects that hide enemies.

## Supporting actions

- Guard: lower stance, firm sword placement, cloth settling; counter adds a brief
  blue flash and decisive return cut on a successful parry.
- Hurt: readable torso recoil and knee compression, then regain balance without
  visually sliding away from the actual collision body.
- Transformation: slime compresses and stretches into the human silhouette; hair
  and coat resolve and settle. Reversion collapses into the slime with a small ripple.
- All actions keep the same scale, palette, sword and foot anchor, and return
  cleanly to movement or idle. Preserve combat interruption of transformations.

## Work order and acceptance

1. Preserve the owner-approved masked baseline and reviewed poses.
2. Inspect the already submitted right-facing sprint pose. Choose the silhouette
   before commissioning a loop.
3. Build one side-view start/run/stop sequence and review at actual game size and
   speed. It must visibly sprint, keep a stable sword grip, show two distinct foot
   contacts per complete stride, and loop without a pop or idle pause.
4. Fix stationary aiming and demonstrate all four directions through real inputs.
5. Prototype one cut with the layered blue arc; compare a hit and a miss and prove
   contact timing in-game. The cut must read clearly with its effect disabled too.
6. Build the four-move M1 chain in one direction. Review successive clicks, rapid
   clicks, aim changes between moves, finisher recovery and interruption before
   ordering all directional combat art. Then prototype tap-versus-hold, partial/full
   charge, release, cancellation and one-action-per-press before ordering charge art.
7. Extend approved motion to other directions and add skill follow-ups individually.
   Review transitions and supporting actions in small groups.
8. Verify direction changes, moving/stopping, buffering, dash interruption,
   pause/focus/resume, form changes, clipping, one-hit-per-move behavior, Ichor costs,
   skill cooldowns, charged attack resource checks/cancellation, Edge upgrade/save
   compatibility and packaged assets. Rebuild and launch.

Extend the existing sprite/frame system and combo flow; no separate combat engine,
skeletal system or cloth physics. Do not order another complete animation batch
before the side-view sprint and single attack have been reviewed in-game.
