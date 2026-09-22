# Human motion audit — 2026-09-16

**Result: artwork rejected for consistency.** This supersedes the previous visual acceptance. Existing runtime images are retained as evidence; this audit does not install replacements.

Reviewed all 15 runtime sheets: 688 frame cells, covering 18 actions × 4 directions. Frame numbers below are zero-based. Transform-in reuses reversed transform-out art; the 688 cells are not all unique drawings. Sources and SHA-256 hashes: [source-inventory.json](source-inventory.json).

Open [interactive review](review.html) to play the movement chain, compare four directions, slow playback and scrub individual poses. All images are embedded; no server or API is required.

**Second pass:** [actual game playback review](runtime-review.html) and [recheck findings](RECHECK.md) cover 76 input-driven scenarios across human/slime forms, including transitions, moving casts, dash buffering and death. Keep `runtime-data.js` and `runtime/` beside this viewer.

## Confirmed problems

- Down and left run poses keep hands raised together instead of showing a free-arm running swing. Sword carry differs from idle.
- Up run moves both arms down/out together rather than a clear opposite arm/leg rhythm.
- Right run has more leg motion, but its mask is a bare oval and loses the idle bangs.
- Idle mask drawings change around frames 3–4 like blinking. A solid mask must not change expression.
- Start/stop transition between two incompatible head designs and sword carries.
- Several combat actions also change mask/weapon shape. Passing Java tests does not establish art quality.

## Complete action review

| Action | Finding |
|---|---|
| Idle | Mask changes expression: down frames 3–4 compress the eye markings and widen the mouth; right frames 3–4 look like a blink. Left face also shifts. Mask must be a rigid object. |
| Run | DOWN: hands stay together at chest all 12 frames; sword reads as sheathed. LEFT: fixed raised hands all 12 frames. UP: both arms drop/spread together around frames 3–9. RIGHT: better leg stride, but exposed oval mask and missing fringe differ from idle. All four rows need correction. |
| Start moving | All directions change hair/head/sword design by frame 3 to match the rejected run. Right and left frame 2 contain competing blade-like shapes. Fix after replacing run; these transitions cannot remain. |
| Stop moving | Head/fringe and weapon jump from run design back to idle. Down frame 0 keeps clasped hands; left frame 0 keeps the folded-arm run. Replace alongside start/run. |
| Turn | Mostly a small ready-pose foot shuffle; does not bridge the running pose. Lower priority than run, but match the replacement stance and locked mask. |
| Dash | Right/left have readable forward lean. Down has compressed hop-like poses; up mostly pushes hair sideways. Keep as motion reference, review mask and joins before accepting. |
| Crescent cast | Free hand extends and sword stays held. Down hand points sideways; up hand points sideways too. Check direction-specific cast silhouette. Up frame 4 has art touching a cell edge. |
| Riposte guard | Guard idea is readable. Right rotates toward camera; left lifts sword alongside head. Keep two-handed grip here only where intentional; mask and recovery still need a consistency pass. |
| Hit reaction | Right poses resemble a forward dash more than recoil. Down/left around frame 5 change the mouth into an open expression. North turns sword forward. Rework recoil and keep rigid mask. |
| Charge preparation | Mask/hair design changes through preparation. Down final pose rotates to three-quarter; left late frames expose the smooth oval mask. Down frame 2 reaches a cell edge. |
| Charged hold | Down sword oscillates from left to right within the hold loop; should mostly hold tension. Left frame 3 flares hair sharply. Mask and head differ from normal idle/run. |
| Combo 1 — cut | Cut has windup/follow-through, but right turns frontward and left resembles an overhead cut. Keep direction-specific contact consistent. Up frame 6 touches a cell edge; standardize mask. |
| Combo 2 — thrust | Right/left thrusts read clearly. Up contact extends sideways instead of strongly up; needs a clearer foreshortened thrust. Right recovery frame 5 touches a cell edge. Down mask changes during recovery. |
| Combo 3 — rising cut | Rising sword path is visible, but direction rows have different body turns/kicks. Down frame 1 touches a cell edge. Review weapon continuity and locked mask before accepting. |
| Combo 4 — finisher | Down/right show overhead-to-low contact; left has a much less decisive finish and jumps to ready sooner. Mask/head still change with pose. Keep distinct finisher silhouette in all directions. |
| Charged release | Left contact turns almost frontward and down frames 9–10 open the mask mouth. Right release reads more like a thrust. Standardize the intended broad cleave and rigid mask. |
| Slime to human | Reverse of transform-out. Directional morphs exist, but inherit changing human mask and unrelated facial expressions. Check mask becoming solid and the join to idle after canonical head is fixed. |
| Human to slime | Down human frames 3–7 change facial markings before liquefying. Side morphs alter face/head during transition. The mask can dissolve with the body, but should not emote while still solid. |

## Mask target

The owner reference has an ivory mask, fixed dark curved/flame-like markings, a centered red dot, a fixed curved lower line, and blue bangs overlapping the top. Preserve the same geometry while translating/rotating with the head; no blinking, moving pupils or opening mouth. Some detail must simplify at 80×80, but it must simplify consistently.

## Replacement order

1. Establish one consistent masked head and outfit in four directions. Reject exposed oval heads and facial expressions.
2. Rebuild the four runs from those bases: hands visibly separate, sword grip/carry continuous, free arm counter-swings with the opposite leg, readable contact/pass/flight poses, restrained head movement.
3. Rebuild start/stop around the new run endpoints; check run wrap and movement-to-idle at normal speed.
4. Correct idle mask movement; then dash/turn/hurt and all combat poses against the same head and sword design.
5. Review frame-by-frame and at game scale before installing. Keep gameplay regression checks, but do not call them visual approval.

Do not spend another full animation batch before the new run and mask are visually acceptable. No generation credits used for this audit.

## Evidence

[All run frames](run.png) · [Run heads](run-heads.png) · [Idle heads](idle-heads.png) · [Start](start.png) · [Stop](stop.png)

A pixel-edge scan flagged five cells for inspection: cast/up/4, charge/down/2, cut/up/6, thrust/right/5, rising cut/down/1. Edge contact is a review flag, not by itself proof of clipping.

The earlier idle-facing code bug is separate: its fixed input logic selects the expected row. This audit concerns the drawings inside those rows.
