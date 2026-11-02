# Beelzebub R-key fix — 2026-10-28

R was registered first as Beelzebub and then as restart. Swing's InputMap retained only the second mapping, so the actual key did nothing in normal gameplay. The original test invoked `beelzebubPressed` directly through the ActionMap and missed the overwrite.

Removed the duplicate registration. The existing R handler now restarts on death/completion and otherwise tries Beelzebub. Skill eligibility, range, cooldown, cancellation, rewards and effects are unchanged.

The regression now resolves the actual pressed/released R KeyStroke through InputMap before invoking its action. It failed against the old JAR and passes after the fix. Added R-after-death checks for both campaign and legacy modes. Existing test checks repeated-key behavior, casting, pause, effects/sound and HUD.

Compiled all production/test sources with Java17/-Xlint:all. Passed six focused programs against the candidate JAR:BeelzebubTest, CampaignDesktopTest, CampaignMenuTest, LoadingScreenTest, CampaignVictoryTest and PackagedAssetLoadingTest. All349 Japanese voice hashes are unchanged. `git diff --check` passed. Inspected the captured1280px cast;1920px render also generated. Replaced build/B2BJ.jar and reopened fullscreen; save bytes unchanged across startup. No full playthrough or complete126-test rerun for this isolated binding patch.

Reproduction, checks, screenshots and save backup: `/home/ghiankylledomingo/tmp/b2bj-beelzebub-fix-2026-10-28/`.
