# UI consistency audit — 2026-11-01

The owner flagged the old pause banner and cramped top-right Menu action after approving Ivory Heraldry menus and Journey Ribbon HUD.

## Changes

- Shared compact menu rows center the complete icon/label group, with a20px icon and10px gap instead of32px/14px; large pause-menu rows retain their existing proportions. Mouse, keyboard and accessibility behavior is unchanged.
- Focus-loss pause banner, full dialogue frame, portraits, skip label, camp services, prices/ranks, expanded map, region card, save notice, defeat/victory screens and missing-art fallback now use the shared ivory serif type and gold/navy framing.
- Opening captions/ability labels and boss name cards use the same typography; boss name cards clear the resource ribbon. No scene timing, voices, effects or camera logic changed.
- Shared key caps and world interaction prompts match the identity. Controls retain their existing bindings. Camp receives readable heart, flask, hourglass, sword and shard icons; victory uses the matching crown.
- Expanded map scales to the viewport. Reduced map-label size removes Standard Yard/Inner Court overlap. Dialogue frame clips at its allocated top edge; Raphael stays inside the left slot and retains animation/reduced-effects behavior.
- Existing logo, approved skill atlas, Journey Ribbon layout and requested slight world zoom-out remain intact.

## Audit coverage

Reviewed normal campaign UI: title/loading, HUD/menu action, pause/Settings/Controls, dialogue/Raphael, camp, map, chapter/boss cards, opening captions, notices, defeat and victory. World sprites/damage numbers and legacy classroom/lab-specific renderer remain pixel art. This is a campaign UI audit, not a full game/art/audio rewrite.

## Verification

Java17 `--release17 -Xlint:all` scoped compilation passes. Twenty focused test entrypoints pass: CampaignMenuTest, VisibleMenuTest, CampaignOptionsTest, CampMenuTest, DialoguePresentationTest, OutpostDialogueTest, ReincarnationOpeningTest, OpeningSealTest, WardenFxTest, WardenSceneTest, WardenVictorySceneTest, CampaignVictoryTest, UiOverhaulTest, UiAuditTest, FullscreenViewportTest, HudOverhaulTest, TitleMenuIconsTest, TitleScreenMotionTest, CameraZoomTest, MenuButtonLayoutTest.

The new MenuButtonLayoutTest exercises idle/hover at720p,1080p,1440p and ultrawide. It fails against the previous JAR because the label touches the border, and passes against the new JAR. Dialogue's initial above-frame border-pixel failure was fixed by clipping the frame and rechecked. PackagedAssetLoadingTest also passes from `/tmp`; final packaged Menu/camp/asset checks pass. Dialogue and opening also passed against the candidate from `/tmp` before the final world-prompt typography-only adjustment.

Actual Java renderer output includes 27 captures; every dialogue line was checked for fitting width/height at three viewports. Reviewed pause, gameplay, map, camp, menus, dialogue, ending and opening captures. Final gallery: `docs/art-review/ui-audit-2026-11-01/index.html`, opened at `http://127.0.0.1:4517/artifact/380639916173/` in Chrome tab1863068280. All12 gallery images decoded with no horizontal overflow. Dialogue/ending stills use staged backgrounds and are labeled accordingly.

## Installation and limits

Installed SHA256 `3ad2c8f0a8677b21da3d05e6bcc30dce541bf51ffee704623a4afb22f3540277`. ZIP audit: only23 class entries across11 scoped class families changed; no entry removed or added, all assets/audio byte-identical, changed classes target major61. User save hash remained `d90b2c68f96a8fd1396a0046f728391a4395257b79721872652431e915d5f26d` from baseline through installation. Current build was compared against its backup before atomic replacement.

Backup, scoped diff and logs: `/home/ghiankylledomingo/tmp/b2bj-ui-audit-2026-11-01`. No forced game restart, manual full playthrough, native desktop input check, full-suite run, commit or push. Restart the game to load the installed UI. Native app control is unavailable in this session; browser shows static actual-render previews.
