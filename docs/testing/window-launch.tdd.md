# Borderless launch fallback — 2026-09-08

Journey derived from the user's report: opening B2BJ must display a playable window.

## Diagnosis and scope

The live game and a minimal decorated JFrame remained `IsUnMapped` / `Iconic`
under this GNOME/X11 session with both OpenJDK 21 and IntelliJ's JBR 25.
The game event thread was responsive. A GTK dialog and an undecorated JFrame
were `IsViewable`. This isolates the observed failure to the native decoration
path, but does not establish the underlying desktop bug.

The minimal fallback is opt-in `--borderless`, set before packing the JFrame.
`run-game.sh` forwards arguments; normal decorated startup remains unchanged.
Alt+F4 closes the borderless window. No desktop settings or gameplay logic changed.

## TDD evidence

- RED checkpoint `96cf557`: `WindowLaunchSmoke --borderless` compiled against the
  previous JAR and failed `--borderless must bypass native decorations`.
- GREEN: compiled `tools/WindowLaunchSmoke.java` with `javac --release 17 -Xlint:all`
  against the rebuilt JAR. `java -ea -cp <jar>:<test-classes> WindowLaunchSmoke`
  passed with `--borderless`, no arguments, and `--borderless-extra`.
- Checks cover exact option matching, fixed preferred game size, actual 1280×720
  borderless content, non-resizable window and EXIT_ON_CLOSE. Decorated geometry
  initially failed on this desktop, so actual geometry is asserted only for the
  fallback; the other invocations verify options, not desktop visibility.
- `./build.sh`: all 74 test programs passed, including 50 admin scenarios and
  packaged-asset loading outside the project directory.
- `bash -n run-game.sh` and `git diff --check`: passed.
- Actual rebuilt game launched on OpenJDK 21 with `--borderless`: X11 reported
  `IsViewable`, `Normal`, 1280×720 at laptop coordinates 2240,180 and active focus.
  A Robot screenshot was visually inspected: gameplay, slime, HUD, Menu and map
  rendered. Only the previous hidden B2BJ process was terminated.

No numeric coverage instrumentation is installed; no percentage is claimed.
The native smoke check stays at the title and disposes only its own windows.
The desktop's decorated-window failure remains outside this fallback's scope.
