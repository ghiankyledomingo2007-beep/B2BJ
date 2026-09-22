# Human motion verification — 2026-09-16

- `./build.sh`: all67 assertion programs pass, Java17 lint compilation and packaged
  asset loading pass. The rebuilt JAR is `build/B2BJ.jar`.
- `WindowLaunchSmoke --borderless`: production launcher passed; updated JAR opened successfully.
- `LiveCampaignSmoke`: actual desktop keyboard/mouse test passed against the JAR,
  using an isolated temporary save. Enter starts; D moves; Blob M1 fires; Q transforms;
  four separate short M1 clicks advance steps0–3; a900ms hold charges without attacking;
  release triggers one cleave. Options, pause, title and Continue also pass.
- Native focus needed `xdotool search --onlyvisible`; selecting an unmapped Java
  window caused the first two focus attempts to stop safely before sending input.
- `BladeCombatTest`: one buffered click, combo wrap/timeout, committed aim, charge
  cost/cancellation, hurt, focus, form loss, collision-safe footwork, single-target
  thrust, contact-frame timing, visible miss effects and native2× pixel effects.
- `PreviewHumanCombat`: actual panel input/update/render captures in `final/` show
  all four directions, runs, combo contacts, charge and release. These use a fixed
  combat fixture, not an unassisted campaign playthrough.

Review `final/run-review.png`, `final/combo-review.png`, `final/charge-review.png`
and four `final/run-*.gif` loops. `native/` contains captures from the physical-input
smoke test. Earlier prototype folders document staging; final/ is current.

Full mask, sword shapes and facing were visually reviewed before selecting source
frames. Generation receipts and exact packing manifest live in
`../../art-review/human-motion-fix/`. Broader difficulty tuning still needs human play.

## 2026-09-16 idle facing correction

A parked mouse was overriding facing every idle frame. Idle now turns only on fresh mouse movement; stopping preserves the last movement direction. Attacks still aim at the cursor. `BladeCombatTest` reproduces the old failure and checks all four stop/idle directions, fresh mouse aiming and attack aiming. All 67 test programs pass. [Native down-facing idle with cursor parked right](native/human-idle-down.png) verifies the reported case. No asset generation required.
