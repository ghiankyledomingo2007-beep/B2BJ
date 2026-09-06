# Rainoray Rework Art Review

Read-only visual review of `docs/art-review/rainoray-rework/*/contact.png`.

## Reference

- `mask-polish/contact.png`: keep teal cloak, dark hood, pale half-mask, low sword silhouette.
- Heavy finisher rows may keep small baked gold/ichor sword arcs when pose, facing, and frame stability hold.
- Do not pack weak rows just to fill eight frames.

## Accepted

- `human-idle-south`: `0,1,2,3,4,5,6,7`
  - Consistent south, clean mask, mild breathing. Skip `8`; loop/recovery frame.
- `human-idle-east`: `0,1,2,3,4,5,6,7`
  - Consistent side read, clean silhouette. Skip `8`; loop/recovery frame.
- `human-run-east`: `0,1,2,3,4,5,6,7`
  - Best east motion cycle. Skip `8`; sword pose pops upward.
- `human-run-north-retry`: `0,1,2,3,4,5,6,7`
  - Stays north, natural leg cycle, cloak motion clean. Skip `8`; softer loop frame.
- `human-run-south-retry`: `0,1,2,3,4,5,6,7`
  - Clean south run, subtle gait, no unwanted rotation. Skip `8`; loop reset.
- `human-dash-east`: `0,1,2,3,4,5,6,7`
  - Clear anticipation into forward lean/contact/recovery. Skip `8`; settles back to idle.
- `human-dash-north-retry`: `0,1,2,3,4,5,6,7`
  - Mostly stable north dash with cloak momentum. Frame `5` exposes side/body, but still reads north in motion.
- `human-dash-south-retry`: `0,1,2,3,4,5,6,7`
  - Good south/front dash; three-quarter lean feels like motion rather than facing drift.
- `human-slash-b-east`: `0,1,2,3,4,5,6,7`
  - Best normal east slash: anticipation/contact/recovery hold together. Skip `8`; turns away too much.
- `human-slash-a-south-retry`: `0,1,2,3,4,5,6,7`
  - Stable south-facing normal slash. Contact is soft, so separate hit effect should sell impact.
- `human-slash-a-east-polish`: `0,1,2,3,4,5,6,7`
  - Cleaner than first east A row; stable side facing, readable contact and recovery.
- `human-slash-b-south-polish`: `0,1,2,3,4,5,6,7`
  - Stable south-facing compact slash. Restrained motion, but no direction drift.
- `human-slash-b-north-polish`: `0,1,2,3,4,5,6,7`
  - Stable north-facing slash/raised-step. Middle frames lean guard-like, but usable in motion.
- `human-cast-south`: `0,1,2,3,4,5,6,7`
  - Strong cast release, stable south facing, gold Ichor burst fits theme. Skip `8`; full return to idle.
- `human-cast-east`: `0,1,2,3,4,5,6,7`
  - Consistent east cast, readable special-skill release. Frame `3` flash is large but acceptable. Skip `8`; idle reset.
- `human-cast-north`: `0,1,2,3,4,5,6,7`
  - Stable north cast with release/recovery particles. Skip `8`; idle reset.
- `human-guard-east`: `0,1,2,3,4,5,6,7`
  - Stable east stance, clean body. Sword sits low, so shield/riposte VFX must carry block read. Skip `8`; idle reset.
- `human-guard-north`: `0,1,2,3,4,5,6,7`
  - Stable north ready/guard stance. Skip `8`; idle reset.
- `guard-polish`: `0,1,2,3,4,5,6,7`
  - Open-center guard overlay after spark burst; good for parry window.
- `counter-polish`: `0,1,2,3,4,5,6,7`
  - Fine fragment burst and clean decay; good counter impact overlay.
- `ring-polish`: `0,1,2,3,4,5,6,7`
  - Clean perimeter-node warning ring. No distracting interior symbol.
- `chevron-polish`: `0,1,2,3,4,5,6,7`
  - Fixed right-facing head, readable as lane direction marker.
- `brazier-animated`: `0,1,2,3,4,5,6,7`
  - Flame-only loop; brazier base stays stable.
