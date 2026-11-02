# Flat pixel crest revision — 2026-10-28

Owner rejected the generated illustration as looking AI-generated. Replaced the boot mark with
code-native pixel geometry in `UiArt.crest()`: turquoise slime silhouette, ivory blade, small gold
star. Exactly three opaque colors plus transparency, 48×52 native canvas, crisp 2× display. No
generated texture or lighting; previous generated master retained for history and no longer read.
PNG export: `assets/source/ui/crest/slime-blade.pixel.png`. Dark splash and live loading line retained.

Validation: compiled all art-tool sources; assert-based check verifies three colors, binary alpha,
transparent canvas corners, opaque 640×360 splash and exact 2× pixel blocks. Inspected rendered
splash. Packaged asset test passed from `/tmp` including nested art checks. `git diff --check` passed.
Every JAR entry compared: only `assets/ui/splash.png` changed; all classes and other assets identical.
Installed SHA256: `cde38f755ac0eb652c80fa87e2074f7c770950269e4bbc71e11bf7595456d8d3`.

No forced game restart; new splash appears on next launch. Save hash unchanged across installation.
No full-suite/manual gameplay replay or OS-level splash capture. Backups, check and logs:
`/home/ghiankylledomingo/tmp/b2bj-flat-crest-2026-10-28/`.
