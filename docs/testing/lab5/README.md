> Archived Lab 5 evidence (submitted 2026-10-19). The current game no longer includes the lab overlay or `--lab5` mode. These instructions describe the preserved submission in `/home/ghiankylledomingo/Downloads/01-School/B2BJ/Lab5-Submission/B2BJ-Lab5-2026-10-18.zip`.

# B2BJ — Laboratory 5: Collision Detection

## What to hand in

The supplied slides explain the implementation and demonstration. They do **not** specify an upload format, deadline, screenshot requirement, video requirement, or grading rubric. Check the LMS assignment or the instructor's separate announcement for those details.

Prepared locally:

- Runnable `B2BJ.jar` with the actual game and integrated Lab 5 overlay.
- Java source, runtime assets, build script, and tests in the submission ZIP.
- `01-no-collision.png`: separate red player and blue enemy boxes, with **No Collision**.
- `02-collision.png`: overlapping boxes, with **COLLISION!**.
- `03-blade-collision.png`: optional extra evidence showing the different Blade frame size.
- `04-desktop-no-collision.png` and `05-desktop-collision.png`: native desktop captures of the two states. These are the recommended images to upload separately.
- This guide: requirement mapping, demonstration steps, and a short explanation.

Images 01–03 are deterministic captures from the real game's Swing renderer with test-positioned actors. Images 04–05 capture the displayed native game window; real keyboard F2 and D input were also verified. The desktop harness controls simulation timing and positions actors for repeatable evidence. These are not a recording of an unassisted playthrough. No illustration or replacement UI was used.

Recommended submission: project ZIP plus the two required-state images if the upload form permits separate images. The third image is optional. Do not submit only screenshots if the assignment expects working Java code. Add your name, section, group members, and required filename only where your instructor requests them. No upload has been performed.

## Launch and demonstrate

Requires Java 17 or newer. Extract the ZIP first. From its project folder:

```text
java -jar B2BJ.jar --lab5
```

In the development checkout, use `java -jar build/B2BJ.jar --lab5` instead.

1. The real Ruined Outpost opens near enemies with the Lab 5 overlay enabled. This is a disposable, invincible test session: it never loads or writes the normal campaign save.
2. Move with **WASD / arrow keys**. Approach an enemy until the red and blue rectangles overlap. Point out **COLLISION!**.
3. Move away until the rectangles separate. Point out **No Collision**. Enemies still run their normal AI, so keep moving if they follow you.
4. Press **F2** to hide and show the overlay. The same key works during regular gameplay; merely toggling it does not enable cheats.
5. Show `src/Sprite.java`, then `checkCollision()`, `drawCollisionBoxes()`, and `drawCollisionStatus()` in `src/B2BJ.java`.
6. Close the window when finished. Restart with `--lab5` for a fresh demonstration. Use `--borderless` as an additional argument if native window decorations cause trouble; Alt+F4 closes it.

Optional: open F1, enable Unlimited Blade form, resume, then press Q to show that bounds follow the larger animated frame too.

## Mapping to the slides

| Slide example | B2BJ implementation |
| --- | --- |
| `Sprite.getBounds()` | `src/Sprite.java` imports `java.awt.Rectangle` and returns a new rectangle at rounded frame coordinates; a missing image returns zero width and height. |
| Width/height of current image | Rendering methods provide the current frame's displayed dimensions, including scale. A full sprite sheet contains many frames and is deliberately not used as one giant rectangle. |
| Player and enemy sprites | Existing Rainoray, ordinary enemies, Warden and Briarheart are reused. Each rendered frame supplies a `Sprite` value with its actual position and dimensions. |
| `BasicSprites.checkCollision()` | Existing window class is named `B2BJ`. Its `checkCollision()` loops over live enemy frames and calls `collisionPlayer.getBounds().intersects(enemy.getBounds())`. |
| Red/blue rectangles | `drawCollisionBoxes()` uses `Color.RED`, `Color.BLUE`, and `Graphics2D.draw()`. Both frames use the same camera coordinates and screen-shake transform. |
| Text feedback | `drawCollisionStatus()` shows **COLLISION!** or **No Collision** from that boolean. |

The project already separates actor simulation, animation timing, and rendering. `Sprite` is an immutable snapshot of the current rendered frame, rather than a replacement for the existing actor classes. `B2BJ` serves the role of the instructor's `BasicSprites` class. The slides do not explicitly say whether exact application class names are graded; explain this mapping if asked.

## Short oral explanation

> Each sprite gets a rectangular boundary using its screen position and displayed frame size. `getBounds()` returns that rectangle. We call Java's `Rectangle.intersects()` to check whether the player's rectangle overlaps any live enemy rectangle. Red is the player; blue is the enemy. If they overlap, the boolean is true and the screen displays COLLISION! Otherwise it displays No Collision. The bounds update with movement, camera position and animation size.

If asked about limitations:

- This is axis-aligned bounding-box detection. Transparent pixels inside the frame count as part of its rectangle; it is not pixel-perfect collision.
- Touching edges alone is not intersection: the rectangles need positive overlapping area.
- Detection reports overlap; it does not automatically stop movement or apply damage. The handout asks for detection and visualization. B2BJ's established combat shapes continue to handle damage separately.
- Dead enemies do not trigger the demonstration. A temporarily hidden player frame is labeled **Player frame hidden** rather than claiming a visible collision.

## Rebuild and verify

On Linux/macOS with a JDK 17+:

```sh
bash build.sh
java -jar build/B2BJ.jar --lab5
```

`build.sh` compiles all Java sources with lint checks, runs the assertion tests, and packages assets. `CollisionLabTest` covers position rounding, frame size instead of atlas size, separated/overlapping/edge-touching bounds, missing images, F2 toggling, camera movement, Blob/Blade sizes, dead enemies, both campaign regions, both bosses, legacy room rendering, and normal-session behavior.

Verified on 2026-10-18: all 77 test programs passed, plus the build's additional packaged-asset checks. Native desktop input and both collision states passed. Build transcript: `build-verification.txt` beside this guide. The separate native harness used frozen simulation for repeatable captures; the normal `--lab5` launch runs the game loop and enemy AI.

For a simple Windows PowerShell compile (without running the full bash test script):

```powershell
New-Item -ItemType Directory -Force classes | Out-Null
javac --release 17 -d classes src/*.java
java -cp classes B2BJ --lab5
```

Run those commands from the extracted project folder so loose `assets/` can load. Windows execution itself has not been tested here.
