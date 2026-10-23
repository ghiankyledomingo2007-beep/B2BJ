# Tent replacement — 2026-10-23

Created with the built-in imagegen tool. The generated master is preserved unchanged at `assets/source/outpost/tent.generated.png`. `ImportedArt.tent()` prepares the native 128 × 100 runtime sprite with nearest-neighbor sampling, binary alpha and a sixteen-color palette. The runtime file is `assets/props/outpost/set_tent.png`.

The canvas, ground anchor (64,96), collision footprint and map placement stay unchanged. The prior tent is preserved in Git commit `74ff8b3`.

Reference 1: the previous `assets/props/outpost/set_tent.png`, for camera, footprint and identity. Reference 2: `assets/props/outpost/barricade.png`, for pixel clusters and outline treatment.

## Prompt

Use case: style-transfer. Asset type: finished pixel-art RPG campsite tent sprite. Image 1 is the tent being replaced: preserve its top-down three-quarter RPG camera, downward-facing open entrance, muted tan canvas, red pennant, guy ropes, and compact footprint. Image 2 is the desired handmade pixel-art style reference: stepped dark outlines, irregular clusters of 2-4 pixels, deliberate restrained texture and warm highlights. Redraw the tent as a convincing worn canvas A-frame shelter, with a readable ridge, draped cloth, two tied entrance flaps, a black doorway, a few sewn patches and wooden stakes. The old tent looks like hard flat polygon planes; make the cloth feel relaxed and hand-pixelled. Use a native 128 by 100 logical pixel composition, enlarged evenly for delivery. Whole object contained, no clipping; transparent margins. Overall including ropes: logical x=2..125, y=3..98. Cloth base at logical y=94, central base x=18..110; the foot anchor is (64,96). Keep perspective and footprint within that template. Limited palette, 16-20 opaque colors, ochre/beige canvas, cool dark brown outlines, tiny dark red flag. Every edge a crisp stair-step, use chunky visible square pixel clusters. No gradients, smooth edges, airbrush, photorealistic texture, random fine noise, vector facets, 3D rendering, glow, floor, drop shadow, scene, characters, extra props, text or frame. Single isolated tent on genuine transparent background.
