# Outpost Warden attack draft — 2026-11-02

Earlier effect-study scope: treated the owner's “Stone Guardian” as the Outpost Warden from Chapter One. Three attacks in two visual phases plus one transition use the installed arena and existing Warden/slime sprites. New effects and player movement are staged in canvas; they do not change attacks, hitboxes, saves or the installed JAR.

## Moving encounter revision — 2026-11-02

Owner found the stationary studies repetitive, accepted a physical-first/core-second direction, and asked for movement and slightly more difficulty against constant running. The new **Fight flow** tab is a playable local movement study. Earlier Hammerfall/Core Sweep/Faultline/Core Unbound tabs remain available as VFX studies. This does not change the installed game.

The boss pursues a moving opponent and uses a bounded velocity estimate to close across their path. It selects a shoulder rush when the player is escaping or beyond useful melee reach. Melee preparation follows briefly, then locks; the rush cannot steer after commitment. Physical lead is capped at24 native pixels. Detached fists have separately committed targets, with48/60px lead caps and small ground contact cues. The chest beam turns across the current running direction, then keeps that planned turn instead of following the player. It originates at the authored chest anchor.

| Attack | Preparation / final lock window | Action | Recovery in phase I |
| --- | --- | --- | --- |
| Advancing fist |0.86s /0.30s | Short early step, committed contact |1.25s |
| Turning backhand |1.00s /0.32s | Moving front arc; rear safe |1.40s |
| Shoulder rush |1.12s /0.34s | Straight0.55s rush; no turning |1.75s, plus0.40s on arena-edge collision |
| Detached fists, phase II |1.10s /0.38s | Two committed arrivals0.48s apart |1.50s base |
| Core beam, phase II |1.25s /0.34s |0.90s planned turn; close inner space safe; single damage event |1.80s base |

Phase I uses physical attacks. Phase II alternates physical attacks and core spells, adds9 native pixels/s to pursuit and subtracts only0.15s from recovery. At half health, the1.4s transition deals no damage. The same1200HP pool continues; selecting phase II directly begins at600HP. Damage is14/13/18/14/18 respectively in this prototype, with0.7s hit immunity and one damage event per attack (one per fist). This is proposal tuning, not a calibrated change to live game stats.

WASD/arrows move at130px/s. Space dashes at450px/s for0.16s with1.2s cooldown, matching the installed slime speed/dash ratio at half world scale. Hold left mouse for simple cursor-aimed shots; exposed-core hits deal more damage. The automatic demonstration circles, sometimes dashes, and fires in bursts so both new spells are visible before victory. Take control resets the chosen phase. Manual play pauses on window blur. Replay preserves manual mode; Watch demo restores automation.

Verification:12 simulation checks cover pursuit, capped prediction, committed charge, missed-charge opening, front/back sweep, beam safe space/single-hit rule, second-fist commitment, dash cooldown, safe half-health transition, actor bounds/attack selection and both spells appearing before demo victory. Fixed60s naive circling took31 damage in phase I and90 in phase II; reactive-dash circling took44/0. Those deterministic examples demonstrate that behavior matters; they are not a human balance study. Earlier seven VFX variants still pass eight distinct frames/reduced effects checks. Scripted browser keyboard/pointer events exercised movement, dash, shooting and input release. Visual inspection covered all five attacks and corrected the beam origin to the chest. Physical keyboard playthrough and subjective sound were not checked.

Limits: reused sprite poses, simplified projectile combat, rectangular arena bounds, scenery props without collision, and no player skill kit. Before installation, adapt decisions and geometry to the real map/player skills and playtest with the actual combat loop. Six existing Warden WAV cues are reused; no new voice/music bank. No game code, installed JAR or save edits. Backup/baseline: /home/ghiankylledomingo/tmp/b2bj-guardian-pressure-2026-11-02/.

## Assessment of the current fight

The Warden already has a strong silhouette, separate attack strips, rubble/dust effects, a core-exposure rule and long recovery windows. It does not need a replacement character design. The weak point is how similarly several attacks read and how often the safest answer remains backing away.

