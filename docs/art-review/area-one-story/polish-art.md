# Camp and portrait polish — 2026-10-23

User review rejected the old HUD icon as a dialogue portrait, the thin Mara sprite, the large control footer and the campfire's smooth-looking flame.

## Current assets

- [Rainoray slime portrait](../../../assets/characters/slime/rainoray_portrait.png): generated from the current idle sheet, retaining the curled tip, face and five-point star. Sampled once to 64 × 64 and drawn at 2×.
- [Mara standing sprite](../../../assets/characters/npcs/mara_idle.png): a separate, clearer world sprite. Fitted to an 80 × 80 frame with aspect ratio preserved, then drawn at the same 2× scale as human Rainoray. Opaque heights measure 70 and 67 native pixels respectively.
- Mara's portrait still uses the left half of [the original sheet](../../../assets/characters/npcs/mara_sheet.png). Its earlier right-hand world sprite is no longer rendered.
- Rainoray's human portrait uses a 40 × 40 crop of the current masked sprite at exact 3× scale. The Warden uses its current 64 × 64 art at 2×.
- [Campfire sheet](../../../assets/effects/fire_camp.png): eight 30 × 40 frames, five opaque flame colors and transparent background. Drawn by `Kit.campfire`, with flat flame clusters and small square embers. Runtime stays at 2×. The camp's dynamic glow is smaller and dimmer.
- [Editable campfire](../../../assets/source/shared/fire_camp.pixie-dust): updated only after confirming the old project had one layer and matched the previous PNG exactly. The updated project was exported and compared pixel-for-pixel with the new runtime sheet.

The dialogue panel now shows the portrait, speaker, line, a small next arrow and a clickable **Skip** label. Keyboard shortcuts remain in the Controls menu and accessibility description. The large footer, chapter label, line counter and duplicate opening notice were removed.

## Generation records

Both new character assets use the built-in imagegen tool. Generated originals were copied unchanged into the project. Pixel-size fitting happens once when the renderer loads them, using nearest-neighbor interpolation.

### Rainoray portrait prompt

Use case: illustration-story. Create ONE production pixel-art dialogue portrait of Rainoray in slime form for the existing RPG. The supplied sprite sheet is an IDENTITY reference: use its first idle frame. Preserve the round low cyan slime body, the little curled tip at the top, the small black oval eyes with tiny highlights, the small friendly smile, and the pale gold FIVE-POINT STAR suspended inside the lower body. It must visibly be the same character. NO armor, NO clothes, NO necklace, NO circular badge, NO medallion, NO arms, NO legs. Portrait framing: the slime's face is prominent, the body fills most of one square, with the star visible below the mouth; 5 logical pixels of transparent margin around the silhouette. Strict clean pixel art matching the game's sprites: a 64 by 64 logical pixel canvas, enlarged by an integer for delivery; coarse visible square pixel clusters, dark teal stepped outline, 4 flat turquoise/cyan shades, pale cyan highlights, 2 gold star shades, no gradients, no airbrush, no blur, no tiny texture noise, no anti-aliasing. Calm attentive expression, looking toward the viewer. Transparent background. No scene, no frame, no UI, no letters. A single portrait, not a sprite sheet or multiple characters.

Reference: `assets/characters/slime/slime_idle.png`.

### Mara standing sprite prompt

Use case: illustration-story. Create ONE standing overworld NPC sprite, Mara the field medic, for this top-down pixel RPG. Image 1 is her identity and outfit reference: retain brown skin, tied-back brown hair with one grey streak, cream scarf, moss-green long coat, brown boots and side satchel. Image 2 is the EXACT pixel density, anatomy, perspective, silhouette-size and style reference for the world sprite: match Rainoray's 80 by 80 frame scale. Produce a SINGLE 80 by 80 logical pixel square canvas, enlarged by an integer for delivery. Mara should occupy about 28 logical pixels in width at shoulders and 60 logical pixels from hair to boot soles. Feet baseline at logical y=72, body centered x=40. Human proportions like the reference, with readable face and head, not a tall thin realistic miniature. Front/down-facing idle stance with very slight overhead RPG perspective; boots planted shoulder-width, arms relaxed. Simplify the outfit to bold readable flat clusters: 3 olive shades, 3 cream shades, 3 skin/hair shades and a dark ink outline. Clear warm highlights, never nearly black all over. No fabric texture, no fine photographic detail, no gradients, no blur, no smooth edges. No ground shadow or background. Keep the satchel small and use a plain brass square clasp instead of a cross logo. One transparent isolated full-body character only. No portrait, no labels, no frame, no extra pose, no weapon. Use actual chunky low-resolution sprite pixels, not a high-resolution illustration covered in noise.

References: `assets/characters/npcs/mara_sheet.png` and `assets/characters/blade/rainoray_idle.png`.
