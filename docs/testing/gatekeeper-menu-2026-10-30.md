# Gatekeeper main menu — 2026-10-30

Ghian selected concept01 from the animated main-menu gallery. Installed the
approved Moonlit Gate logo above a compact left-side menu: ivory labels, cyan
selection diamond, a quiet selection wash, chapter/save caption and a thin gold
divider. Added the mockup's slow camera drift and staged entrance, with restrained
rain and breathing gate light. Existing wind, water, lantern, menu sounds and
title music remain connected. Reduced effects shows the complete, still title.

`TitleScreen` owns the presentation; `TitleMenu`/`B2BJ.doLayout()` share its scaled
menu bounds. `PixelSwing.Button`'s title-only branch paints the new selection and
uniform labels. Continue/New Journey save guards and real Swing controls remain
in use. The runtime logo at `assets/ui/title/moonlit-gate-logo.png` is byte-identical
to `assets/source/ui/splash/moonlit-gate-logo.png`.

## Verification

- All136 test classes completed successfully across the initial build and its
  continuation. The first run stopped when `PixelConsistencyTest` applied the
  hard-alpha sprite rule to the approved smooth logo. Added only that title-art
  exception and a byte-exact source/runtime identity assertion; recompiled the
  test and resumed at it. Already-passing tests were not needlessly rerun.
- Title motion/reduced effects, menu actions/save guards, icon states, visible
  controls and fullscreen hit targets passed. Added a check that all menu buttons
  fit their scaled parent panel. Packaged asset checks include the new logo.
- Rendered the actual Java UI at1280×720,1920×1080 and2560×1080 using a temporary
  save. Inspected the fullscreen/ultrawide images; temporary save unchanged.
- Built the JAR and ran `PackagedAssetLoadingTest` against that actual JAR from
  `/tmp`. Confirmed packaged logo/splash bytes and changed classes. Installed
  through an atomic rename after validation; original build backed up.
- Launched the game with `--fullscreen`. X11 reports a visible1920×1080 window
  at0,0, exactly covering one of the user's two monitors. Startup log empty.
- Campaign save hash unchanged through installation. Scoped whitespace check
  passed. No manual combat playthrough, commit or push.

Installed JAR SHA256:
`1c9ecba18d508ee9135fc73491b97a584f70b644de23fcc9ebdc0e9ba2b91757`.

Backups, scoped diff, build logs, renderer, images and window verification:
`/home/ghiankylledomingo/tmp/b2bj-gatekeeper-menu-2026-10-30/`.
Verified build staging: `/tmp/b2bj-build.N9u4UQ/`.
