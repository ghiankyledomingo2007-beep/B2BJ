# Ruined gate title art — 2026-10-23

Generated with the built-in OpenAI image-generation tool. The selected full-size
master is `ruined-gate-master.png`. `tools/art/TitleArt.java` imports it to
`assets/ui/title/ruined-gate.png` at 640×360 using nearest-neighbor samples. The game
renders it at 2×; wordmark, menu labels and rain are drawn at runtime. DejaVu Serif
and Sans are bundled under `assets/ui/fonts/` with their redistribution license.

The prior eight title PNGs are retained in `previous-title/`, outside the game JAR.
The sky/outpost/forest layers still used during loading have dedicated runtime copies
under `assets/ui/loading/`. `../previous-aim-reticle.png` preserves the retired cursor ring.

## Final generation prompt

Use case: stylized-concept. Asset type: original 16:9 title-screen background for the pixel-art dark-fantasy game BLOB TO BLADE. Produce ONLY the background artwork, absolutely no text, no logos, no UI, no watermark. Scene: an overgrown ruined outpost gate in a quiet forest at blue hour, rain-dark stone, tangled branches, restrained teal ambient light and one small warm amber lantern by the arch. Composition: a confident cinematic wide composition, LEFT 44 PERCENT kept almost empty, very dark desaturated blue forest shadow with only broad shapes so cream title text and menu can be placed over it. Main architectural focal point and protagonist in RIGHT HALF. Rainoray is a small slender back-facing adventurer with long straight ice-white hair, deep navy blue long tunic/coat, light gold shoulder accents, dark boots, and one slim lowered silver sword, standing quietly on the foreground path looking into the ruined gate. Character occupies around 15 percent of image height, integrated into scene, not a giant portrait. A few subtle cyan ichor glints on the path connect the character to the gate. Style: exceptionally clean authored low-resolution pixel-art key art, strong readable silhouettes, deliberate blocky pixel clusters, coherent 640x360-style pixel grid, limited deep slate, muted pine teal, icy hair, tiny amber accents. Architectural foreground dark, arch luminous but restrained, real depth through grouped values. Avoid blurry gradients, airbrush, excessive star particles, confetti dithering, noisy texture everywhere, shiny 3D, cartoon mascot, huge moon, floating castle, gigantic blade, mountains, text, signage, border. Request wide landscape 16:9.
