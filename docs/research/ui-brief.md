# B2BJ UI research brief (2026-10-16)

**Tags.** [M] measured by me on official Steam screenshots (1080p, +-3 px; art-pixel grid detected for pixel-art games), given as % of screen and px on a 640x360 grid. [S] cited source. [S*] snippet-only (page blocked). [R] my recommendation. gameuidatabase.com returned 403; interfaceingame.com has no per-game text. No Hades/Hollow Knight/HLD animation timings were retrievable, so timings are [S] generic or [R]. Sea of Stars and Blasphemous use a 640x360 grid (x3 at 1080p): their px apply 1:1.

## 1. Boss health bar
| Game | Position | Width | Frame/fill @360p | Notes |
|---|---|---|---|---|
| Hades | top-centre | 20% each (2 bosses = 2 bars) | 8/2 px | small-caps name (6 px cap), pale cap on fill edge |
| Blasphemous | bottom-centre | 45% (53% with ornaments) | 11/7 px | gold rails, name 8 px cap, bar centre 20 px above edge |
| Moonlighter | bottom | 67% | none/6 px | flat red, small name |
| CrossCode | bottom | 97% | 12/8 px | "BOSS" tag, white damage band, phase slash at 36% |
| Children of Morta | top | 34% | thin | skull badge |
| Gungeon | right edge | 0.7% x 96% H | vertical | neon green |

- Bars sit away from the player's HP [M]. Hollow Knight/Silksong show none; Destructoid says that hurts newcomers [S].
- Ghost trail: hold ~0.5 s, restart on each hit, then drain [S gamedev.tv]; roughly constant drain time, accelerating [S plasmabeam]. CrossCode: lost HP = white band [M]. Bar shake read "cartoonish" [S].
- Dead Cells' Conjunctivius: 3 chains = 3 phases, one hit capped at 15% max HP [S]. 50 ms hitstop + white flash on hit [S Strayspark]. [R] B2BJ bar: ~288x11 px frame, 7 px fill, bottom-centre, name above, trail pale cyan; enrage = fill turns hot hue, 2 px jolt 150 ms, name flash.

## 2. Objective tracker, prompts, toasts
- Few games keep a live tracker. Children of Morta: top-centre line (yellow label + white text, 1.4% H) with a "Done" banner, plus a short block under the minimap [M]. CrossCode's appears intermittently top-right [S*]; Sea of Stars leaves side quests untracked [S].
- Persistent info in corners, momentary at centre; updates flash then hide [S Strayspark]. Text >=28 px at 1080p (~9 px at 360p) [S GAG].
- Prompt [M]: accent-cyan text, no plate, above the door. Toast [M]: Gungeon pill 46% W x 8.9% H (32 px), black fill, 1 px cyan border, 8 px text.
- [R] tracker: 2 lines, 65% opacity, 100% for 5 s on change, hidden in boss fights.

## 3. Minimap
- Size [S*]: most 1-3% of view, ceiling 10% (MDPI). [M] Children of Morta: circle 26% H (95 px) = 3.1% area, top-right, walls as thin grey outline, no fill. Moonlighter: bottom-left 72x54 px (1.7%), translucent plate, room graph with puzzle-tab doors, 3x3 window, white brackets = current room.
- Abstract beats terrain; red hostile/green friendly/yellow objective; edge arrows off-map [S solhun]. Fog: blank until explored [S Wikipedia]. Hollow Knight: map page only [M].
- [R] ~72x72 px, top-right, north-up.

## 4. Title / main menu
- [M] CrossCode (568x320): logo 33% W in top 40%; "Press to start" at 45% H (10 px cap at 360p, 1 px rule under, input glyphs); key art right third; tiny version text bottom-right.
- Keep the menu alive (particles), button tweens 0.2-0.4 s, skippable splashes [S Game Developer]. UI motion 100-500 ms, enter ~300 ms vs exit 200-250 ms, ease-out [S NN/g].
- [R] logo fade 1.2 s, item stagger 60 ms, cursor 90 ms with tick/confirm/back sounds, confirm flash 120 ms then 300 ms fade; <=5 entries, Continue default.

