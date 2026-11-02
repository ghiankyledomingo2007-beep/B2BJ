# Minimal crest splash — 2026-10-28

Replaced the large wordmark/starfield boot splash with a small slime-and-blade crest, Rainoray's
gold star, flat dark background and a thin turquoise loading line. The generated transparent master
and exact built-in imagegen prompt are in `assets/source/ui/crest/`. Runtime remains a 640×360 PNG
displayed by the JVM splash mechanism; no added startup delay or dependencies.

`Splash` owns the bar geometry used by both the live overlay and `UiArt.splash()`. Added the narrow
`tools/art/build-art.sh splash` target; no unrelated art regeneration. Reviewed the actual generated
PNG at its native display size. The manifest's manual-art guard remains intact.

Validation:
- All production and test sources compile with Java 17 and `-Xlint:all`.
- `UiOverhaulTest`, `PixelConsistencyTest` (192 sheets), and `PackagedAssetLoadingTest` pass.
  Packaged verification ran from `/tmp` against the candidate JAR, including its nested art checks.
- Compared every packaged asset with the previous installed JAR: only `assets/ui/splash.png` differs.
  Same manifest splash path, verified 640×360 dimensions. All audio preserved.
- `git diff --check` passes. Installed JAR SHA-256:
  `135a27e0a630f8d05e2404bbb38f698adc20f502afc373e250cd27f27fffa126`.
- Reopened normal game via `b2bj-crest-1793171392`; PID 704181 active on two checks,
  empty startup log, campaign-save hash unchanged immediately across launch.

Backups, candidate, test and launch logs:
`/home/ghiankylledomingo/tmp/b2bj-crest-splash-2026-10-28/`.
No full-suite run, manual gameplay replay, OS-level splash capture, commit or push.
