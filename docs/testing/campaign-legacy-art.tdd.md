# Preserve reviewed Outpost art in campaign mode

RED `c9ec788`: an actual `B2BJ.drawWorld` render ignored the existing Remnant
atlas (“campaign bypasses approved remnant walk art”). The same branch routed
Spitters and the Outpost Warden into campaign blockouts.

Restore the existing render paths for the two original Outpost mob kinds and
biome 0's Warden. Other campaign kinds/bosses retain their own identity. Reuse
the existing animation clocks, foot anchors, health bars and frame selection;
do not generate replacement art for assets that are already usable.

`CampaignLegacyArtTest` marks the loaded atlases and checks that walk, attack
and Warden frames reach the integrated world renderer. It also checks that the
Forest boss never becomes a Warden. Existing `RemnantFacingTest` and
`GuardianPresentationTest` cover facing, every Warden phase/frame and death.
The new test's pursuit fixture was corrected to keep its target fixed rather
than moving it away every tick; the original bypass failure was genuine.