## 5. Pause menu / settings
- [M] Sea of Stars: panel 86% W x 66% H, 44 px margins, 1 px light border, corner studs, drop shadow; world ~80% dimmed (est.); selected row 27 px, cyan border, glove cursor; button hints bottom-right. Hollow Knight: black pages, white flourish dividers. Moonlighter death screen: background = 16x9 mosaic (120 px blocks), darkened: a pixel-consistent blur substitute.
- Options: Controls/Audio/Visuals/General tabs, master/music/SFX/UI sliders, brightness [S caniplaythat].
- [R] Resume default, <=6 rows, remember cursor, 60-70% dim.

## 6. Area titles, loading, death, victory
- Area card [M]: CrossCode plate hangs from the top edge, 39% W x 8.6% H (31 px), flat navy, 1 px light edge, diamond emblem, tracked label (6 px cap) over name (10 px). Hollow Knight NPC name card: bottom-left, tracked small caps 1.9% H over name 5.2% H.
- Loads under ~4 s: overlay, not screen; no tips; press-to-continue [S Game Developer]. Loader after 300 ms, hold >=400 ms [S Learning Equality].
- Death: fast retry [S joyplayx]; Moonlighter [M]: loot recap with lost items struck through, one action bottom-right. Sea of Stars Level Up [M]: 16-22 px small-caps title, 5 px tracked subtitle, hue-coded orbs, 34 px stat strip.
- [R] card 0.4 s in / 2.2 s hold / 0.6 s out, non-blocking; fades 250 ms out / 350 ms in; death retry at 0.8 s, skippable after 0.4 s.

## 7. Pixel fonts
- Cap height @360p [M]: primary 8-10 px (Sea of Stars 9, Blasphemous 8, Gungeon 8, CrossCode 10); labels 5-6; dialogue-heavy Celeste 12. Proportional text, right-aligned numbers.
- m5x7: ascender 7, x-height 5, sizes in multiples of 16 [S]. Body 5x7/5x8, titles 8x8+, 1 px strokes, no AA, no outline+shadow on body [S pixnote]. Contrast >=4.5:1, outline over busy backgrounds [S GAG]. [M] Sea of Stars: numbers white + 1 px black outline, menu text 1 px drop shadow.
- [R] Java2D: AA off, integer coords, 640x360 buffer, x2 nearest.

## 8. Enemy telegraphs
- Timing [S]: reaction ~0.25 s; wind-up = reaction + response delay + buffer (Hornet dash 15f = 0.5 s at 30fps); up to ~3 s for novices/busy screens; punish window 0.25-1 s.
- Mobs: animation tell (Dead Cells red aura + "!"); bosses add ground info (red line along charge path) [S]. Hades II's red-zone AoEs drew legibility complaints [S nex-3]. [R] decals only for delayed, offset hits by bosses/elites.
- Drawing [M]: Gungeon unfilled red rings (2-3 px stroke @360p); beam 7-9 px, yellow core/orange/red. HLD beam 5-8 px hot pink + white core. Nuclear Throne sniper laser 1-2 px solid red. All solid; hazard = most saturated hue (here hot red-orange/magenta; keep rust trim duller).
- Clutter [S]: attack tokens (DOOM 2016 concept; example pools 1 melee/2 ranged/1 heavy per target). Don't bury telegraphs under VFX or dim colours; match decal to cause; size AoE vs dodge; avoid dark red alone.
- [R] aim line 1-2 px fading in over 150 ms, locks >=250 ms before firing, then 6-8 px beam.

