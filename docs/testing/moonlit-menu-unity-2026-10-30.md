# Moonlit Gate menu unity — 2026-10-30

The selected Gatekeeper menu now uses bundled Cinzel lettering, a quiet full-screen gold frame, and custom gold/ivory/cyan line icons. The approved Moonlit Gate logo stays left aligned and unchanged. Removed the Select / Enter Confirm footer. Shared menu pages now use the same navy panel, thin trim, font, icons, diamond slider handles, and simple check boxes. All four volume labels are visible; Settings checkbox rows expand to prevent antialiased text clipping.

## Scope

`Ui`, `MenuTheme` (new), `TitleScreen`, `TitleIcons`, `PixelSwing`, `CampaignMenu`; bundled font/license/provenance. World/HUD artwork, dialogue type, gameplay, audio assets, logo and loading splash unchanged in this update. Existing title motion and reduced-effects behavior retained. No new runtime dependency.

## Validation

- Changed classes compiled with `javac --release 17 -Xlint:all`.
- Eleven targeted checks passed on the actual `/usr/bin/java` launcher runtime: TitleMenuIconsTest, TitleScreenMotionTest, CampaignMenuTest, CampaignOptionsTest, VisibleMenuTest, FullscreenViewportTest, UiAuditTest, UiOverhaulTest, StoneMenuArtTest, CampMenuTest, PackagedAssetLoadingTest.
- Packaged assets/fonts verified from `/tmp`, against the candidate JAR; nested art/dialogue/overlay checks passed.
- Updated icon antialiasing/frame-edge assertions for the intentional new treatment. Added bundled Cinzel-family and checkbox text-fit checks; existing callbacks, keyboard selection, disabled controls and save guards remain tested.
- Inspected actual Java renders at1280×720 and1920×1080. Native game launched fullscreen1920×1080 on the primary monitor; title and Settings inspected. Launch log clean.
- Initial native launch caught a Java25 compiler/default versus Java21 launcher mismatch. Restored the previous JAR, rebuilt all changed production/test classes for Java17, and repeated all targeted checks on the actual launcher runtime before installing again.
- Whitespace check passed for touched tracked sources/tests. Compared all JAR entries; only scoped menu classes and Cinzel resources changed. Campaign save SHA256 unchanged through installation and launch.

## Installed build / evidence

JAR: `build/B2BJ.jar`

SHA256: `edf66552d08ab9dd9e5dde04e06966cbb0cf3bf03adf46bc442fcde8a99ec404`

Backup JAR, scoped diff, logs, final runtime test log, JAR entry comparison, native screenshots and rendered layouts: `/home/ghiankylledomingo/tmp/b2bj-menu-unity-2026-10-30/`.

Font source/license: `assets/ui/fonts/Cinzel-README.md` and `Cinzel-OFL.txt`.

No full136-test rerun or manual combat replay for this UI-only pass. No commit or push. Unrelated existing work preserved.
