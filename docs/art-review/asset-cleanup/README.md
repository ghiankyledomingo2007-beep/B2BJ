# Asset cleanup — 2026-10-23

Replaced the camp tent, prepared native dialogue sprites, and moved confirmed unused
art out of the runtime folders. The current Rainoray sprites, both regions and Lab 6
remain intact.

- [Current camp capture](camp.png), rendered from the packaged game.
- [Interactive inventory](index.html): filter every file by group, decision or name.
- [Inventory TSV](inventory.tsv): paths, usage, packaging, hashes and evidence.
- [PNG metrics](pixel-metrics.tsv): dimensions, colors and partial-alpha counts.
- [Archived files and restoration notes](../../../archive/assets/2026-10-23/README.md).
- [Tent source and prompt](tent-art.md); [dialogue art records](../area-one-story/polish-art.md).
- [Follow-up scenery, boss effects and animated gate](../outpost-polish/README.md).

## Changes

| Finding | Result |
| --- | --- |
| The old tent looked like flat polygon planes | New worn-cloth sprite, 128 × 100, sixteen colors, binary alpha. Canvas, ground anchor, collision footprint and placement unchanged. |
| Three dialogue images were large generated masters sampled during loading | Native 64 × 64 portraits and an 80 × 80 Mara body, imported offline with limited palettes and hard transparency. Original masters preserved under `assets/source/characters/`. |
| Twenty PNGs had no runtime use | Moved to `archive/assets/2026-10-23/`, including old Blade/slime/effect sprites, prototype environment props, concepts and unused icons. |
| Supplies had duplicate runtime and editable files | Both render paths use `set_supplies.png`; duplicate PNG and source project archived. Exporters no longer recreate them. |
| Older files could be confused with unused files | Kept the room atlas, room props and Lab 6 slime sheet because code still uses them. Kept all 91 audio recordings and both pixel fonts. |

All 22 archived files retain their original bytes. The manifest records their former
paths, current paths, SHA-256 hashes and reasons. Three character masters were moved
unchanged. The tent's original generated master is also preserved outside the JAR.
The prior tent and source project remain available in Git commit `74ff8b3`.

The editable tent project was replaced only after confirming it had one layer and
matched the prior PNG. Its new export was compared pixel-for-pixel with the new runtime
sprite. Other editable projects and teammate layers were preserved.

## Review coverage

The baseline contained 376 files under `assets/`, including 203 PNGs. All original
PNGs received dimension, palette and transparency checks, with representative frames
reviewed visually by family: characters, both regions, legacy tiles, effects and UI.
The [campaign enemy sample](campaign-enemies.png) uses their real 92/88-pixel cells.
No animation was declared unused based only on a literal filename search; dynamic
action slots, region names, source exporters and packaging were checked too.

After the follow-up polish, the current inventory contains 360 asset/source/guide
files and the 22 archived files. There are 182 packaged PNGs, all with binary alpha,
and five generated source PNGs
excluded from packaging. Binary alpha checks for softened edges; it does not measure
artistic quality. Simple flat scenery was reviewed but not all redrawn. The current
camp capture shows the tent, flame and character scale together.

Matching Warden/Briar impact or dust files and Rainoray start/turn frames remain because
both named runtime slots are active. Source projects, atlas metadata, map fingerprints,
credits and review fixtures are authoring/build records, not unused sprites.

This pass adds no game dependencies. Existing imported sound attribution remains in
`assets/audio/CREDITS.md`; no license terms were changed or newly verified.

## Verification

- Initial cleanup passed all 84 test programs. The follow-up Java 17 lint/compile,
  all **85 test programs**, and packaged asset checks passed.
- `AssetArchiveTest` verifies all 22 preserved hashes, missing former runtime paths
  and exclusion from the package. `PackagedAssetLoadingTest` checks archive/source exclusion.
- `CampArtTest` checks tent/portrait dimensions, palette limits, binary alpha,
  animated fire and human scale. Story, combat, save compatibility and Lab 6 checks pass.
- `ExportSourcesTest.py` passed routing, existing-layer preservation, repeated export
  and canonical supplies checks. Native asset imports preserve manual PNG edits.
- Every current runtime PNG matches its packaged bytes; the JAR contains no source
  or archive folders.
- All report asset/evidence links resolve locally. Headless Chrome checks passed
  all three inventory filters with no browser errors. The camp capture uses a
  temporary game session with no campaign save path.
- Live preview verified at (2254,245), 1280 × 720, entirely on the laptop eDP screen.
  Its launch log was empty; the normal campaign save's hash and timestamp were unchanged.

Full uninterrupted manual campaign playthrough and FPS profiling remain unchecked.
Representative animation frames were reviewed; every frame was not watched manually.
