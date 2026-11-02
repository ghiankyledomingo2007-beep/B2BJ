# Rainoray skill kit — 2026-11-02

Status: owner approved the full kit on 2026-11-02; implemented in the playable game. The original staged audition remains a visual design reference. “Void Sever” is a working game name, not a claim about an anime ability.

## Combat intent

Land a combo or read an attack, earn an opening, then spend the opening on a heavier move. Fallen enemies fuel the next attack. Keep existing bindings and give the human form a distinct sword-and-devouring identity.

| Input | Job | Visual language |
| --- | --- | --- |
| Tap LMB | Existing connected four-hit sword chain; final hit helps earn the ultimate. | Blue-white steel trails, small hit pause; stronger last cut. |
| Hold LMB | Close-range charged cleave; punish an opening and break ordinary guards. | Body coils, blade lights, short broad swing; clear recovery. |
| F | Parry into reversal; a successful read creates a punish window and earns ultimate readiness. | Brief gold contact spark, immediate blue-white counter-cut; no large ground indicator. |
| R | Beelzebub consumes nearby remains, restoring HP/Ichor and storing one Devoured charge. | Existing black-violet absorption flows into the sword, leaving a small persistent ember until spent. |
| RMB | Void Sever replaces Ichor Crescent: an earned advancing sword ultimate. | Coiled wind-up, quick advancing cut, dark seam through the hit lane, delayed violet-white fractures and a short recovery. |
| Space / Q | Existing dash / transformation. | Existing motion and identity preserved. |

## Crescent replacement: Void Sever

- Start with a readable0.45–0.50s wind-up. Mouse aim remains adjustable until release.
- Advance up to approximately320 world pixels over0.18s in the committed direction, respecting world collision. This is a narrow melee lane that can cross several enemies, not a homing projectile.
- A thin cut remains behind Rainoray. He turns into a finishing cleave; about0.13s after the advance, the line ruptures along the enemies struck. Each target has a fixed two-hit maximum: contact and delayed rupture.
- Retain about0.45s recovery. Invulnerability belongs only to the brief advancing portion; preparation and recovery can be punished. No long cinematic camera lock or full-screen white flash.
- A Devoured charge adds the violet secondary rupture and stronger ordinary-enemy stagger. It is spent once. Bosses take bounded damage and a brief valid punish window, never automatic execution or indefinite stun.
- Earn one stored use through combo finishers, successful parries and absorption. Show readiness inside the existing RMB slot. No new key or large HUD meter. Readiness should survive form changes; current gains are34% per landed chain finisher,50% per successful parry and25% per absorbed corpse. Rates still need hands-on balance testing. Aim for an occasional earned moment, not a skill fired every few seconds.

## Make the other skills feed the fight

Parry keeps a short readable guard window. Success opens a short counterattack opportunity and contributes to ultimate readiness. Boss attacks must explicitly be designated parryable; ground hazards and area eruptions remain dodge decisions. No blanket parry against everything.

Beelzebub keeps corpse harvesting and its existing sustain purpose. One or several corpses store at most one Devoured charge. The next charged cleave or ultimate consumes it for a clearly visible extra rupture. No unlimited stack bookkeeping and no repeated charged attack without a new absorption.

Ichor continues to govern human-form duration. Parry and charged cleave no longer deduct additional Ichor; the ultimate uses its earned readiness. This removes the incentive to avoid the skills merely to stay human. Readiness and stored empowerment survive form changes, but reset on death, retry and chapter changes; neither is written into the campaign save. An interrupted ultimate or committed empowered strike spends its charge.

## Original animation audition (historical scope)

The three clips use current game scenery and character/enemy sprite sheets. The latest sound audition replaces the old recordings with an original void-magic pass coded by Claude Code: inward breath, detuned resonance, spectral tears and diffuse tails. The owner rejected the earlier physical metal/explosion layers, which have been removed. Extra effects and ultimate movement are choreographed in a preview tool. The parry and absorption clips use existing mechanics plus proposed follow-up effects. The parry opening is staged longer, with a smaller guard spark replacing the existing large cross; orange hit overlays are replaced for the empowered strike. Damage reactions in the ultimate are scripted for readability, not proof of an implemented hit system. The ultimate footage shows the empowered version; an unempowered implementation should use a smaller rupture.

The impact revision adds two contact frames (ivory silhouettes, then ink silhouettes with an edge) to the advancing cut, ultimate rupture, parry catch/punish and empowered cleave. A33–100ms pose hold marks the contact while sound tails continue. These holds are staged in the preview output, not installed simulation rules. Use them on these major beats rather than every basic hit.

Reduced-effects versions of all three concepts keep the blade, lane and contact/hold timing, with no added silhouette flashes, camera shake, dimming, afterimages or extra sparks. All source is in tools/PreviewRainorayKit.java; the installed JAR and campaign save are preserved.

Runtime implementation covers collision and hit limits, earned readiness, empowerment consumption, interruptions, boss armor/parry rules, HUD readiness and reset behavior. See the verification report below. The videos retain staged timing; they are not recordings of the installed mechanics. No new voice recordings or key bindings were added.


The sound draft lives in assets/source/audio/rainoray-skill-draft-2026-11-02/ with its generator at tools/audio/generate_rainoray_skill_sfx.py. Cue sync points target the final preview timeline including contact holds; runtime integration must use actual skill/contact events, not these fixed preview timestamps. Normal and reduced-effects clips share the new sound.

## Runtime rules and evidence

- Void Sever:0.48s aimable wind-up,0.18s collision-limited advance (maximum320px), rupture at0.80s, end at1.22s.32 contact damage plus48 rupture damage before target defense; Devoured raises rupture to66. Each target is recorded once. Walls, boss armor and death/cutscene cancellation still apply.
- Only the advancing portion protects Rainoray. Wind-up and recovery remain vulnerable. Pause cancels a committed ultimate without refunding it.
- Parry: existing0.30s window and6s cooldown. Success earns readiness and gives ordinary enemies1.15s hurt recovery. Guardian TARGET/SWEEP strikes can be caught; charge, shockwave, fissure and ground hazards cannot. A caught Guardian enters its normal bounded recovery.
- Empowered cleave consumes one stored meal on release and adds one delayed18-damage hit to landed, surviving targets with range/cover checks. No stacking. The ordinary charge is still available without a meal.
-15 approved void cue files plus one derived sustained hold loop; fixed-pitch playback and actual contact-event alignment. Existing dialogue, music and slime sounds remain.
- Native-pixel lane, rift, blade gather, small gold parry, contact silhouettes and short afterimages. Reduced mode omits the added silhouette/dimming/afterimages/sparks.

Verification: [runtime report](../testing/rainoray-kit-runtime-2026-11-02.md).
