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
  are unchanged; the follow-up below lengthens the Warden's preparation time.
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
- [Compact dialogue with Mara](dialogue-mara.png) and [Rainoray](dialogue-rainoray.png).
- [Five attack preparations in motion](warden-attack-tells.mp4), in the table's order.

## Dialogue and attack readability follow-up

The owner found the dialogue panel too empty and the text too thin. The panel is now
1024 × 160 at the standard game resolution, with a 136-pixel portrait frame, bold
display-font speaker names and 3× body text. All current spoken lines fit within two
rows. The ornate frame and small Skip/next controls remain; no instruction footer.

The shared motion atlas previously took precedence over the existing attack strips.
Sweep, charge and shockwave now use their authored preparation, strike and recovery
poses. The last preparation pose holds before release. Fissure reverses the recovery
poses into a crouch with hands planted, then holds it during the eruption. Hit flashes
and charge afterimages use the same selected pose and facing as the body.

| Attack | Preparation | Time before damage |
| --- | --- | --- |
| Targeted slam | Both fists raised; dust gathers at the committed impact spot | 1.05 s |
| Sweep | Side-on arm preparation; energy gathers at that hand | 1.00 s |
| Charge | Braced forward pose; visor energy and dust kicked behind its committed heading | 1.20 s |
| Shockwave | Both hands glow low; rubble stirs around the feet | 1.30 s |
| Fissure | Planted crouch; dust follows the committed crack lane | 1.40 s |

The previous Warden preparation was 0.9 s for most attacks and 1.1 s for fissure.
Targets and headings remain fixed throughout preparation; no damage is dealt then.
Attack damage, active durations, recovery and the other bosses' timings are unchanged.

## Verification

Java 17 lint/compile, all 85 test programs and packaged asset checks passed.
`WardenSceneTest` covers clear approach, reveal timing, pause, skip, frozen combat,
camera return, gate proximity and loading an already-open gate. Art tests check
frame sizes, distinct poses, hard alpha, flame palettes and solid sweep clusters.
Existing combat, story, save compatibility and Lab 6 checks also pass.

The follow-up tests all five preparation poses and timings, committed targets,
exact body/flash silhouettes in both facings, dialogue wrapping and the relocated
Skip target. The clips use scripted states through the real renderer, not a manual
fight or a performance benchmark.

The build includes all 182 runtime PNGs byte-for-byte and excludes generated masters
and archived assets. Seven editable exports match their runtime sheets. No game
dependencies were added. The [interactive inventory](../asset-cleanup/index.html)
includes this pass.

The packaged preview was opened at (2254,245), 1280 × 720, wholly on the laptop
eDP screen. Its launch log was empty, and the regular campaign save's hash and
modification time remained unchanged.

Full uninterrupted manual campaign playthrough and FPS profiling remain unchecked.
