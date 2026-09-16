# Human form rebuild — 2026-09-16

Integrated the owner's requested Rimuru Tempest reference: pale blue hair, golden
eyes, visible face, blue split coat, beige fur collar and katana. Reference:
[official anime artwork](https://www.ten-sura.com/character/rimuru).
PixelLab generated the sprites; no generation service or key is used at runtime.

## What ships

| Motion | Reviewed source | Runtime layout |
| --- | --- | --- |
| Idle | `idle-{south,east,north}` | 8 poses × 3 directions, 6 fps |
| Run | `run-south-v2`, `run-east-v2`, `run-north-v3` | 16 poses × 3 directions, 28 fps |
| Dash, cast, guard, hurt | Corresponding direction folders | 8 poses × 3 directions |
| Three-hit combo | Selected cuts plus separate recovery clips | 8 poses × 9 rows |
| Transform/revert | South/east morphs; north exit forward/backward | 16 poses × 3 directions |
| Portrait | `portrait/download.png` | Native 64×64 |

Body cells remain native 80×80, drawn at 2×. West mirrors east. The new body is
taller than the former masked design, with feet anchored around row 72. No frames
are rescaled or painted over. One-pixel translations align initial references;
packing preserves authored running lift rather than pinning each pose to the floor.

Run duration stays 16/28 seconds, matching the old 8/14-second loop. Combat keeps
its existing damage and recovery rules. Each cut has three anticipation poses,
contact in column 3 at `strikeWindup(combo)`, then four recovery poses. North's
first and third cuts share a reviewed strike/recovery with different anticipation;
the second uses the upward follow-through.

`tools/pack-rimuru.sh` is the exact source-to-runtime selection. Run it to reproduce
all human sheets and the portrait, then run `./build.sh`. Older
`pack-rainoray.sh` and `pack-motion-polish.sh` reproduce historical art and can
overwrite current sheets; run `pack-rimuru.sh` last when rebuilding all art.

## Why these motions

- Lead a strike with hips/shoulders; hands and the rigid blade follow together.
  [Animation Mentor: body mechanics](https://www.animationmentor.com/blog/animation-tips-tricks-what-makes-or-breaks-a-good-body-mechanics-shot/).
- Delay hair and coat slightly behind the body, then let them settle.
  [Animation Mentor: overlap and follow-through](https://www.animationmentor.com/blog/tutorial-animate-overlap-and-follow-through/).
- Give attacks readable anticipation, contact and recovery; give running distinct
  contact, compression and airborne poses.
  [Adobe: animation principles](https://www.adobe.com/creativecloud/animation/discover/principles-of-animation.html).

These principles guide authored poses. The runtime still uses a simple frame
timer; there is no new skeletal or cloth-physics system to explain.

Rejected studies stay here for provenance, outside packaged assets. The dark coat
lost to the blue coat. Initial running had restrained arm/hair motion; unpinned
loops jumped at the seam. Several cuts bent the blade or barely swung it. Only
selected rigid-blade poses enter the atlas. The generated north entrance turned
toward camera, so the clean north exit is reversed instead. The MiniMax attempt
was unavailable on trial access and produced no frames. Request manifests include
planned jobs; per-job responses record actual calls.

## Budget

Four supplied trial allocations began with 40 generations each: **160 initial**.
Final service balances on 2026-09-16: **0 + 11 + 34 + 40 = 85 remaining**.
See [budget.json](budget.json) for timestamps and service-reported usage.
The service reports 41 used against the first 40-generation allocation after
asynchronous completion, so reported usage totals 76 rather than 160−85.
USD credit balance was $0; generation allowances paid for this work.

All four keys remain in an owner-only private file outside the repository. No
credentials belong in art requests, documentation or commits. Check all saved
balances before requesting more keys.

## Verification

`./build.sh` passed all **66 test programs**, Java 17 lint compilation, and the
packaged-asset check run from `/tmp`. It produced `build/B2BJ.jar`.
Checks cover grid/transparency/uniqueness, run boundaries, looping, directions,
contact timing, gameplay regressions and asset loading from the JAR.

[Render evidence](../../testing/rimuru-human/README.md) includes all four run
directions, actions, transformation, portrait and fence occlusion. These are staged
headless renders, not a completed manual playthrough or a frame-pacing test.
