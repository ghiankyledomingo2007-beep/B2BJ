# Journey Ribbon gameplay HUD — 2026-10-31

Owner chose HUD concept04, Journey Ribbon. The installed game now groups HP/Ichor above the bottom-center skill bar and uses the approved Ivory Heraldry typography, ivory symbols, navy panels and thin gold engraving throughout the visible gameplay HUD.

## Behavior and scope

- Four skill slots in slime form; six in human form, including F/R. Human LMB uses the existing slash icon so its role reads as an attack. Key labels, disabled states, cooldown masks/progress and numeric cooldowns remain visible.
- Resource values update live; damage edge flash, low-health color, green passive healing and regeneration delay are preserved. Full Ichor shows the transform cue above the ribbon.
- Upper-left Rainoray crest, form label, human Blade duration and trait status. Human duration distinguishes unlimited/ending states. Trait description/time and progress remain visible.
- Mission/currency plaque wraps long objectives. Real minimap data and markers remain within a matching frame. Location names use the shared lettering and clear the player sprite. Menu retains the already approved Ivory styling.
- Boss bar uses the same visual family with its existing delayed damage trail, exposed-core and rage colors. Raphael retains its glow/presence and moves below boss information as before.
- Exploration speech is above the new resources; interaction prompts wrap inside the bottom-left area without crossing the ribbon. Toasts/debug status use matching frames.

Production scope: `JourneyHud.java` (new shared drawing/layout helpers), `B2BJ.java`, `CampaignRenderer.java`, `DialogueRenderer.java`. Uses existing fonts, art and standard Java2D; no runtime dependency added. Removed obsolete private player-HUD frame/meter routines and their unused loaded image fields. Game rules, save handling, voice/audio assets and title/pause-menu behavior unchanged.

## Verification

Changed classes and all tests compile for Java17 with `-Xlint:all`. Fourteen targeted checks passed across the verification run and final typography/cue adjustment run: HudOverhaulTest, FullscreenViewportTest, SlimeDepthRenderingTest, B2BJIntegrationTest, UiAuditTest, UiOverhaulTest, CampaignMenuTest, TitleMenuIconsTest, SagePresenceTest, SageCombatTest, InnerVoiceOverlayTest, DialoguePresentationTest, BeelzebubTest and PassiveHealingTest.

Updated existing assertions for the intentionally moved resources, trait panel and speech band; initial checks exposed old screenshot coordinates, which were corrected while retaining the live health/healing/flash assertions. Added objective-wrap content/width checks and extended collision checks to the full screen against mission/map/Raphael/debug overlays. Final typography pass was rechecked against HUD, viewport, traits, speech and healing tests.

PackagedAssetLoadingTest passed from `/tmp` against the actual candidate JAR, including nested character/environment art, dialogue assets and speech-overlay checks. Compared every JAR entry: only the four scoped class families changed (13 entries); all changed classes target Java17, no entries removed, all assets byte-identical. Whitespace check passed.

Inspected actual Java renders at1280×720,1920×1080 and2560×1080, plus damage/trait/cooldown, full-Ichor transform and human/boss states. Final small keycaps have simpler corners to avoid cramped ornament. Browser preview confirms all five embedded captures load with no horizontal overflow.

## Installed delivery

`build/B2BJ.jar`

SHA256: `11af5ba5e95ff6a707287c2114a1c25fb8f65b3b3cbe39a7983a4e8f8f170ca2`

The installed JAR matched the pre-task backup immediately before atomic replacement. Campaign save SHA256 unchanged from task start through installation. Existing native process was not forcibly restarted; next launch uses this build.

Preview: `docs/art-review/journey-ribbon-2026-10-31/index.html`

Local URL: http://127.0.0.1:4517/artifact/82c2f27a8e26/

Chrome tab1863066981 remains open. Static captures, not interactive gameplay. Backups, scoped diff, compiled classes, tests/logs, candidate, package audit, save hashes and renders: `/home/ghiankylledomingo/tmp/b2bj-journey-ribbon-2026-10-31/`.

No full-suite rerun or manual combat playthrough. No commit/push. Unrelated existing work preserved.
