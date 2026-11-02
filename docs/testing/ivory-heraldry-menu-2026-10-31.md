# Ivory Heraldry menu implementation — 2026-10-31

Owner selected concept 1, Ivory Heraldry, from the five menu families. Implemented its solid ivory silhouettes, stronger Cinzel lettering, cyan selection outlines, restrained gold engraving and crescent arch in the title and shared menus. Settings now has distinct voice/music symbols, larger serif labels, diamond slider handles and a centered Back action.

## Scope

Production: `TitleIcons`, `MenuTheme`, `PixelSwing`, `CampaignMenu`. Extended `TitleMenuIconsTest` and `CampaignMenuTest`. Reused the bundled Cinzel/DejaVu fonts and existing Swing controls; no new dependency. Existing title animation, reduced-effects mode, focus/keyboard handling, settings callbacks and save guards remain. Original TitleMenu dimensions/hit targets retained. Logo, splash, world/HUD, dialogue, gameplay and all audio assets unchanged in this pass.

## Verification

- Changed production classes and tests compile with Java17 / `-Xlint:all`.
- Ten targeted tests passed on `/usr/bin/java`: TitleMenuIconsTest, TitleScreenMotionTest, CampaignMenuTest, CampaignOptionsTest, VisibleMenuTest, FullscreenViewportTest, UiAuditTest, UiOverhaulTest, StoneMenuArtTest and CampMenuTest.
- PackagedAssetLoadingTest passed from `/tmp` against the actual candidate JAR, including nested character/world art, dialogue, speech assets and overlay checks.
- Inspected actual Java title render at1920×1080, Settings at1280×720 and1920×1080, and shared menu at1280×720. All title actions visible; enlarged Settings labels fit.
- An initial taller title layout exposed the existing B2BJ class's inlined menu constants in FullscreenViewportTest. Restored the original dimensions and omitted the unchanged TitleMenu class from the patch. Final viewport test passed.
- Compared every JAR entry: only the four scoped class families changed; no entries removed and every changed class targets Java17. Assets byte-identical.
- Whitespace check passed for touched sources/tests. Current JAR matched the pre-task backup before atomic installation. Campaign save hash unchanged from task start through installation.
- Browser preview opened in Chrome tab1863066973. All three screenshots decoded at expected dimensions; no horizontal overflow.

## Delivery

Installed: `build/B2BJ.jar`

SHA256: `c03e330535a69395367e3d187c8463bce6b1d6847e832481a497f69a534503a0`

Review page: `docs/art-review/ivory-heraldry-2026-10-31/index.html`

Live local preview: http://127.0.0.1:4517/artifact/45fae9b026d3/

Backup JAR, candidate, scoped diff, compile output/classes, test logs, package comparison, save hash and Java renders: `/home/ghiankylledomingo/tmp/b2bj-ivory-heraldry-2026-10-31/`.

The preview contains static captures from the actual Java UI, not interactive game controls. Native game was not forcibly restarted; installed changes appear on its next launch. No full-suite/manual combat replay, commit or push.
