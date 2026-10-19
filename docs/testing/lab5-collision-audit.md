> Archived Lab 5 evidence (submitted 2026-10-19). The current game no longer includes the lab overlay or `--lab5` mode. These instructions describe the preserved submission in `/home/ghiankylledomingo/Downloads/01-School/B2BJ/Lab5-Submission/B2BJ-Lab5-2026-10-18.zip`.

# Laboratory 5 collision detection audit

Date: 2026-10-18

**Follow-up completed 2026-10-18:** The integrated F2 overlay, `Sprite.getBounds()`,
`B2BJ.checkCollision()`, red/blue boxes, and both text states are now implemented.
`--lab5` launches a save-isolated campaign test session. All 77 test programs and
additional packaged-asset checks passed; native keyboard input and screenshots
were verified. See [the completed submission guide](lab5/README.md). The original
audit below records the state before implementation.

Source: `/home/ghiankylledomingo/Downloads/Laboratory 5 - Collision Detection.pdf`.
All slide text extracted; code and demonstration screenshots on pages 5–6 and 8–11 visually inspected.

## Finding

The current overhaul builds successfully but does not yet demonstrate the handout's exact rectangle-based exercise. Existing combat collision is not sufficient evidence of the requested `getBounds()` and `Rectangle.intersects()` implementation.

The handout explicitly says to update the previous laboratory's code. Recommended next change: add a switchable collision demonstration to the existing B2BJ game. A separate application would be a less direct interpretation. No gameplay code was changed during this audit.

## Requirement mapping

| Handout step | Current B2BJ evidence | Remaining work |
| --- | --- | --- |
| Import `java.awt.Rectangle`; add `Sprite.getBounds()` | No `Sprite.java`; actors and animation live in separate classes | Provide the rectangle boundary method in the existing sprite/actor drawing flow and clearly explain its mapping to the instructor's `Sprite` example |
| Bounds use rounded position and current frame dimensions; null image gives zero-sized bounds | B2BJ draws cropped animation frames, generally at 2x scale, with ground anchors | Calculate bounds for the displayed frame, including scale and offsets; never use the entire sprite-sheet dimensions |
| Add an enemy sprite | `Wisp`, `CampaignEnemy`, and `Guardian` already exist | Reuse an existing live enemy for the demonstration |
| Boolean `checkCollision()` calls `player.getBounds().intersects(enemy.getBounds())` | Gameplay uses radius/distance and shape-specific hit tests; rectangle intersections also appear in rendering culling | Add the explicit player/enemy rectangle overlap check; rendering culling is not the lab check |
| Draw player bounds red and enemy bounds blue | Attack warnings exist, but no lab bounding-box overlay | Add a presentation toggle and draw boxes in the same coordinate system as the sprites |
| Display `COLLISION!` or `No Collision` | Not implemented | Show current overlap result and demonstrate both states |

The slides use `Sprite` and `BasicSprites` as example class names. They do not state whether renamed classes are accepted. Keeping `Sprite.getBounds()` where practical and documenting the equivalent of `BasicSprites` reduces presentation ambiguity. Do not claim that an adaptation has instructor approval.

## Integration constraints

- Keep existing combat hitboxes and damage rules intact. A sprite-frame rectangle includes transparent padding and is a different geometry from the game's precise combat checks.
- Scope the overlay explicitly to sprite rectangle overlap. Do not present an overlap message as proof of an actual damaging hit.
- Inspect both rendering paths: `B2BJ` and `CampaignRenderer`. The campaign has two regions, including Corrupted Forest; changing only legacy room rendering would miss the main game.
- Cover frame size, scale/anchor alignment, camera movement, overlapping/separated/edge-touching rectangles, and dead or missing sprites. `Rectangle.intersects()` requires positive-area overlap; merely touching edges is false.
- Add the presentation key to the visible Controls page and README. Provide a short repeatable demonstration route using the existing protected testing session if needed.
- Preserve the substantial pre-existing uncommitted overhaul. No commit, push, submission, or external publication was performed.

## Verification performed

`./build.sh` passed on 2026-10-18: Java 17 compilation with all lint checks, all 76 `*Test.java` programs, and the additional packaged-asset check from `/tmp`. The script rebuilt `build/B2BJ.jar`. This is a baseline result, not verification of a Lab 5 implementation.

No manual playthrough or native window demonstration was performed. The PDF contains no explicit grading rubric, deadline, upload format, or submission instructions. A passing grade cannot be inferred from the build result.
