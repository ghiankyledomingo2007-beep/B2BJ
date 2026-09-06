# Ruined Outpost — GCD alignment and remaining issues

Updated: 2026-09-06.

Later owner-approved changes and current verification are recorded in the
[2026-09-07 immersion report](testing/immersion-40/README.md): ranged Spitter,
magnetic/overflow-preserving Ichor, corrected hit/facing effects and larger Warden.
The historical status and generation counts below describe the earlier audit pass.

## Authority and correction

Source: **Blob to Blade Game Concept Document v1.0**, dated August 5, 2026:
`/home/ghiankylledomingo/Downloads/01-School/Game-Dev/Blob-to-Blade-Game-Concept-Document-1.pdf`.

The GCD was read in full. It defines Ruined Outpost as abandoned military
defences at the entrance to the kingdom. It calls for roughly 12–18 authored
rooms per zone, including optional exploration; it does not require enclosed
one-screen rooms. The owner explicitly prefers an open-outpost feel.

The earlier version of this audit incorrectly elevated old Godot shrine notes
over the GCD. That interpretation is withdrawn. Six-room shrine names, a mask
altar narrative, three 34-Ichor Wisps, and a 12-health boss are not this biome's
design authority. The Java project remains the implementation; no engine
migration or restoration of deleted Godot work is intended.

## Implemented and checked

- Twelve connected sections: ten critical-path areas and two optional branches.
  Each is larger than the 1280×720 viewport, with scrolling camera, outdoor ground,
  uneven earth banks and broad road breaches. No four-sided brick box around
  every section. Names/topology are proposals, not quotations from the GCD.
- Walk-through transitions after room clear; visible wooden gates have matching
  collision while encounters are active. All exits are graph-connected and
  reachable with the player's actual foot radius.
- Fixed the reported actor-on-top-of-barricade rendering defect: standing props,
  actors and dash afterimages sort by ground contact. Long passage gates sort
  in local segments. Blob and Blade front/rear occlusion have render regressions.
  Four reviewed PixelLab prop types replace selected procedural blockouts, at
  native 2× scale with calibrated ground footprints, including the small flag base.
- Subsequent full-allowance batch adds native-scale barrels/trees, an eight-pose
  Blob idle, dissipating teal hits and clearer Warden arm anticipation. All 27 jobs
  completed; 155 PNG results retained. Clipped/wrong-facing attacks and wrong-camera
  props are excluded, and the oversized helmet was removed after in-scene review.
- Safe awakening, ten basic enemies before the first transformation yard,
  a safe Q tutorial, and a Captain Camp checkpoint. Optional field dressing
  and signal-tower story beat. Rainoray's identity replaces the invented mask call.
- Basic drops: 10 Ichor. Boss summons: 15. Capacity: 100.
  Full-meter transformation; 12-second base drain; pickups extend Blade form.
  Blade damage 4×; movement 0.8×; received damage 0.75× plus five Ichor.
  Reversion recovery: 1.5 seconds, without attack/dash/movement.
- Dash invulnerability, bounded collision substeps, one hit per lunge/dash,
  delayed attack impact and obstacle line-of-sight checks.
- Warden locks its target before impact; damage does not cancel its attack.
  Alternates targeted ground strikes with a radial shockwave.
  Recovery exposes it; periodic summons replenish Ichor.
- Mouse aiming, held-click attack sequence, pause, non-pausing map, focus-loss
  reset, correct overlapping arrow/WASD inputs, mute and reduced effects.
- GCD-positioned pixel HUD, integer-scaled characters and bitmap font,
  gold Ichor, red danger tells, health bars, hit-stop, afterimages,
  local impact feedback and distinct Blob/Blade synthesized attack cues.
- Enemy silhouettes: corrupted Slime palette variant for ordinary outpost
  encounters. Existing Wisp art is restricted to Warden summons.
  Existing source sheets are preserved; this is a runtime art choice, not
  a claim that the owner approved new creature canon.

## Remaining issues — do not call the biome finished

Owner-requested change after the original GCD audit: Blob's jelly punch is now
replaced by travelling cyan Water Slash and a cooldown-limited Tide Wave. Both use
reviewed PixelLab effects, collision/drag/knockback and native 2x rendering. Basic
water does not stun-lock enemies; heavy water interrupts. This is a deliberate
design revision, not text from the GCD. See [water pass](art-review/water-slash/README.md).

| Priority | Issue | Evidence / next check |
| --- | --- | --- |
| High | Boss pacing not validated against the GCD's 3–5 transformation target | Health is provisionally 240 with recovery windows. Unit checks reject a three-hit boss, but a complete human fight must verify cycles, downtime and resource recovery. |
| High | Final player/enemy animation quality remains unfinished | Blob idle and Warden anticipation improved. Blade south/north cuts are integrated at native scale; side facing is corrected but legacy side motion/foot alignment remains. New side studies failed review. Audit source frame boundaries and stray pixels before more generation. Three attack timings are not three finished character cycles. |
| High | Outdoor art is only partly through its art pass | Reviewed palisades, cots, standards and low walls are integrated. Banks, tall/long blockout obstacles, ground dressing and other props still need authored art; richer destruction and environmental storytelling remain. |
| Medium | No disk save for checkpoints/unlocks | Death recovery retains current-session state only. GCD persistent upgrades/unlocks remain future work. |
| Medium | No layered music, controller support or remappable controls | Synthesized SFX and fixed keyboard/mouse controls are implemented. |
| Medium | Sections change discretely | Roads connect automatically, but this is not one seamless streamed world. Validate whether camera cuts are acceptable during live play. |
| Medium | Small curated enemy roster | Ordinary encounters reuse the corrupted-Slime silhouette/AI; Wisp is a summon. GCD later-biome enemies are deliberately not dumped into this zone. |
| Medium | Only the first-biome boundary exists | Forest exit ends this prototype. No forest gameplay, later bosses, upgrades, secrets economy or final endings are implemented. |
| Verification | Full unassisted playthrough outstanding | Route tests use actor-position fixtures and a nearly defeated boss to verify the final strike and exit flow. They do not certify difficulty or a complete real-time playthrough. |
| Verification | Audio is synthesized/tested, not perceptually mastered | Waveform and failure-safety checks pass; listening tests and music mix remain. Windows installer not executed locally. |

## Asset and spending boundaries

Only reviewed candidates should become runtime assets. Matching dimensions,
palette or transparency is necessary, not visual approval. Old staged/concept
files are preserved; unused shrine props are not loaded by the game.

Current PixelLab decisions and exact job IDs live in
[animation plan](ruined-outpost-animation-plan.md). The latest pass used 28 of a
fresh 40-generation allowance, including a rejected 20-generation Pro edit batch.
The subsequent water-combat pass used the remaining 12; the provider now reports
0 remaining / 40 used. Generation is paused. No purchased credit was charged.
The owner explicitly removed the PixelLab spending cap: generate what the game
needs, without treating the provider's 40-generation token allowance as a project
budget. Provider limits remain unchanged; credentials are not stored in the project.
Visual review still gates integration.
