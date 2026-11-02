# Menu and chapter-loading cleanup — 2026-11-01

Removed the rejected main-menu tagline and divider. LoadingScreen now uses the existing Cinzel/serif fonts, ivory region name, muted gold chapter label and small animated gold stars. Both chapter entries and camp return share the change. Existing timing, skip logic, world freeze, goo transition and reduced-effects behavior are unchanged.

Only TitleScreen.class and LoadingScreen.class differ from the previous installed JAR. All other ZIP entries, including assets and audio, are identical. Changed classes target Java17. Installed SHA256: `124c992c50962c6c85184a6431cb4c92d4d11283a52f65fb140eb3d9a1eb7d4a`. User save SHA256 unchanged throughout. Backups, scoped diff, build logs, package audit and render helper: `/home/ghiankylledomingo/tmp/b2bj-loading-unity-2026-11-01`.

## Verification

- Scoped Java17 compile with -Xlint:all passed; all test sources compile. Initial targeted test compilation omitted the packaged test's helper classes; compiling those sources resolved it without production changes.
- LoadingScreenTest, TitleScreenMotionTest, TitleMenuIconsTest, CampaignMenuTest and FullscreenViewportTest passed.
- PackagedAssetLoadingTest and its bundled art/dialogue checks passed against the candidate JAR from /tmp.
- Actual Java renders reviewed for title and both region names, including camp return. Browser comparison images loaded without horizontal overflow.
- No new tests added for this visual-only change. No full-suite or manual game playthrough; the running native game was not restarted.

## Selected designs reopened

`docs/art-review/chosen-designs-2026-11-01/index.html` places original selected Ivory Heraldry01 (main menu, settings, icons) and Journey Ribbon04 beside existing31October game renders. Originals copied byte-for-byte, displayed with CSS crops only; no regenerated art. Full-image links and today's actual cleanup renders are included. Chrome: http://127.0.0.1:4517/artifact/adbe0ac03a36/ .

The user is correct that the earlier implementation is not an exact match: menu/HUD scale, spacing, frame detail and settings proportions still differ. This pass completes the explicitly requested tagline/loading cleanup and exposes that gap; it does not claim to finish a broader visual-fidelity revision.
