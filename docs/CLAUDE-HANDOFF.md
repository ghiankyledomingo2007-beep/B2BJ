# B2BJ continuation — 2026-10-23

Ghian ended the Codex session and will continue with Claude. Start from the current
checkout and wait for his next direction; older handoffs describe historical work.

## Checkout

- Repository: `/home/ghiankylledomingo/Domynix/gamedev-vault/Projects/B2BJ`
- GitHub: `https://github.com/ghiankyledomingo2007-beep/B2BJ`
- Active branch: `backup/local-work-20261019-b2bj`
- Latest implementation commit: `2549fa96536a7a544b45ec80a99987fe60b4d75b`
- Java 17 Swing, no runtime dependencies. `bash build.sh` compiles, tests and packages
  `build/B2BJ.jar`; `./run-game.sh` launches the normal game.

## Current state

- Human Rainoray has eight real facing views across all 19 actions, including trails,
  dash echoes and transformations. Row order: S, E, N, W, SE, NE, NW, SW. Keyboard
  diagonals select the matching body view; fresh mouse movement still aims attacks.
- Human running is 299 px/s, previously 208. Slime remains 260; diagonal movement is
  normalized. Dash speeds, combat timing and damage were not changed in this pass.
- New ruined-gate title art, quieter title controls, smooth serif/sans menu fonts.
  Camp opens with U/click, suspends play and blocks input leakage. Existing ornate
  HP/ichor borders remain. Smaller diamond cursor, direction cue, hover target marker
  and thin enemy health bars with a damage trail replace the bulky targeting ring.
- Firelight has smooth runtime falloff and moving 2D shadows. Floor pools are
  occluded by projected actor/prop footprints; small sprite bloom is not occluded.
  Static masks and buffers are cached. Reduced Effects freezes flicker; dormant
  arena braziers remain dark. The old speckled pools were removed from both floor bakes.

## Art and save precautions

- Preserve the current pixel polish and eight-way sheets. The 672 prior cardinal
  body frames remain intact, padded where diagonal weapon tips needed more canvas.
- Editable timeline: `assets/source/characters/rainoray/Rainoray-Human.pixie-dust`
  (1,344 frames, 152 tags). Its Before polish layer is the eight-way import snapshot;
  the earlier four-way native project remains in the adjacent `archive/` directory.
- Original Blender file is unchanged. A full Blender rebuild does not merge painted
  pixels; use `tools/sprite3d/build_sheets.py --preserve-cardinals --out FRESH_FOLDER`
  for review first. Never run the editable source's `rebuild.py` casually.
- Legacy packing scripts are not launchers. `PackFrames` rejects three-/four-facing
  human or morph output into runtime paths. Retired title/cursor art is preserved in
  `assets/source/ui/`, excluded from the JAR.
- Pre-install backup: `/home/ghiankylledomingo/Backups/B2BJ/2026-10-23-before-eight-way/`.
- User prefers previews on laptop eDP-1-1 (1920×1080 at x=1920), not HDMI. Use a
  temporary no-save campaign for testing; do not overwrite the personal save.

## Verification and limits

All 89 Java test programs and packaged-asset checks passed. PackingToolTest passed
separately. Native editor export/timing/overwrite checks and Blender rig/camera checks
passed. All 177 runtime PNGs and 279 packaged asset files matched disk; the 394-entry
asset inventory was verified. No dependencies added; `pip_audit` was unavailable.

Slime artwork is still cardinal. Shadows approximate footprints rather than limbs.
A full campaign playthrough and manual eight-direction combat sweep remain unchecked.
See [review, screenshots and rebuild notes](art-review/rainoray-eight-way/README.md).

The last temporary preview launcher is in `~/tmp/b2bj-ui-preview/`; its title is
`B2BJ — Movement & lighting preview (temporary, no save)`. It starts human beside a
lit brazier with testing invulnerability, unlimited blade and 25 shards. Focus loss
pauses it. Verify live process state before closing anything; older normal game/editor
windows may still exist. No further feature work is queued by this handoff.
