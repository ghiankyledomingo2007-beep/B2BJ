# Manual pixel corrections — work in progress

The owner approved manual corrections on 2026-09-16. These files use local pixel editing with Pillow and Aseprite's batch scripting. The Aseprite brush UI was not used. No generation service or credits were used for this pass. The last live PixelLab balance remains 3; it was not queried again.

## Editable files

- [canonical.aseprite](canonical.aseprite): four native 80×80 direction bases, one frame/tag each. Original source is preserved on a hidden layer; manually drawn ivory mask, fixed markings and red dot are on the visible corrected layer. North retains the original back view.
- [run-draft.aseprite](run-draft.aseprite): 48 frames, four 12-frame direction tags at 20 fps. Hidden original layer and visible correction draft. Legs, coat and flowing hair mostly reuse source frames; arms, grip and masks have local edits.
- [Animated comparison](review.html): original versus draft, normal/slow playback and individual frame stepping.

These are editable drafts, **not installed or accepted replacements**. The first complete articulated-body redraw was discarded because it looked stiffer than the source. The current localized run corrections still need shoulder/torso joins, complete removal of old sword edges, consistent profile neck joins, and stronger front/back stride. The mask detail is constrained by its small native pixel dimensions and still needs artistic review against the supplied reference. This pass does not repair the remaining support/combat/transition/morph sheets. Their findings remain in the [full audit](../human-motion-audit/README.md).

## Reproduce

From the project root:

```bash
rtk proxy python3 tools/repair-human-frames.py
rtk proxy aseprite --batch --script tools/export-human-review.lua
```

The Python script writes the staged PNGs only. The Lua script creates the layered editable files, with direction tags assigned after all frames exist so later frame insertion cannot extend earlier tags. Both commands regenerate these draft files; save any hand-edited Aseprite version under a different name before rerunning them. Runtime assets and the existing selection manifest are not changed.

Validation: all four run rows have 12 distinct nonempty transparent 80×80 frames. Both Aseprite documents reopen and export with exactly matching visible PNG pixels and the correct four tag ranges (4 canonical frames and 48 run frames). RGB values behind fully transparent pixels are ignored during comparison. Browser review loaded all four directions and its pause, slow-speed and loop-boundary frame controls worked. These are file/playback checks, not visual acceptance. No game build was needed for these staging-only changes; the earlier 68-test gameplay result is separate.
