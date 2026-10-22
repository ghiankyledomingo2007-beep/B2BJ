# Area One conversations — 2026-10-23

The [story](../../design/area-one-story.md) is implemented in the campaign, using the existing window kit and pixel font. Captures come from the real Swing game renderer:

- `mara-conversation.png`: first meeting at Captain Camp; Mara's matching world sprite stands beside the player.
- `rainoray-conversation.png`: the current masked Rainoray sprite supplies the human portrait.
- `warden-conversation.png`: the Warden waits for its introduction before activating.
- `mara-art.md`: generation prompt, source sheet and import details.

Enter, E or left click advances. X skips. Escape pauses. Dialogue stops the simulation and clears held combat input. Optional discoveries and return conversations can be revisited; their rewards cannot be repeated.

`OutpostDialogueTest` checks frozen movement/enemies/Ichor, blocked attacks, pause, save/Continue, old save compatibility, one-time boss introduction, repeat rewards, keyboard-repeat suppression, closing-click safety, text wrapping, accessibility text and all portrait assets. `CampaignVictoryTest` covers a late first meeting with Mara, skipping the gate scene, both region exits and final completion. Existing UI tests now dismiss the opening before testing combat.

Reproduce the captures by passing this directory as the output argument to `OutpostDialogueTest` after compiling the game and tests. The regular build also runs the dialogue test from `/tmp` against packaged assets. These are automated interaction/render checks; they do not replace a full uninterrupted campaign playthrough.
