# Blade direction pass — 2026-09-06

Scope: source-facing correction and reviewed front/back attack integration. Shared
renderer and Java assertion harness reused; no dependencies or gameplay balance changes.

RED: attack timing/padding tests failed compilation because `padding()` did not
exist. After renderer metadata was added, the asset grid check failed against the
old 144-pixel-wide slash sheet. A full directional replacement was not accepted:
all new side studies failed visual review. Acceptance scope was explicitly narrowed
to the previously reviewed front/back studies; side artwork was not claimed fixed.

GREEN: all 17 Java test programs pass with a separate two-row `blade_cut.png` and
the unchanged legacy side sheet. Tests check exact source run pixels in front/back
attack entry poses, pixel-identical actual rendered position/size, unchanged 0.3s
duration, final recovery frame, correct east/west mirroring for run/dash/attack,
legacy side frame bounds, unique reviewed poses, palette, transparent margins,
ground baseline, and classpath loading outside the repo.

Commands: `bash build.sh`, `python3 tools/test_pixellab_call.py`, `git diff --check`.
The offline Python check uses fake credentials and mocked HTTP only: native image
encoding, auth delivery, output redaction, image-output stripping and tool allowlist.
Four muted scene fixtures rendered from the JAR were inspected in the art review
folder. They do not replace a live unassisted fight or certify side animation quality.

No checkpoint commits: extensive prior dirty work was preserved. No measured line
coverage claim, no new credential written to the project, no push or publishing.
No current game window was restarted, so the user must relaunch to see this build.
