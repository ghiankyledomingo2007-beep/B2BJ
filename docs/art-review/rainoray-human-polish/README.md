# Rainoray human form — 2026-10-23

The current white hair, blue coat, simple mask and separate sword are retained.

- Staggered ready stance, slightly bent knees and arms, quieter breathing and wrist movement.
- One small red mask mark stays visible through the front idle. The renderer adds a flat decal in memory; the editable model and packed texture remain unchanged. Side poses can still hide the mark.
- Short contact arcs replace the ribbon that followed every sword-tip correction. Draw cut, diagonal, overhead, cleave and spin use different planes; thrust uses a narrow streak. The body occludes the effects. Ordinary cuts fade before recovery.
- Start/stop sword poses clear the ground in all four directions. Full-speed running keeps the previously reviewed carry.
- The sprite builder now reads the current editable model directly. It cannot silently rebuild the old GLB. Default renders go under `build/rainoray-sprites`; `--install` remains an explicit replacement step.

[In-game clip](ingame.mp4) · [Four-facing attack review](four-facing-attacks.mp4) · [Before/after stance](idle-comparison.png) · [Game screenshot](human-ready.png)

The 11-second in-game clip uses the packaged Swing renderer and scripted controls at 20fps, with unlimited human form in a temporary session. The separate four-facing reel plays all nine attacks slowly at 8fps for inspection; it does not represent combat speed. Combat timing, damage, collision, camera scale and the normal form timer are unchanged.

## Checks

- `bash build.sh`: all 85 Java test programs and packaged checks passed with Java 17 lint enabled.
- `blender -b --python-exit-code 1 --python tools/sprite3d/check_editable.py`: rig weights, planted stance, guard continuity, ground clearance, contact-arc geometry, hidden mask/sword and body occlusion passed.
- New checks cover the mask mark through idle, effects staying off during windup/recovery, and the existing ambient aura keeping the face clear and respecting reduced effects.
- All 14 body sheets, four effects sheets and two transforms rebuilt. No clipped body cells or duplicate action frames reported. All 182 runtime PNGs have binary alpha and match the JAR. Asset inventory hashes refreshed.
- The original editable `.blend` still has SHA-256 `be6e909e7fa9555b2a48367a76dd64a2922fa178da37ca12b5d168dfb63e3c6e`. Baseline game/art is preserved in Git commit `2fe940f30121220880d79c1d3d004e9f402df43f`.

Build/reproduction:

```sh
/usr/bin/python3 tools/sprite3d/build_sheets.py
# Review build/rainoray-sprites before replacing live art.
/usr/bin/python3 tools/sprite3d/build_sheets.py --install
bash build.sh
```

No runtime dependency or per-frame geometry added. Detailed hand and hair texture retouching, a full manual campaign playthrough and desktop FPS profiling were not part of this pass. The flat decal is anchored to the current mask; recompute its UV anchor if that surface is reshaped later.
