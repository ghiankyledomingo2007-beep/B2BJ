# Development notes before the October 2026 folder cleanup

Historical README snapshot. Its build scope, art descriptions and test counts belong to earlier passes. Use the current project README and FILE_MAP for navigation.

# B2BJ — Blob to Blade

Java 17 action adventure with a focused **Ruined Outpost presentation build**.
Rainoray wakes as a cyan slime among fallen knights; their Ichor grants a temporary Blade form.

Ruined Outpost is a continuous 6144×4096 exploration map with twelve named landmarks,
alternate paths, optional discoveries, a safe camp and the Outpost Warden.
Normal encounters do not lock travel.

Player sprites and selected effects use reviewed PixelLab art. The Outpost itself — floor,
forest edge, walls, palisades, landmark set pieces, fires and night lighting, and the
Warden's fight effects — is pixel art drawn by code in `tools/art/` (see its README; one
command rebuilds it, hand retouches are kept). Some enemies and UI still use code-drawn
placeholders; this is not a fully tuned release. The game runs offline without API keys
or paid services.
See [FILE_MAP.md](../../FILE_MAP.md) for the presentation guide and file responsibilities.

## Build and run

Requires a JDK 17 or newer. No downloaded dependencies.

```bash
./build.sh
java -jar build/B2BJ.jar
```

Or run `./run-game.sh` to verify, build, and launch.
Rebuilding does not update an already running game window.
If Java starts but the desktop never displays its window, use
`./run-game.sh --borderless` (or `java -jar build/B2BJ.jar --borderless`).
This bypasses native window decorations; use Alt+F4 to quit. The game stays
1280×720, and its on-screen Menu still provides Settings and Admin / Testing.

## Laboratory 6 Enemy AI

```bash
java -jar build/B2BJ.jar --lab6
```

A separate demonstration uses B2BJ's slime and remnant sprites with the instructor's
300 px detection range, 80 px stopping distance and 0.08 px/ms chase speed (80 px/s).
Move with WASD/arrows. Keys 1, 2 and 3 set up the outside-range, chasing and nearby
cases; Space pauses, R resets. It never opens a campaign or touches save data.
Normal gameplay keeps its existing combat AI.
See [the demonstration guide](../testing/lab6/README.md).

## How the code works

`B2BJ.java` starts the Swing window and runs one update step about every 16 ms.
It sends keyboard and mouse input into `RuinedOutpostGame`, then paints the current
state through `CampaignRenderer`.

`RuinedOutpostGame` owns the live state: player, enemies, Warden, projectiles,
Ichor drops, checkpoints and game phase. `RuinedOutpostMap` supplies the Outpost
terrain and collision checks. `Player`, `Wisp`, `Guardian`, `WaterProjectile` and
`EnemyProjectile` each own their movement or combat rules.

The renderer reads that state and draws the world, actors, effects, HUD and map.
Images are loaded from `assets/`; the build copies those assets into the JAR, so no
runtime dependency download is needed. `CampaignSave` writes the checkpoint to
`~/.b2bj/campaign.properties` with an atomic file replacement.

For a guided walkthrough, open the CodeTour file in `.tours/`. For a quick file
reference, use [FILE_MAP.md](../../FILE_MAP.md).

The [masked motion pass](../art-review/human-motion-fix/README.md) documents the
current Rimuru mask, four-direction motion, click combos and charged attack.
The [human form rebuild](../art-review/rimuru-human/README.md) records the earlier unmasked design. The older
[Rainoray pass](../testing/rainoray-rework/README.md) records the previous masked
design, skills, whole-body feeding and arena fissures.

## Controls

- Click **Menu** at the top-right for **Resume**, **Settings**, **Admin / Testing**,
  and **Return to Title**. Settings and testing controls open inside the menu;
  **Back** returns to its main page. **Close**, **Resume**, or Esc returns to play.
  Settings are also available from the title screen. Alt+G opens/closes Menu.
