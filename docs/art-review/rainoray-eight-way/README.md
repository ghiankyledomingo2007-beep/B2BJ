# Eight-way Rainoray and interface revision — 2026-10-23

Rainoray's human form now has eight real facing views for all 19 actions. W+D uses
northeast movement and animation; the other combinations select their matching
three-quarter views. Releasing movement holds that facing. Standing dash uses it;
fresh mouse input still aims attacks. Human running speed is now 299 px/s (previously
208); slime stays at 260. Diagonals are normalized, and dash speeds, damage and attack
timing are unchanged.

- 14 body sheets, four matching trail sheets, two eight-way transformation sheets.
- 1,344 editable frames and 152 tags in the native Pixie Dust project.
- All 672 previous cardinal body frames retained pixel-for-pixel (guard/hurt were
  centered in larger transparent cells to protect diagonal weapon tips).
- [Cardinal preservation checks](preserved-cardinals.json).
- [Blender rig, camera and pose inspection](motion-inspection.json): 20 bones, 228
  sampled diagonal poses, correct screen-space headings, unchanged source model hash.
- Slime form retains its existing cardinal artwork; its movement and direction cue support diagonals.

The previous native file remains under `assets/source/characters/rainoray/archive/`.
A complete pre-install backup also lives outside the repository at
`/home/ghiankylledomingo/Backups/B2BJ/2026-10-23-before-eight-way/`.

## Interface

The title uses a quiet ruined-gate illustration, live serif wordmark and four simple
text buttons. Continue and the two-press New Journey save-replacement guard are unchanged.
The previous title layers are preserved under `assets/source/ui/title/previous-title/`;
loading-screen layers have explicit paths under `assets/ui/loading/`.

Camp shows a compact button while exploring. U or a click opens camp services; the
world waits while it is open. Upgrades can be clicked or bought with 1–4 while the
panel is open. H rests and saves; E talks to Mara in the Outpost; Esc/U or Leave closes.
Prices, caps and save protections still live in RuinedOutpostGame. Held inputs and
clicks cannot pass through into combat. No save-format change.

The large animated cursor ring is replaced by a small native cursor. A short facing
chevron follows movement or aim. Hovering an enemy adds a target marker; enemy health
bars show a brief damage trail and hide for healthy, unalerted enemies. Shield state
uses a small gold shield. The normal HP/ichor frames remain. Menu text and boss names
use bundled DejaVu typefaces with their license. The repeated combo instruction is gone;
charge status remains when actually charging.

## Lighting

Fire and lamp pools now render at runtime with smooth falloff instead of fixed dithered
bands baked into the terrain. Nearby actors and props block the light on their far side;
the brazier's three feet break up the pool beneath it. Shadows move with the actors,
stay behind their sprites and clear when they leave. Static prop masks and scratch
images are cached; Reduced Effects disables flicker. The flame sprites stay unchanged.
Outpost and Forest floors were rebaked through the existing hand-retouch guard.
A 240-frame headless check around the four lit arena braziers measured 2.10 ms median /
2.34 ms p95 for the floor-light pass alone; this is not an end-to-end FPS measurement.

This is 2D footprint projection, not per-limb shadows or a 3D lighting engine. The
small flame bloom can still tint surfaces; only the floor pool is occluded.

Current packaged render captures: [title](title.png), [camp](camp.png),
[diagonal movement](diagonal.png), [brazier lighting](lighting.png).

## Rebuild and validation

`bash build.sh` compiles Java 17, runs the full test suite and verifies packaged assets.
`/usr/bin/python3 tools/art/check-rainoray-project.py` checks a lossless native export,
clip timing and overwrite guards. `tools/sprite3d/check_diagonals.py` is a read-only
Blender inspection. `build_sheets.py --preserve-cardinals --out FRESH_FOLDER` renders
new diagonal views without replacing the polished cardinal cells. Review before installing.
The legacy `PackFrames` tool now rejects three- and four-facing runtime exports before
writing anything; its regression check still permits archived legacy exports.

The new title art, generation prompt and source are recorded in
[the title source notes](../../../assets/source/ui/title/README.md).

Final validation: all 89 Java test programs and packaged-asset checks passed; the
packing guard regression passed separately. All 177 runtime PNGs match the JAR, and
all 394 asset-inventory hashes match disk. The native project round-trip and original
cardinal pixel checks passed before this build. No new dependencies; `pip_audit` was
unavailable. No full campaign playthrough or manual eight-direction combat sweep.
