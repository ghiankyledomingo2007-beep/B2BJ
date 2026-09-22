> **2026-09-16 correction:** Owner rejected hand poses, mask changes and movement continuity. Earlier visual acceptance is withdrawn. See [complete movement audit](../human-motion-audit/README.md). Runtime frames remain installed pending replacement; passing code checks does not approve artwork.

# Masked human motion — 2026-09-16

This pass keeps Rimuru's original ivory mask on the human character. The owner's
three reference images are in `references/`. The mask has dark curved markings
and a red center dot; blue hair, fur collar, blue coat and katana stay consistent.

## Playing the character

Move with WASD. Moving faces travel; standing follows the mouse. Four separate
short left clicks produce a diagonal cut, straight thrust, rising cut and overhead
finisher. Each step has its own anticipation and recovery. A late click can buffer
one next move; extra clicks never build a long queue.

Hold left click beyond 0.18 seconds to charge. Full power arrives at 0.8 seconds.
Release spends 10 Ichor for one stronger blue cleave. Holding never repeats normal
attacks. Space cancels charge; hurt, pause, focus loss and form loss clear stored input.
A recovery dash preserves one unfinished combo; a fresh dash can open with a thrust.
RMB retains Ichor Crescent, and F retains Riposte.

## What the code does

- `RuinedOutpostGame`: input timing, combo/charge state, contact time, one damage
  application, target selection and interruptions.
- `Player`: movement, resource costs and short collision-safe attack steps.
- `BladeAnimation`: directional row, animation state and playback time.
- `B2BJ`: forwards mouse events, draws sprites and blue contact effects, shows
  four combo marks and charge feedback.

There is no animation service running in the game. PixelLab supplied development
images; the finished game reads local PNG sheets and works offline. Hair and coat
motion are authored into frames, not simulated with a separate physics engine.

## Source selection

Native cells are 80×80; game rendering is 2× nearest-neighbor. Direction rows are
south, east, north, west. Run uses a 12-pose stride at 20fps. Start/stop/turn use four
poses; idle and supporting actions use eight. Combo sheets group four direction
rows per move, with 12 columns. Column4 is the contact pose and matches the damage
clock. Transformations use16 poses; entrance reverses the reviewed reversion.

`selection.json` is the exact frame manifest. Repack offline with
`python3 tools/pack-human-motion.py`. Requests and sanitized job receipts remain
beside the source frames. No API credentials belong in this repository.

Rejected studies include the detached-hair sprint, upright template run, unmasked
portrait edits, bent/duplicate sword frames, the first west cut with painted trails,
and the first north cleave that exposed a human face. Explicit corrected sword
key poses gave clearer thrust and overhead movement. Frame selection excludes
poor recovery poses rather than installing every generated image.

Some rotation services labelled a right-facing reference as south. The manifest
uses visually checked facing, not those API labels. West has its own authored row;
the renderer does not mirror the entire right-facing sheet.

## Verification

All67 test programs, Java17 lint compilation and packaged assets pass. Actual
keyboard/mouse combo and charge-release smoke passed against the JAR with an
isolated save. Four-direction input-driven captures are in
[the verification report](../../testing/human-motion/README.md).

15 sheets containing688 frame cells and the masked portrait are installed. Some
attack rows intentionally begin and end on the same ready pose. No new runtime
dependency or source-class hierarchy was introduced.

Final live balance:13 generations remain (0+0+0+2+0+10+1) across seven allocations.
The richer masked implementation used189 additional generations from the202 available
after the final three allocations were supplied. That includes pose studies,
rejected attempts and replacing supporting actions to keep the mask on. The service
reports268 used, including its first allocation's historical one-generation overrun;
`budget.json` retains the exact live readings.
No further generation jobs are pending.
