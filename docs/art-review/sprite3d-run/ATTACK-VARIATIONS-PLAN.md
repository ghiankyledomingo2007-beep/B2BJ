# Attack variations — plan (2026-09-22)

Owner asked: "now plan some variation attacks". **Built 2026-09-22: 1 counter cut, 2 dash slash (wide,
pass-through, by the owner's choice), 3 full-charge spin cleave.** Still only planned: 4 Edge finisher,
5 back-step cut (owner: not now), 6 A/B light cuts. How they were wired is in the vault note.

## Ground rules

- **No new buttons.** Every variation hangs on an input or a moment the game already has.
- **Reuse hooks the combat code already has**: the dash opener (`dashOpener`), the one combo-preserving
  dodge (`comboDodged`), the riposte counter event (`RIPOSTE_COUNTER`), full charge (`chargeReady`), the
  Edge upgrade (`bladeFinisherBonus`), movement intent at click time.
- **Cheap to make.** A variation is one pose track (~20 lines in `tools/sprite3d/attacks.py`). The builder
  already photographs four facings, fits the cell, and makes the blade trail for free.
- **Cheap to wire.** The game indexes the slash sheet as `row = facing + 4 x move`. A variation is just
  another move number: more blocks on the same sheet, the same draw code, the same trail code. Only
  `strikeWindup/Duration/Step`, the choice of move, and the sheet-size test change.
- **Same stats first.** Each variation ships with the damage, reach and timing of the move it replaces, so
  it cannot unbalance the fight. Tuning is a separate, later decision.

## The variations, in the order I would build them

| # | Variation | When it happens | What he does | Game code | Risk |
|---|-----------|-----------------|--------------|-----------|------|
| 1 | **Counter cut** | A riposte (F) catches a hit | Today the parry only flashes an effect and the enemies take damage from nowhere. He answers with a fast rising cut out of the guard pose. | Play it on `RIPOSTE_COUNTER`; damage already exists | Low |
| 2 | **Dash slash** | Click right after a dash with no combo running (the game already turns this into "start at the thrust") | A low sliding cut that keeps the dash's momentum, blade dragged through, instead of a standing lunge | Pick the new move where `dashOpener>0`; keep the thrust's stats | Low |
| 3 | **Full-charge cleave** | Release the hold after the charge-ready cue (0.8 s) | The normal cleave is one draw cut. Fully charged he spins: a second, wider pass with a much bigger trail. Partial charge keeps today's cleave. | Pick by `releasedCharge >= 1`; damage already scales with charge | Low |
| 4 | **Edge finisher** | 4th click, only when the Edge upgrade is owned | Two crossing diagonals (an X cut) instead of the overhead slam, so the upgrade is something you can see, not just +2 damage | Pick by `bladeFinisherBonus()>0` | Low |
| 5 | **Back-step cut** | Click while holding the direction AWAY from your aim | He hops back as he cuts: a defensive opener. Replaces move 0 only. | Needs movement intent at click time and a negative attack step (collision-safe step already exists) | Medium |
| 6 | **A/B light cuts** | Every other combo | Second animation for the draw cut and the diagonal so long fights repeat less. Cosmetic only. | One counter | Low |

## Not recommended

- **Delay combos** (click, click, pause, click for a different finisher). Hidden timing with no UI to teach it;
  it also fights the input buffer that was just made reliable.
- **Charged finisher** (hold on the 4th click). Holding already means "cleave"; two meanings for one hold
  inside a combo is how inputs start misfiring.
- **Jump or air attacks.** There is no jump.
- **Attacks while running that do not plant.** The 2026-09-16 repair deliberately planted every strike to stop
  feet sliding; the dash slash (2) gives the moving attack without reopening that.

## What each one costs

Art, per variation: one pose track, rebuild (~20 s), read the builder's report, look at the GIF. The trail
comes with it.

Code, once, shared by all: let `bladeMove` run past 4; slash-sheet block = `move<4 ? move : move-1`
(move 4 is the cleave, on its own sheet); generalise the "modern sheet has 16 rows" check in
`B2BJ.drawBlade` and the 12x16 assertions in `RainorayArtTest` / `BladeTrailTest` to `4 x moves` rows;
add the new timings.

Code, per variation: the one condition that selects it, plus a test that the right move is chosen and that the
old move is still chosen otherwise.

## Open questions for the owner

1. Order: 1 then 2 first? They are the two moments that currently have no animation of their own.
2. Dash slash: keep the thrust's narrow single-target hit, or make it a wide pass-through cut? (Wide is a
   balance change.)
3. Back-step cut (5): wanted at all? It is the only one that changes how the character moves.
