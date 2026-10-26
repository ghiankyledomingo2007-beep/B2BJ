# Fullscreen menu and HUD — 2026-10-26

The pinned launcher now passes `--fullscreen`. The game uses the monitor's native viewport: world camera, mouse aim, telegraphs, screen effects, loading transition and edge HUD positions follow the actual panel size. The ability bar stays at the bottom centre at its existing pixel size.

Title art, menu icons, text, pointer, spacing and save notices share the 1280×720 layout scale. At 1920×1080 they are 1.5× larger. Actual Swing button bounds scale with the artwork, preserving mouse targets, keyboard controls and the existing new-save confirmation.

Sage's seal scale increased from 0.60 to 0.85, with its centre lowered to y=76 and captions shifted down 40 pixels. The brighter halo remains voice-reactive, reduced effects stay static, and the boss offset keeps the seal below the health bar.

## Verification

- Java 17 compilation with `-Xlint:all`; production and all test sources compile cleanly.
- Eight focused programs pass against the staged JAR outside the source directory: `FullscreenViewportTest`, `TitleMenuIconsTest`, `TitleScreenMotionTest`, `VisibleMenuTest`, `CampaignMenuTest`, `LoadingScreenTest`, `InnerVoiceOverlayTest`, `SagePresenceTest`.
- Viewport tests cover 1280×720, 1600×900, 1920×1080 and 2560×1080; menu checks include physical click targets and scaled text/icons. HUD pixel checks verify bottom-centre placement without stretching the ability art.
- Installed `build/B2BJ.jar` atomically and opened the real pinned desktop launcher. The visible game window measures 1920×1080; title screenshot reviewed. No launch log errors. Existing JAR assets are byte-for-byte unchanged.
- Actual game-renderer frames at 1920×1080 reviewed for the larger Sage, bottom HUD and boss health-bar clearance. These use an in-memory staged campaign, not the player's save.
- `git diff --check` passes. Full test suite and a continuous playthrough were not repeated for this UI change.

Evidence and launch/test logs: `/home/ghiankylledomingo/tmp/b2bj-menu-fullscreen-2026-10-26/`. No commit or push.
