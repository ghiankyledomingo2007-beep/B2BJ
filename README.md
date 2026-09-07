# B2BJ — Blob to Blade

Java 17 pixel-action prototype for the **Ruined Outpost**, the first biome.
Rainoray wakes as a cyan slime among fallen knights; their Ichor grants a temporary Blade form.

The outpost uses twelve connected sections (ten main, two optional), not twelve indoor boxes.
Each section is a scrolling 1920×1152 outdoor playfield. Paving, earth banks, broken
palisades, barracks and open road breaches define the compound. Cleared passages
can be crossed on foot. Room names and exact layout are implementation proposals.

## Build and run

Requires a JDK 17 or newer. No downloaded dependencies.

```bash
./build.sh
java -jar build/B2BJ.jar
```

Or run `./run-game.sh` to verify, build, and launch.
Rebuilding does not update an already running game window.

The [Rainoray / Ruined Outpost pass](docs/testing/rainoray-rework/README.md)
documents the masked human, new skills, whole-body feeding, arena fissures,
pixel-art warnings, credit ledger and verification limits.

## Controls

- Enter / click: begin.
- WASD / arrows: move. Mouse: aim. Hold left click: Water Slash (Blob) / cuts (Blade).
- Right click: Tide Wave (Blob, 4.5-second cooldown) / Ichor Crescent (Blade, 5 seconds).
- F: Riposte (Blade), a 0.3-second one-hit parry with a 6-second cooldown.
- Space: dash, with brief invulnerability. Blob dash damages enemies.
- Q: transform at 100 Ichor.
- Walk through cleared breaches; E also works near a passage.
- E: absorb nearby remains in Blob form, rest, use field dressing, or leave.
- Esc: pause. Focus loss pauses and releases held input.
- Hold Tab: explored-area map; stays live during play or remains paused from pause.
- M: sound toggle. V: reduced screen effects.
- R: restart after death/completion. After reaching Captain Camp, death reforms there.

Health pips and segmented gold Ichor are top-left; Blade timer top-centre; form
top-right; local terrain minimap bottom-right. Blade lasts 12 seconds before
pickups, deals 4× Blob damage, moves at 80% speed and takes 75% incoming damage.
Blade hits also cost 5 Ichor; reversion has 1.5 seconds of vulnerable recovery.
Crescent and Riposte each spend 8 Ichor from the remaining Blade timer; they require
more than 8 remaining so the ability has time to exist before reversion.

Blob passively regenerates 0.6 Ichor/sec. Absorption takes 0.8 seconds standing
still and grants 5 Ichor plus half a health pip; boss summons cannot heal you.
Movement/actions/damage interrupt absorption. Persistent tutorial text has moved
out of gameplay; controls remain in pause. See the [HUD/recovery report](docs/testing/hud-40/README.md).

## Verification and scope

`build.sh` compiles with Java 17 compatibility and all lint warnings, runs the
assertion-based tests, and creates a self-contained asset JAR. No numeric
coverage claim is made.

Checks include terrain pixel scale, section connectivity, collision reachability,
combat gates, directional attacks, dash immunity, Ichor, pause/focus handling,
checkpoint recovery, story transitions, sound synthesis and headless rendering.
Standing props and actors now sort by ground contact. Reviewed PixelLab palisades,
cots, military standard and low walls render at native 2× with fitted footprints;
regression checks cover Blob/Blade occlusion and all four approach directions.
The latest reviewed batch adds grounded supply barrels and bare trees, an eight-pose
Blob idle, dissipating teal hits and a clearer Warden wind-up. See the
[27-generation review](docs/art-review/outpost-batch-27/README.md) for accepted,
staged and rejected candidates; raw generations are not automatically packaged.
The [Blade direction pass](docs/art-review/blade-direction-pass/README.md) integrates
eight-pose front/back cuts with unchanged body scale and corrects side mirroring.
The [combat repair batch](docs/art-review/combat-repair-40/README.md) now adds two
reviewed side-attack sequences, body-ripple casting, helmeted remnant actions and
Warden movement/attacks. Player/remnant native 2× body scale is preserved; rejected candidates
remain outside the packaged assets.
At the owner's request, Blob's close-range jelly punch is removed. Water Slash
deals one damage with 0.48-second recovery; Tide Wave deals two with a 4.5-second
cooldown. Both have windup, finite travel, drag, wall/gate impacts and collision-safe
enemy knockback. Blade retains its melee cuts. This is an explicit change from the
GCD's original Blob attack, not a claim that the document already specified water skills.
See [water pass](docs/art-review/water-slash/README.md) for art and balance checks.
The route test positions actors deliberately and uses an end-of-boss fixture;
it is not evidence of a complete unassisted playthrough.

See [GCD audit](docs/gcd-alignment-audit.md) for remaining work.
See the [combat repair report](docs/testing/combat-repair-2026-09-06/README.md) for
26 regression programs, fresh-route observations and adaptive boss scenarios.
The [official PixelLab guide snapshot](docs/reference/pixellab-mcp-guide.md) was
read and saved on 2026-09-06; this pass exhausted its 40-generation allowance.
The subsequent [immersion pass](docs/testing/immersion-40/README.md) adds magnetic
animated Ichor, distinct sprite-based hurt/pickup effects, a ranged Spitter and
the owner's requested larger3× Warden. All34 Java checks pass; this new key's
40-generation allowance is fully consumed. See its [art review](docs/art-review/immersion-40/README.md).
The [Ruined Outpost environment pass](docs/testing/outpost-fable-handoff.md) (2026-09-07)
replaces the blue-brick blockout ground and panel banks with code-composed 32px Wang sheets
(`tools/ComposeGround.java`: worn flagstone road, wet mud, raised earth banks drawn as one
collider-exact shape with slope and shadow), re-authors all twelve layouts around one landmark
each (barracks foundation, watch platform, captain's lean-to, gate piers, infirmary shelter,
collapsed signal tower), retires lit braziers, gold caches, placards and the repeated remains
stamp, and adds 26 reviewed PixelLab props with mud contact bases. `tools/PreviewReference.java`
renders the reference scene, a scale sheet and viewport captures for review. Terrain and bank
sheets are procedural, not generated art; see that report for balances, rejections and limits.
This is **not a finished biome or a bug-free release**: boss pacing, final animation
art, music and longer live playtesting remain. Later biomes are not implemented.
Progress currently survives deaths within the running session, not application restarts.

Windows release workflow is configured but has not been run in this pass.
Checkpoint `446420e` was pushed to the owner's personal GitHub on 2026-09-07;
its verification workflow passed. No release tag or installer publication was made.