`Guardian.hits` currently treats SWEEP as a circular radius, so moving behind it is not rewarded. TARGET lands on the recorded player position, which can make an arm slam feel disconnected from the body. FISSURE activates its whole long rectangle together. Improving these attack shapes and timings would make the visual work meaningful. Existing five attack types should not all be expanded into longer multi-hit combos.

## Earlier effect-study proposals

| Concept | Body cue and impact | Player response | Opening |
| --- | --- | --- | --- |
| Hammerfall | Arms rise, core pressure gathers, fast planted descent; compact cyan contact, branching seams and sparse violet armor chips. No leap. | Dodge sideways after the body commits. Actual ground contact controls the damage area. | Fists stay low; roughly1.5s recovery with visible core. |
| Core Sweep | Shoulder twists, hand scrapes, a violet front arc carries a cyan edge and a small gold leading glint. | Flank behind it or time a dash through the swing. Back is safe. | Torso settles with the arm extended. Reward staying close. |
| Faultline | Planted strike sends a narrow chain of violet armor fins forward, each eruption arriving after the last. | Sidestep the committed path. No late tracking or instant arena-wide line. | Boss stays planted while the line travels. |
| Core Unbound | Once at50%HP: armor compresses, cyan seams open, small plates separate and one pressure pulse spreads. Phase two keeps plates floating beside the exposed core. | Reposition during the transition; do not make it unavoidable damage. | Resume the same readable attacks with exposed seams and stronger core energy; this visual pass preserves timings and safe spaces. |

Suggested first gameplay pass: these three distinct attacks plus the half-health transition. Keep the charge only if it adds a clear wall-crash opening; otherwise it competes with the faultline for the same distance-control role. Retire a redundant generic circular shockwave instead of stacking six noisy patterns.

## VFX and sound direction

Owner rejected the draft as too rocky and asked for both phases to match the character. Native pixels now use colors sampled from the Warden sprite: armor #2a1836 / #4a2757, horn gold #ffd08a and core cyan #2ed1cd. Broad gray rubble became smaller violet armor chips (about half the normal particle count), less mist, narrower armor fins and cyan seams. Effects still start at hands/feet and travel into the ground. Gold stays on selected hard edges; cyan carries the magic. Use one short impact hold (the preview uses65ms) and a small camera kick, then let recovery read. No broad red circles, constant core flashing or whole-screen white frames. Reduced mode keeps essential armor fins/cracks and removes shake, speed echoes and extra particles.

Phase I contains the energy in sealed armor. Phase II adds floating armor fragments, brighter chest seams and stronger cyan in each attack without changing its path, impact time or recovery. The Core Unbound tab stages phase I to II; phase buttons are disabled for that transition and restored for attack tabs. Per-frame chest anchors measured from the original cyan sprite pixels keep the glow attached during authored poses. No new sprite sheets were generated.

The preview uses current Warden sound WAVs, not a new sound bank. Future sound work should reinforce stone strain, a short pressure dip, the impact and a restrained core tail. New voice lines are not needed to judge the attack motion.

## Preview and checks

Open `index.html` through the existing local preview server. Modes can be paused, replayed, slowed, scrubbed and switched to reduced effects. Staged slime movement illustrates the intended answer; it is not proof of live collision or AI behavior. Existing sprite poses are reused: final animation work may need attack-specific hand/torso frames.

Checked seven variants (three attacks in each phase plus the transition) at eight timeline samples each: all eight frames differ and reduced effects change each variant. Phase I/II visuals differ, while staged player paths remain equal. Transition crosses from I to II; phase controls disable/restore correctly. Visually inspected both phases and checked the per-frame core placement. UI phase button selects phase II, no horizontal overflow, no errors captured during verification; five audio cues decode with AudioContext running. Compared original player-motion functions and impact-hold function byte-for-byte: unchanged. `verify.js` contains the repeatable browser self-check. Physical speaker quality and live encounter balance remain unverified.

No game code/build or save writes in this draft. Runtime physics, damage timing, front-arc collision, path collision, boss-specific parries and manual playtesting must follow design selection.
