# Enemy attacks and hit feel — research brief (2026-10-16)

Sources are cited inline. "(synth)" marks a 60 fps synthesis to be tuned by playtest.

## Attack timing
- Signal + active >= 340 ms (~20 f) on known moves; the hitbox is live only 2-4 f (drawn bigger than the blade); recovery is the longest phase and is the punish window; a bait pose holds >= 240 ms. (Anatomy of an enemy attack in Dark Souls 3 — https://www.gamedeveloper.com/game-platforms/anatomy-of-an-enemy-attack-in-dark-souls-3)
- Start values tell / active / recover in frames (synth): scout lunge 24/10/30, shield bash 20/3/30, spitter 30/1/40 (3-shot fan on one frame), boss slam 50/4/50. Stronger attack = longer, more obvious tell. (Enemy design and AI for melee — https://www.gamedeveloper.com/design/enemy-design-and-enemy-ai-for-melee-combat-systems)
- Body first: a pull-back pose held 3-4 f; chargers never turn mid-attack; projectiles gather particles, sound and a bright flash before release. Ground markers only for area and boss attacks, in the exact shape. (Dead Cells wiki: Zombie, Rampager, Inquisitor; Telegraphing — https://www.gamedeveloper.com/design/enemy-attacks-and-telegraphing)
- At most 1-2 enemies attack at once; the rest circle for 1-2 s and ask again; attack only on screen, never during another's stagger. (Battle Circle AI — https://code.tutsplus.com/battle-circle-ai-let-your-player-feel-like-theyre-fighting-lots-of-enemies--gamedev-13535t; Intelligent Brawling — https://www.gamedeveloper.com/design/intelligent-brawling)

## Hit feel
- Hit-stop freezes attacker and victim only (victim vibrates); light 3-4 f, heavy 6-9 f, kill +3 f, player hurt 8-12 f. (Capcom brawlers; Sakurai on hitstop — https://sourcegaming.info/2015/11/11/thoughts-on-hitstop-sakurais-famitsu-column-vol-490-1/; Celeste Player.cs; Nuclear Throne, The Art of Screenshake — https://www.youtube.com/watch?v=AJdEqssNZ-U)
- Shake along the hit vector, integer offsets: +-2-3 px on hits, +-5-6 on slams, <= 12 f; keep an off switch. One white frame on hit. (Making Game Feel — https://infovore.org/?p=5275)
- Hurt: brief stun, then blinking invulnerability (A Link to the Past: 19 + 39 f). Sound on the same frame: >1 kHz transient + body + falling sub.

## Pixel effects
- Smears 1-2 f, never on anticipation or recovery; dust dense on frame 1 then easing out; rings and sparks 3-6 f; flare 2 f. (Slynyrd Pixelblog 9, 36, 56, 59)
- Projectiles: bright core + dark edge, drawn on top, length matches speed; enemy damage in red/magenta (clashes least with fire and gold); player effects cyan/white; keep the ground mid-value so effects are the brightest thing. (Boghog's bullet hell 101; Enter the Gungeon wiki)
- Death: flash away, a 3-frame burst, the body (here: edible remains) persists.

## Readability
- One silhouette and one wind-up pose per archetype; an alert hop/bark when an enemy notices you; eyes that track the player.

## Do / don't
- Do fire sound, flash, freeze, shake and particles on one frame; scale hit-stop with damage, capped.
- Don't freeze the whole game in a crowd, smear on anticipation, chain attacks out of recovery, or clutter the screen.
