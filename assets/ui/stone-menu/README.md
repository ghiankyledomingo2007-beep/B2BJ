# Stone pause menu artwork

Generated with the built-in image generation tool on 2026-10-25, from the stone mockup selected by Ghian. These are the unmodified RGBA source outputs, not screenshots containing baked menu text. All labels, controls and effects remain live Java/Swing UI.

| File | Contents | Source output |
| --- | --- | --- |
| `frame.png` | Carved slate frame, cyan slime crest, gold fittings, runes, blank title plaque and banners | `exec-080b750c-d785-4b64-81d3-5886382a0865.png` |
| `button.png` | Empty slate action plate with gold clamps and right-hand engraving | `exec-4d33a6f5-8cc4-4e4f-862e-44f8c4cd08e5.png` |
| `icons.png` | Five equal horizontal cells: slime, gear, book, potion, gate | `exec-25b6aa35-ed6c-424c-97ba-45715248f793.png` |

Generation reference: `exec-309e94b2-b31e-4032-81ca-94cee990e212.png`, the approved stone mockup. Original outputs reside in `/home/ghiankylledomingo/.codex/generated_images/01a14f3c-ce5f-7ea1-8a0d-f73da12fb96d/`. Runtime loads only the project copies above; that machine-local directory is not required.

## Prompt specifications

- **Frame:** Use the approved stone menu as the style reference. Isolate its full carved blue-slate frame, brass corner fittings, glowing cyan slime crest, cyan side runes and navy hanging banners. Preserve the empty opaque midnight content well and wide blank title plaque. Remove every label, button, icon, Close control, HUD and game background. Orthographic pixel-art UI asset with a genuinely transparent exterior.
- **Button:** Derive one empty unselected horizontal slate menu plate from the same reference. Keep gold corner clamps, bevelled stone depth and the engraved diamond on the right. Remove all text and icons. Transparent outside the plate; no game scene.
- **Icons:** One transparent horizontal row of five equally spaced icon cells matching the reference: cyan slime, metal gear, open book, cyan potion, arched gate. Consistent optical size, readable silhouettes, gold/slate/cyan palette. No labels, plates or background.

`StoneMenuArt` trims transparent margins, samples the sources onto native 2x pixels and thresholds edge alpha. It caches the scaled frame, action plates and icons. Crest/rune light, crest motes and a 450ms selection sweep render separately at runtime. Reduced effects removes motion while retaining a static selected state. Original source PNGs retain their alpha and resolution for future art work.
