# B2BJ — Blob to Blade

Java 17 desktop action adventure. Rainoray changes from a cyan slime into a temporary
Blade form by gathering Ichor. The current campaign includes **Ruined Outpost** and
**Corrupted Forest**, with the Outpost Warden and Briarheart. Each region is a
6144×4096 map with twelve landmarks, optional discoveries and a safe camp.

## Start here

- [File map](FILE_MAP.md): where the game systems, tests and art tools live.
- [Documentation index](docs/README.md): current guides and historical records.
- [School files](School/START-HERE.md): GDD, progress reports, handouts and lab submissions.
  `School/` is a local shortcut, excluded from Git.

## Build and run

Requires JDK 17 or newer. The game runs offline without downloaded Java dependencies.

```bash
./build.sh
java -jar build/B2BJ.jar
```

`./run-game.sh` builds, checks and launches. Rebuilding does not update a game window
that is already open. If native window decorations fail, use `./run-game.sh --borderless`.
Alt+F4 closes that window.

### Laboratory 6

After building, run `./run-lab6.sh`. It launches the separate enemy-chase demonstration
with a small Java heap and borderless window. You can also use
`java -jar build/B2BJ.jar --lab6`.

The lab uses 300 px detection, 80 px stopping distance and 80 px/s enemy speed.
WASD/arrows move the player; 1/2/3 select demonstration cases; Space pauses; R resets.
Red/blue boxes and collision text remain visible. This mode does not load or write
campaign saves. See the [Lab 6 guide](docs/testing/lab6/README.md).

## Folder guide

| Folder | Contents |
| --- | --- |
| `src/` | Game code and the separate lab panel |
| `test/` | Java assertion-based tests |
| `assets/` | Runtime art/audio and editable art sources |
| `tools/` | Art generation, sprite packing, previews and audio tools |
| `docs/` | Design, testing, art reviews and historical notes |
| `build/` | Generated runnable JAR |
| `out/` | IntelliJ compiler output |
| `release/` | Existing platform release outputs |
| `School/` | Local shortcut to school documents and submissions |

The Java build reads `src/*.java` and `test/*.java`. Asset and review-fixture paths are
also used by the build, tests and art tools; the file map groups them by responsibility.

## Campaign controls

- Enter starts a journey; C continues. N confirms replacing an existing save.
- WASD/arrows move. The mouse aims. Left click attacks; hold left click to charge in Blade form.
- Right click uses Tide Wave in Blob form or Ichor Crescent in Blade form. F uses Riposte.
- Space dashes. Q transforms at 100 Ichor. E interacts with discoveries, remains and gates.
- H at camp heals, saves and sets the checkpoint. 1–4 buy camp upgrades.
- Esc pauses; losing focus also pauses. T while paused saves and returns to the title.
- Hold Tab for the regional map. M toggles audio; V reduces effects; O opens audio/effects options.
- Menu or Alt+G opens the in-game menu. F1 opens testing controls during a campaign.

Blade form starts with a base duration of 12 seconds. Pickups, damage, skills and
upgrades alter that duration. At zero Ichor, Rainoray returns to slime form and recovers
for 1.5 seconds. Camp progression is saved at `~/.b2bj/campaign.properties`.

Using a testing cheat disables saving for that test session. Turning the cheat off
does not restore saving; return to the title and Continue to resume the normal save.
New Game explicitly replaces it. The [admin audit](docs/testing/admin-audit.md)
explains the tested behavior.

## Art and verification

Environment art and UI are built in [tools/art](tools/art/README.md). The current human
sprite pipeline lives in `tools/sprite3d/`; audio tools are in `tools/audio/`.
See the [documentation index](docs/README.md) for the latest overhaul captures and
older art decisions. Review outputs are separate from runtime assets.

`build.sh` compiles with Java 17 compatibility and lint checks, runs the Java tests,
checks packaged asset loading and writes `build/B2BJ.jar`. The tests cover movement,
combat, saving, rendering, map safety and lab behavior. They do not replace human
playtesting or prove release balance.

[Earlier development notes](docs/archive/development-notes-2026-10-21.md) preserve
previous art passes, test counts and release history. The September school GDD still
needs reconciliation with the two-region build.