## 9. Pixel UI frame craft
- [M] One recipe: Sea of Stars panels and HUD plates share dark navy fill (~90% opaque, est.), 1 px lighter border, corner studs, shadow, one highlight hue; Blasphemous reuses gold ornaments on portrait, counter, boss bar; Hades: parchment + gold frame.
- 9-slice: inset contains corner art, leaves flat edge/centre (e.g. 48x48 source, 8-16 px corners); point filter, integer scale [S generalistprogrammer].
- [R] 6-8 UI colours: navy fill, slate border, bronze trim, cyan selection, gold for reward/rank; light top/left bevel.

## 10. Amateur tells
- Mixed pixel scales [S itch.io x2]; non-integer scaling (640x360: x2 = 720p, x3 = 1080p) [S notkey]; AA fonts.
- Too many fonts, all-caps paragraphs, equal-weight buttons [S wayline]; contrast <4.5:1; hidden telegraphs; 1 px-thin key info, pillow shading [S Derek Yu]; unskippable splash/death screens, loader tips.

## Sources
Consulted: https://gdkeys.com/keys-to-combat-design-1-anatomy-of-an-attack/ https://note.com/darkangels_417/n/nb520b22d60f7?hl=en http://www.chaoticstupid.com/telegraphs-2-post-attack-vulnerability/ https://nex-3.com/blog/hades-ii-has-a-legibility-problem/ https://www.gamedeveloper.com/design/enemy-attacks-and-telegraphing https://signalsandlight.substack.com/p/how-do-simultaneous-enemy-attacks https://www.strayspark.studio/blog/attack-token-system-ue5-group-combat-that-feels-fair https://www.strayspark.studio/blog/game-ui-ux-design-principles https://deadcells.wiki.gg/wiki/Conjunctivius https://www.playstationlifestyle.net/2018/08/06/dead-cells-tips/ https://www.destructoid.com/hollow-knight-silksong-is-great-but-theres-one-issue-dating-back-to-the-original-they-really-should-have-addressed/ https://plasmabeamgames.wordpress.com/2024/03/01/boss-health-bars/ https://community.gamedev.tv/t/how-would-you-implement-health-bar-that-decreases-gradually/221793 https://www.nngroup.com/articles/animation-duration/ https://www.nngroup.com/articles/response-times-3-important-limits/ https://www.gamedeveloper.com/design/game-design-rules-loading-screens https://www.gamedeveloper.com/design/how-to-create-immersive-game-intros https://www.gamedeveloper.com/design/splash-screens-are-wrong https://design-system.learningequality.org/loaders https://pixnote.net/en/learn/pixel-text/ https://managore.itch.io/m5x7 https://gameaccessibilityguidelines.com/use-an-easily-readable-default-font-size/ https://gameaccessibilityguidelines.com/provide-high-contrast-between-text-ui-and-background https://notkey.studio/en/tutorials/choosing-the-right-render-resolution-for-a-pixel-art-game/ https://generalistprogrammer.com/tutorials/nine-slice-scaling-explained https://derekyu.com/makegames/pixelart2.html https://itch.io/post/14274357 https://itch.io/post/6877680 https://www.wayline.io/blog/game-ui-fails-practical-fixes https://www.joyplayx.com/article/how-to-handle-failure-in-games-designing-death-and-retry-loops https://caniplaythat.com/2023/11/01/sea-of-stars-accessibility-review/ https://mechanicsofmagic.com/2022/04/28/blog-response-visual-design-of-games-14/ https://design.solhun.com/en/game-ui/minimap/ https://en.wikipedia.org/wiki/Mini-map https://interfaceingame.com/ . [M] data: https://store.steampowered.com/api/appdetails?appids=<id> screenshots for 15 games (Hades, Dead Cells, HLD, Hollow Knight, Eastward, Sea of Stars, CrossCode, Children of Morta, Moonlighter, Blasphemous, Celeste, Gungeon, Nuclear Throne, Tunic, Death's Door).
Snippet-only (blocked, unverified): https://www.mdpi.com/2220-9964/12/2/58 https://steamcommunity.com/app/368340/discussions/0/1736633196174765994
