# Outpost polish — 2026-10-23

Follow-up to the [asset cleanup](../asset-cleanup/README.md), based on the owner's
screenshots of the dummy, watchtower fire, Warden encounter, sweep, enraged form and gate.

- The training dummy has two stitched cloth/straw variants at the original 36 × 72
  frame size and ground anchor. [Source and prompt](dummy-art.md).
- Tower and arena braziers share a broad, flat-color eight-frame flame. The Warden's
  phase-two flames use the same shape with its cyan palette. Two small flames sit
  around the armor instead of five tall flames above the crown and shoulders.
- The Warden uses exact 4× pixels, increasing its canvas from 192 to 256 pixels.
  Flashes, afterimages and fallen remains share that size and foot anchor. Briarheart
  stays at its native 96 × 96 art drawn at 2×. Boss damage, health and attack ranges
  are unchanged.
- The sweep uses a tapered solid cleave with a bright edge and coarse pixel clusters.
  Its four poses play once per attack instead of repeatedly circling with a dithered tail.
- The first encounter waits for a clear approach, pans to frame the Warden and gate,
  then presents the dialogue. The Warden recognizes Rainoray's name as a death record.
  Combat stays frozen through the camera's return; pause and skip remain available.
- The East Gate has eight opening poses: wooden doors separate and the portcullis
  lifts to reveal a dark passage and road. Opening waits until the player approaches
  after victory, and holds open afterward. The old golden dithered portal is gone.

The seven changed runtime sheets have matching editable projects. Each prior project
was verified single-layer and pixel-identical to the previous PNG before replacement;
each new export was checked against its runtime PNG. Original versions remain in Git
checkpoint `8a12789`. The dummy master is preserved under `assets/source/outpost/`.

Captures use the actual Swing renderer in temporary sessions. Scripted combat captures
set boss states for inspection; they are not evidence of a complete manual boss fight.

## Preview

- [Warden reveal clip](warden-reveal.mp4) and [dialogue frame](warden-reveal.png).
- [Gate opening clip](gate-opening.mp4): [closed](gate-closed.png),
  [opening](gate-opening.png), [open](gate-open.png).
- [Phase-two flames](warden-enraged.png) and [sweep](warden-sweep.png).
- [Training dummy](dummy.png) and [watchtower fire](tower.png).

## Verification

Java 17 lint/compile, all 85 test programs and packaged asset checks passed.
`WardenSceneTest` covers clear approach, reveal timing, pause, skip, frozen combat,
camera return, gate proximity and loading an already-open gate. Art tests check
frame sizes, distinct poses, hard alpha, flame palettes and solid sweep clusters.
Existing combat, story, save compatibility and Lab 6 checks also pass.

The build includes all 182 runtime PNGs byte-for-byte and excludes generated masters
and archived assets. Seven editable exports match their runtime sheets. No game
dependencies were added. The [interactive inventory](../asset-cleanup/index.html)
includes this pass.

The packaged preview was opened at (2254,245), 1280 × 720, wholly on the laptop
eDP screen. Its launch log was empty, and the regular campaign save's hash and
modification time remained unchanged.

Full uninterrupted manual campaign playthrough and FPS profiling remain unchecked.
