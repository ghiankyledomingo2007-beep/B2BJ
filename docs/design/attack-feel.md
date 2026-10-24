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


## 2026-10-21 — HP, defense, critical hits, and impact feedback

- Player starts with 100 HP; Vitality adds 20 HP per rank. Ordinary absorption heals 10 HP. Existing upgrade/save format is unchanged.
- Basic attack: Blob 20 ATK, Blade 24 ATK. Damage is `round(ATK * 100 / (100 + DEF))`, minimum 1 for a positive attack. Blade has 30 DEF; Iron Membrane adds 50 DEF for one accepted hit.
- Outgoing hits have a 12% critical chance and a 1.5x attack multiplier before defense and rounding. Blocks, invulnerability and dead targets produce no damage numbers; overkill numbers show only HP actually removed.
- Regular enemies have 64–90 HP and 0–20 DEF, tested to take 3–5 basic, noncritical hits in either form. Fallen Knights (160 HP / 35 DEF) and bosses are exceptions. Finishers, charged attacks, and critical hits can end fights sooner.
- Water finishers/Tide use 32 attack; Crescent/riposte 36; Blade finisher adds 12 attack (+12 with Edge); charge adds up to 36 attack. Boss HP ranges from 1200 to 1600 with 25 DEF; vulnerability windows and phases remain.
- Floating white damage numbers, gold CRIT labels, red incoming damage, local impact flashes, and short pixel sparks reuse existing text/particle rendering. Heavy and critical hits increase feedback through the existing shake controller. Reduced Effects disables shake and added white flashes, reduces spark count, and slows number motion.
- Runtime bounds: 48 damage labels lasting 0.9 seconds, existing 420-particle limit; reset/room transitions clear labels.
- Verification: 79 Java test programs and packaged assets passed. CombatStatsTest covers damage/balance/crit boundaries, shields, boss windows, immunity, and overkill. DamageVfxTest covers label rendering, limits, expiry, reduced effects, and reset.
- Review clip: [Combat-VFX-Preview.mp4](../art-review/combat-hp-vfx/Combat-VFX-Preview.mp4). Controlled no-save game fixtures, four sections: normal, guaranteed critical, charged heavy, reduced effects. Full game renderer; not a manual playtest. Offscreen update+paint averaged 10.03 ms across 220 measured frames, excluding encoding; desktop FPS not measured.


## 2026-10-22 — Slime cast commitment and quieter combat UI

- Slime basic water, return cut, back-dash counter and body slam use 18 ATK (previously 20). Water casting recovery is 0.60 seconds (previously 0.48). Projectile travel speed/range, release windups, 32-ATK finishers and Tide are unchanged.
- Casting reduces walking to 65% for 0.20 seconds, then restores full movement before the shot cooldown ends. Dash cancels the slowdown immediately; transformation, relocation and testing refill clear it. Pause/hit-stop freeze it with simulation. The movement calculation preserves distance across different update sizes.
- Regular enemies remain a 3–5 connected noncritical combo-hit target, including the stronger third water cut; repeated basic hits alone may take six against armor. Blade stays 24 ATK with its existing 36-ATK finisher. No enemy HP or defense nerf.
- Player vitals retain the original ornate 320×128 frame. Inside it, readable aligned HP/Ichor labels sit above recessed meters with stepped ends and shaded ruby/gold fills. Q-ready and Blade countdown cues remain.
- Ranged enemies replace full-flight warning lines with translucent ground glints and tiny charge sparks close to their body. Cues point along actual volley headings and brighten during windup, clearing at release. They communicate direction, not projectile range. Boss danger-area boundaries and melee warnings are unchanged.
- This is a first balance pass. Automated cadence, movement/cancellation, damage, geometry and HUD checks do not replace an encounter playtest.

- Follow-up dodge tuning: cooldown 0.32s in both forms (was 0.40s). Slime bursts 144 world pixels at 900px/s for 0.16s, with a watery takeoff and short ground ripples. Blade quicksteps 120 world pixels at 1000px/s for 0.12s, with up to six blue pose snapshots fading over 0.14s. Both retain collision sweeps and have a vulnerable interval before the next dash.
- Combo-preserving dodges, dash slash and Slime back-dash counters use the actual form's dash duration. Slime body-slam damage and Blade's attack-only dash slash rules remain. In-flight speed is fixed at dash acceptance, including a mid-dash transformation. Slime animation now faces the dash heading even when no movement key is held.
- Reduced Effects disables Blade echoes and keeps only Slime's initial ripple. Echoes use the existing dash sprite as a cached blue silhouette; ripples reuse water_wake.png, with no new asset generation or dependencies.

## 2026-10-23 — Warden preparation and attack silhouettes

The Warden's shared motion sheet was overriding its authored sweep, charge and
shockwave strips. Those strips now take precedence during their attacks, including
matching hit flashes and charge afterimages. Fissure lowers into a planted crouch;
the targeted slam keeps the two-fist overhead raise. The last preparation pose holds
before release. Dust and energy gather at the relevant hand, visor, impact spot or
ground lane. Shockwave preparation stirs rubble near the feet.

Preparation is now 1.05 s for targeted slam, 1.00 s for sweep, 1.20 s for charge,
1.30 s for shockwave and 1.40 s for fissure. No damage during preparation; targets and
headings stay committed. Active/recovery durations, damage and other bosses' timings
are unchanged. This tuning gives the owner more time to recognize the next move;
full encounter balance still needs manual play. [Captures and clip](../art-review/outpost-polish/README.md).

## 2026-10-23 — Blade combo retimed for weight

Owner found Blade swings "too fast". The 12 authored poses played at ~40 fps, so the four-pose draw
lasted 90 ms and the contact pose 26 ms (skipped entirely at 30 fps capture). Retimed, no new art:

| Move | Wind-up (contact) | Total | Was |
|---|---|---|---|
| 1 draw cut | 0.13 s | 0.40 s | 0.09 / 0.30 |
| 2 thrust | 0.14 s | 0.42 s | 0.10 / 0.34 |
| 3 rising cut | 0.17 s | 0.48 s | 0.14 / 0.40 |
| 4 finisher | 0.24 s | 0.70 s | 0.20 / 0.54 |
| dash slash | 0.10 s | 0.42 s | 0.08 / 0.36 |

Charged cleave (0.18/0.58) and spin (0.18/0.70) unchanged. Poses are no longer evenly spaced
(`B2BJ.bladeAttackFrame`): the coiled last draw pose holds, the contact pose and follow-through end hold
~1.5x, the smear passes quickly, the return to guard eases out. Contact frame and damage still share one
clock. Existing hit-stop (0.05 s light, 0.10 s finisher/cleave, 0.085 s kill) is unchanged and now lands on
a contact pose that is already held. Input buffer (0.12 s), chain window (move end + 0.5 s), dash rules and
press-time credit unchanged. BladeCombatTest fails if a cut drops below 0.10 s draw / 0.40 s total, a pose
flashes for under 1.2 frames, contact holds under three frames, or the finisher stops being the heaviest.