- Enter: new journey. C: continue from saved checkpoint. N explicitly confirms replacing an existing save.
- WASD / arrows: move; stopping keeps your last facing. Move the mouse while standing to aim Blade.
- Blob: hold left click for Water Slash. Blade: four short left clicks chain a cut,
  thrust, rising cut and overhead finisher. One click requests one move.
- Blade: hold left click to charge; release for a blue cleave (10 Ichor). Full power
  at 0.8 seconds; dash cancels charge. Hurt, pause and focus loss clear stored input.
- Right click: Tide Wave (Blob, 4.5-second cooldown) / Ichor Crescent (Blade, 5 seconds).
- F: Riposte (Blade), a 0.3-second one-hit parry with a 6-second cooldown.
- Space: dash, with brief invulnerability. Blob dash damages enemies.
- Q: transform at 100 Ichor.
- E: read discoveries, absorb nearby remains in Blob form, or use the Outpost gate.
- H at a safe camp: heal, set checkpoint and save.
- 1–4 at camp: buy Vitality, Capacity, Efficiency or Edge with Ichor Shards.
- Esc: pause. Focus loss pauses and releases held input.
- T while paused: save progression and return to title. Continue starts at the checkpoint, not mid-combat.
- Hold Tab: Outpost map; stays live during play or remains paused from pause.
- M: sound toggle. V: reduced screen effects.
- O: audio/effects options (master, SFX, music volumes). Settings last for this session.
- F1: testing menu during a live campaign. God mode, unlimited Blade form, one-shot attacks, refill/cooldown reset,
  +25 shards, travel to any camp/boss and reset the current region's encounters.
- R: reform at the Outpost checkpoint after death; return to title after completion.

Health pips and segmented gold Ichor are top-left; Blade timer top-centre; form
top-right; local terrain minimap bottom-right. Blade lasts 12 seconds before
pickups, deals 4× Blob damage, moves at 80% speed and takes 75% incoming damage.
Taking damage in Blade form also costs 5 Ichor; reversion has 1.5 seconds of vulnerable recovery.
Crescent and Riposte each spend 8 Ichor from the remaining Blade timer; they require
more than 8 remaining so the ability has time to exist before reversion.

Testing cheats are off by default. Opening Menu or F1 does not enable cheats; using any cheat marks a
test session and prevents saving to both disk and the normal Continue snapshot.
Switching cheats off does not resume saving. To restore normal play: use Back and
Return to Title in the menu, then C at the title. New Game explicitly replaces the save.
God mode blocks damage, not the Blade timer; one-shot still requires a connected hit
and cannot pass through walls. **Menu > Admin / Testing > Unlimited Blade form** refills
Ichor: resume and press Q to transform. The timer, skills and incoming hits cannot drain
Ichor while enabled; the HUD reads UNLIMITED. Health still takes damage. Switching it off
resumes normal drain. The setting survives testing travel/reset and death, but resets
on Continue or New Game. Boss travel/reset respawns encounters in the test
session only. See [50-scenario admin audit](../testing/admin-audit.md).

Blob passively regenerates 0.6 Ichor/sec. Absorption takes 0.8 seconds standing
still and grants 5 Ichor plus half a health pip; boss summons cannot heal you.
Movement/actions/damage interrupt absorption. Persistent tutorial text has moved
out of gameplay; controls remain in pause. See the [HUD/recovery report](../testing/hud-40/README.md).

Slime combat now gives each move a different role. Holding LMB chains two opposing
Water Slashes into a stronger single-target finisher. A timely shot after a backward
dash is a quick countershot. RMB Tide Wave has a broad front that travels through
groups, hitting each enemy once; terrain and raised knight shields still stop it.
Water keeps finite range, drag, swept collisions, stagger resistance and collision-safe knockback.