- `human-hurt-south`: `0,1,2,3,4,5,6,7`
  - Subtle flinch/recover, but body and facing stay stable. Skip `8`; idle reset.
- `human-hurt-east`: `0,1,2,3,4,5,6,7`
  - Clean compact recoil and recovery, consistent east facing. Skip `8`; idle reset.
- `human-hurt-north`: `0,1,2,3,4,5,6,7`
  - North read holds, recoil readable enough. Skip `8`; idle reset.
- `transform-in-south`: `0,1,2,3,4,5,6,7,8,9,10,11,12,13,15,16`
  - Good transient slime-to-human morph. Skip `14`; detached ground spark.
- `transform-in-east`: `0,1,2,3,4,5,6,7,8,9,10,11,12,13,15,16`
  - Strong side morph into Rainoray. Skip `14`; detached spark pulls focus.
- `transform-in-north`: `0,1,2,3,4,5,6,7,8,9,10,11,12,13,15,16`
  - Usable transient morph. Frames `9-10` reveal front-facing body before north settles, acceptable during transformation.
- `transform-out-south`: `0,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16`
  - Good human-to-slime dissolve. Skips duplicate-ish early frame `1`; keeps endpoints.
- `transform-out-east`: `0,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16`
  - Coherent side dissolve. Frame `11` has broken head/fragment transition, acceptable as transient.
- `transform-out-north`: `0,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16`
  - Good north dissolve. Frame `14` has minor top speckle noise, acceptable in motion.
- `engulf-south`: `0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15`
  - Clear slime squash/splash absorption. Preserve authored airborne/floor positions; renderer owns body/corpse alignment.
- `engulf-north`: `0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15`
  - Strong swallow/consume read with visible corpse mass. Preserve authored vertical positions.
- `engulf-west`: `0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15`
  - Better west/source row than polish: tall phase resolves sooner and return is natural. Use canonical west and flip for east.

## Conditional

- `human-slash-a-east`: `0,1,2,3,4,5,6,7`
  - Facing and body are stable, but contact arc is muddy. Use only as lighter secondary east slash if needed.
- `human-slash-c-south`: `0,1,2,3,4,5,6,7`
  - Acceptable as heavy finisher, not generic base slash. Baked gold arc works with Ichor theme. Skip `8`; return frame.
- `human-slash-c-east`: `0,1,2,3,4,5,6,7`
  - Stable east pose, good charged-skill read. Motion is more charged release than cutting slash. Use as east heavy/cast finisher, not base slash.
- `human-slash-c-north`: `0,1,2,3,4,5,6,7`
  - Stable north-facing raise/release. Reads more windup/finisher than slash; needs gameplay impact/VFX to sell hit.

## Rejected

- `human-idle-north`
  - Only frames `0,1,2,8` stay north. Frames `3-7` rotate side/right.
- `human-run-south`
  - Not a run cycle; becomes slash/stance rotation.
- `human-run-north`
  - Frames `4-7` turn right/front.
- `human-dash-south`
  - Turns side after frame `3`; reads as sword flourish, not south dash.
- `human-dash-north`
  - Frames `4-7` become side/three-quarter; not enough clean north dash frames.
- `human-slash-a-south`
  - Weak slash; sword jumps pose-to-pose without decisive contact arc.
- `human-slash-a-north`
  - Rotates north to east/front.
- `human-slash-b-south`
  - Turns east by mid-animation; bad south attack read.
- `human-slash-b-north`
  - Direction mostly okay, but action reads like step/settle, not attack.
- `human-guard-south`
  - Drifts into side-facing middle poses; not stable enough for strict south guard.
- `engulf-west-polish`
  - Reject final polish. Neck stays tall too late and return is delayed; old `engulf-west` is better.
- `ring-runes`
  - Reject. Pentagram-like interior dominates and changes combat telegraph read.
- `chevron-rune`
  - Reject. Downward mark is not a readable directional lane warning.

## Missing

- `engulf-east/contact.png`
  - Not present during this pass; cannot review.
