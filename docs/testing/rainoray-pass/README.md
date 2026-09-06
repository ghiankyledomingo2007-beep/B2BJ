# Rainoray render review

Reviewed 2026-09-07 with the final packed assets. These are deterministic, headless staged scenes—not a claim of a live playthrough. No game window was opened.

## Visual verdict

No blocking visual defect remains in the sampled final states. The review did find and correct actual rendering problems before this verdict:

- The engulf body used a different position curve from its target. It now covers the current corpse at peak spread, encloses the remains during landing, and returns to the stationary player before completion. Cancellation immediately restores the uneaten remains. Native pixels remain at 2×; no feeding beam is drawn.
- The map reused Rainoray's new portrait for the Warden. It now draws the Warden's own first sprite frame.
- An animated brazier initially rendered its whole sheet. It now selects one 64×64 frame, retaining the 128×128 footprint, ground anchor, collision base and depth order.
- The horizontal fissure looked like separate vertical slivers. The final render joins longitudinal strands and marks the full lane. The oversized shockwave warning rim was capped so it no longer obscures the arena.
- Combat can cancel the body-replacement morph, so attacks, hits, dashes and skills are not hidden inside transformation art. Missing human action art falls back to directional idle rather than an invisible player.

## Reviewed examples

| Area | Evidence | Finding |
| --- | --- | --- |
| Human silhouette and grounding | [South](human-south.png), [east](human-east.png), [north](human-north.png), [west](human-west.png), [behind fence](human-behind-fence.png), [in front](human-before-fence.png) | Mask readable; human roughly 1.5× the Blob's visible height. Shared native scale and correct prop occlusion. |
| Human actions | [Dash](action-dash.png), [slash](action-slash.png), [cast](action-cast.png), [hurt](action-hurt.png) | Consistent body scale and foot anchor; distinct authored poses rather than one legacy side attack. |
| Morphs | [Blob](transform-0.png), [liquid human](transform-1.png), [human](transform-2.png), [reversion](revert-1.png), [reformed Blob](revert-2.png) | Readable intermediate anatomy and stable position. |
| Engulf | [Leap](engulf-south-3.png), [landing](engulf-south-6.png), [spread](engulf-south-8.png), [reform](engulf-south-15.png), [side](engulf-east-11.png), [back](engulf-north-11.png) | Entire liquid body encloses the target. South has the widest splash; side uses a curl around the remains. |
| Human skills | [Crescent](crescent.png), [guard](riposte.png), [counter](riposte-counter.png) | Crescent remains curved, not a flying sword. Its native 1× art core fits the 44px damage diameter; actor art remains 2×. Guard and counter have distinct short-lived feedback. |
| Enemy and boss warnings | [Enemy intent](enemy-warnings.png), [charge](boss-charge-warning.png), [shockwave](boss-shockwave-warning.png), [fissure warning](boss-fissure-warning.png), [active fissure](boss-fissure-active.png) | Ground markings remain behind actors. Final warning silhouettes are readable and retain their physical extents. |
| Dressing and map | [Brazier pose 0](brazier-frame-0.png), [pose 5](brazier-frame-5.png), [fogged map](arena-map.png), [all rooms](arena-map-all.png) | Fire animates without changing the prop footprint. Boss marker no longer uses the player's portrait; room names remain readable. |

The remaining directional engulf screenshots show poses 3, 6, 8, 11, 14 and 15 plus before/after states for all four facings. The north source's gold mound is authored swallowed material, not an extra runtime corpse. Gold sparks during collection are separate Ichor pickup feedback.

## Reproduce

From the project root:

```bash
rtk proxy bash -c 'set -e; review_dir=$(mktemp -d /tmp/b2bj-preview.XXXXXX); javac --release 17 -Xlint:all -d "$review_dir" src/*.java tools/PreviewRainoray.java; java -Djava.awt.headless=true -cp "$review_dir" PreviewRainoray; java -Djava.awt.headless=true -cp "$review_dir" PreviewRainoray --engulf-only; java -Djava.awt.headless=true -cp "$review_dir" PreviewRainoray --idle-only'
```

`RainorayRenderingTest` covers native canvas/timing, morph interruptions, engulf placement and cancellation, missing-art fallback, brazier footprint/frame/depth and the Warden marker. `RainorayArtTest --runtime-only` checks the 16th engulf pose and Crescent core scale. These checks were run with assertions enabled and passed; the main verification report records the complete build result.

Limits: staged images do not establish live input feel, audio-device behavior, frame-rate stability or overall boss balance. Generated motion still merits human playtesting; this report does not call the entire biome finished or bug-free.