Swallowing an ordinary guard/knight grants **Iron Membrane**: absorb half a heart
from one incoming hit. Swallowing a spitter/shaman/hexer grants **Jet Current**:
water projectiles travel 30% faster, with unchanged damage, range and attack recovery.
Each trait lasts 10 gameplay seconds; only one can be active, and transformation
clears it. Boss summons grant neither trait. The corpse prompt previews its trait;
the HUD shows the active benefit and countdown without replacing health or skill slots.
See the [slime-depth verification and art review](../testing/slime-depth.tdd.md).

## Verification and scope

`build.sh` compiles with Java 17 compatibility and all lint warnings, runs the
assertion-based tests, and creates a self-contained asset JAR. No numeric
coverage claim is made.

Checks include terrain pixel scale, section connectivity, collision reachability,
combat gates, directional attacks, dash immunity, Ichor, pause/focus handling,
checkpoint recovery, story transitions, packaged recordings, the audio mixer and headless rendering.
Standing props and actors now sort by ground contact. Reviewed PixelLab palisades,
cots, military standard and low walls render at native 2× with fitted footprints;
regression checks cover Blob/Blade occlusion and all four approach directions.
The latest reviewed batch adds grounded supply barrels and bare trees, an eight-pose
Blob idle, dissipating teal hits and a clearer Warden wind-up. See the
[27-generation review](../art-review/outpost-batch-27/README.md) for accepted,
staged and rejected candidates; raw generations are not automatically packaged.
The [Blade direction pass](../art-review/blade-direction-pass/README.md) integrates
eight-pose front/back cuts with unchanged body scale and corrects side mirroring.
The [combat repair batch](../art-review/combat-repair-40/README.md) now adds two
reviewed side-attack sequences, body-ripple casting, helmeted remnant actions and
Warden movement/attacks. Player/remnant native 2× body scale is preserved; rejected candidates
remain outside the packaged assets.
At the owner's request, Blob's close-range jelly punch is removed. Water Slash
deals one damage with 0.48-second recovery; Tide Wave deals two with a 4.5-second
cooldown. Both have windup, finite travel, drag, wall/gate impacts and collision-safe
enemy knockback. Blade retains its melee cuts. This is an explicit change from the
GCD's original Blob attack, not a claim that the document already specified water skills.
See [water pass](../art-review/water-slash/README.md) for art and balance checks.
The route test positions actors deliberately and uses an end-of-boss fixture;
it is not evidence of a complete unassisted playthrough.

The art reports below describe historical outpost-only passes and their then-current test/credit counts.
The legacy twelve-section outpost constructors remain available to regression tests;
the desktop launcher starts the focused Ruined Outpost campaign.

See [GCD audit](design/gcd-alignment-audit.md) for historical findings.
See the [combat repair report](../testing/combat-repair-2026-09-06/README.md) for
26 regression programs, fresh-route observations and adaptive boss scenarios.
The [official PixelLab guide snapshot](../reference/pixellab-mcp-guide.md) was
read and saved on 2026-09-06; this pass exhausted its 40-generation allowance.
The subsequent [immersion pass](../testing/immersion-40/README.md) adds magnetic
animated Ichor, distinct sprite-based hurt/pickup effects, a ranged Spitter and
the owner's requested larger3× Warden. All34 Java checks pass; this new key's
40-generation allowance is fully consumed. See its [art review](../art-review/immersion-40/README.md).
The campaign saves the Outpost checkpoint, shards and upgrades to
`~/.b2bj/campaign.properties` using atomic replacement. Invalid saves are preserved
and reported; N is an explicit new-game overwrite. Enemies respawn after reloading or
death, but permanent progression stays. Automated fixtures refill health/Ichor, so this
does not establish unassisted difficulty or release-quality balance. Final animation/art,
controller support, remappable bindings and longer human playtesting remain.

Windows release workflow is configured but has not been run in this pass.
Checkpoint `446420e` was pushed to the owner's personal GitHub on 2026-09-07;
its verification workflow passed. No release tag or installer publication was made.
