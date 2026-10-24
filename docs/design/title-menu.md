# Title menu — 2026-10-24

Rainoray keeps his white hair and navy outfit. The owner requested a strong Rimuru-inspired sense of light, fluid movement and blue water magic. This pass changes the title presentation; gameplay character sheets remain unchanged.

- Large teal BLOB / pale steel BLADE wordmark with a compact shadow.
- BLOB forms first, BLADE enters with a cold light sweep, and the menu choices slide in with staggered timing. A slow occasional sheen remains after the entrance. Returning from Settings keeps the clock running; returning from gameplay replays the entrance. Reduced effects shows everything immediately.
- Continue shows the area from the saved checkpoint. New Journey requires confirmation for both valid and unreadable saves, including the N shortcut.
- One small, faceted cyan chevron marks selection, with a quiet pulse and warm text. The owner rejected both sword selectors; the wide highlight strip and underline are also removed. Continue warms the portal, New Journey brightens its energy and lifts the held sword slightly, Settings calms the scene, and Quit dims it.
- Hair and coat move on a 6.4-second loop over a clean plate. The crown, stance, hands and architecture stay anchored. Reduced effects holds the character still and removes ambient motion.
- “Through the Rain” is an original 48-second harp, strings, flute and cello score. Music stays on in title settings and fades for one second when entering a journey. Navigation, confirmation and entry use separate quiet chimes; existing mute/music/effects controls apply.

The original gate image remains intact. Built-in imagegen produced the clean background master in `assets/source/ui/title/`; `java tools/art/PrepareTitleArt.java` imports the native 640×360 plate. Character motion uses masked samples of the original character, not a replacement sprite. `TitleScreen` composites the scene at native resolution before scaling, and reuses the existing cached light textures. The rejected generated blade art/code is archived outside the game under `gamedev-vault/backups/art/title-menu-rejected-blades-20261024/`.

Audio is reproducible with `python3 tools/audio/title_audio.py`; only regeneration needs the installed FluidSynth/TimGM6mb tools. The game packages WAVs and needs no new runtime dependency. Instrument provenance is in `assets/audio/CREDITS.md`.

Validation: full 91-test build before the final animation iterations, then `TitleScreenMotionTest`, `UiOverhaulTest`, `UiAuditTest`, `VisibleMenuTest`, `CampaignDesktopTest` and `CampaignMenuTest` on the final entrance implementation. Packaged asset checks exclude the rejected blade selector. Close-up rest/gust comparisons preserve feet and background; the seven-second intro replay and live title were inspected without changing the campaign save. Native scene compositing measured approximately 4.5 ms median / 6.2 ms p95 for a 1280×720 headless render on the laptop before the entrance pass; this is rendering time, not a guarantee of whole-game frame rate.
