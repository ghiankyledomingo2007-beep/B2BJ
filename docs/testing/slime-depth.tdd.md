# Slime combat depth — 2026-09-08

Source: the owner's approved conversation plan: differentiated Tide Wave, readable
M1 finisher, temporary corpse traits, reviewed pixel art and collision-safe physics.
This is a focused slime pass, not a claim that the placeholder campaign is final art.

## Gameplay contracts

- **M1:** two opposing single-target cuts followed by a two-damage finisher. The
  finisher has its own directional body pose, water sprite, release/impact sound
  and dissolving impact. Back-dash countershots and the damaging dash remain.
- **Tide Wave:** two damage, 4.5-second cooldown, 280-unit nominal travel, finite
  lifetime and drag. The front is wider across its travel direction; it passes
  through ordinary bodies, hitting/pushing each once. A raised knight shield,
  boss body or terrain stops it. Per-target clear-water-line checks prevent
  reaching through cover. Broad rendered spray is clipped out of solid terrain.
- **Iron Membrane:** ordinary guard/knight corpse grants 10 gameplay seconds of
  protection; the next accepted hit loses half a heart less and consumes it.
  Invulnerable/god-mode contacts do not consume it. It cannot stack with Blade defense.
- **Jet Current:** ordinary spitter/shaman/hexer corpse grants 10 gameplay seconds
  of 1.3x projectile speed. Damage, range and attack cooldown remain unchanged.
- Only one trait is active; a different trait replaces the old one. The existing
  0.8-second interruptible absorption channel must complete. A useful new trait
  permits eating at full health/Ichor; an identical fresh trait alone does not.
  Summoned enemies cannot supply traits. Traits expire, clear on transformation/
  death, and are not checkpoint progression. Pause freezes their gameplay timers.
- Existing authored Outpost groups already mix guards, remnants and spitters;
  this pass adds reasons to use those encounters, not another enemy roster.

## RED/GREEN evidence

Relevant targets use the repository's Java 17 assertion-program runner:
`javac --release 17 -Xlint:all -d <temporary-classes> src/*.java test/<target>.java`,
then `java -ea -Djava.awt.headless=true -cp <temporary-classes> <target>`.

| Journey / guarantee | Target | RED checkpoint | GREEN checkpoint |
|---|---|---|---|
| Tide reaches several bodies across 8 headings, never repeats a hit, preserves focused M1 and cover | `TideCrowdTest`, `TideFeedbackTest` | `a2e5368`: target 0 outside old narrow front failed | `fc2da8e`: both targets plus `WaterCombatTest` and `SlimeComboTest` passed |
| Completed swallow grants a finite, non-stacking identity-specific benefit | `AbsorbedTraitTest` | `d334ccf`: missing `Player.Trait`/trait API | `03732b8`: trait tests plus absorption, progression and water combat passed |
| Finisher cue occurs at release; animation routes by facing; trait overlays preserve face/HUD; water stays on a 2x pixel grid | `SlimeDepthRenderingTest` | `136e59a`: absent finisher release cue | `26d2675`: rendering, water, Tide and audio targets passed |
| Finisher has motion margin without enlarging the body or moving its feet | `SlimeDepthRenderingTest` | `0419f73`: old 48px atlas failed the required margin | `b730fc1`: all 24 poses pass margin, scale and ground-anchor checks |
| Jet benefit is readable with the actual bitmap font | `SlimeDepthRenderingTest labels` | `e680597`: unsupported `+` / `%` left ambiguous text | `b730fc1`: supported `SHOT SPEED 1.3X` text passes the live HUD pixel check |

The release test also exposed floating-point residue in the shared cast countdown;
a near-zero clamp fixes one-step late releases across the common path. Impact
feedback is anchored at the actual event location, not the moving player.
The final full build also caught a stale `MotionPolishRenderingTest` fixture
injecting the superseded crest instead of the new Tide front. The corrected
fixture checks both current and fallback sheets without weakening its exact
forward-facing marker assertion; the targeted effects check passes.

## PixelLab art review

Generated through the existing scoped PixelLab MCP helper, following the
[official tool guide](https://api.pixellab.ai/mcp/docs). Credentials remain outside
the repository. Raw-image jobs are account-scoped; download URLs are public by
unguessable job ID and expire, so accepted pixels are packaged locally.

Five effect atlases have eight reviewed frames each. The finisher body atlas has
three directional eight-frame rows (south, west, north; east mirrors west).
All use native 2x pixels. The 64px finisher canvas adds an 8px transparent margin
around the existing 48px character, preserving its original scale and ground line.
Returned frame 0 is the input seed; runtime uses generated frames 1–8.

| Runtime asset | Base / animation job IDs | Review decision |
|---|---|---|
| `assets/effects/tide_front.png` | base `552b5817-1d96-4203-b72d-2e0855ef9097`; loop `3e41dd39-a2d0-422b-8e70-13528502ba57` | Open curved water front; restrained teal palette and foamy edge |
| `assets/effects/water_finisher.png` | base `cf5f1941-808d-4d80-ae70-bf6313374fa4`; loop `bb77643e-2329-477b-aed8-41582d46226e` | Distinct crossed liquid cuts, not resized M1 art |
| `assets/effects/water_finisher_impact.png` | `b15d0330-8846-4c4d-b35a-fe7490ceea89` | Cross breaks into foam and separated droplets; plays once |
| `assets/effects/guard_membrane.png` | base `ddb82bf9-a3e1-43d8-b27e-800b22aa8daa`; loop `b5ac3134-4101-433b-ac22-11ecbcec5f59` | Two pulsing armor scales; open center; painted behind body |
| `assets/effects/spitter_current.png` | base `8814c26c-0b8e-4415-a028-bbe62d65b90f`; loop `3e964e17-dc9b-405a-8134-04231c75e4a5` | Liquid currents orbit behind body; readable without hiding face |
| `assets/characters/slime/slime_finisher.png` | south `79fa7148-9c4a-4169-8751-6fc9ec279b8f`; west `1813fc20-d68d-47b2-9bf2-f7d0b4d68320`; north `da994607-761d-4175-83af-f5426d70f337` | All 24 poses remain unclipped and planted at native row 55; each directional settle pose exactly matches the original character pixels |

Rejected/superseded candidates, not used as final art:

- `ac6a5c78-00c0-406f-91c6-707f9c881410`: wave looked like a portal/torus.
- `6ff1494c-5419-4a0b-89dc-f9d369ba430b` and `1a41c137-1922-4954-a3e1-39087ecd6b22`: solid overlay centers.
- `6c28d0c0-e948-4c64-98de-d463765fbd86`: Jet overlay too sparse to read.
- 48px finisher casts `9a797b6f-6b85-44d5-9e0e-4baa90994afe`, `8e2f4ba2-842f-4641-bd95-cd4b7e8557ac`,
  `6fa26569-4a49-4621-8ae3-4714267b23eb`, and side retry `e68f8b91-cc3f-402d-831f-8047ca8be89b`:
  tight canvas caused side crest/grounding problems; superseded by transparent motion margin.

Source prompts: [first bases](../art-review/slime-depth/bases.json) and
[second bases](../art-review/slime-depth/bases-v2.json). Body prompts preserve the
exact reference identity, native scale, direction and planted ground line;
timing asks for coil, release, elastic recoil and return to the original stance.
This pass used 20 generation jobs, including rejected/replaced candidates, and
integrated 64 reviewed runtime frames. After adding the owner's latest backup,
the live balance check showed 88 generations remaining across four scoped keys
(0 + 8 + 40 + 40). This is a dated snapshot, not a permanent allowance guarantee.

## Verification / known limits

- Mechanics-stage full `rtk proxy bash build.sh`: PASS, before final margin revision.
- Final `rtk proxy bash build.sh`: PASS, all 82 test programs; Java 17-compatible
  compilation with `-Xlint:all`. The verified JAR replaced `build/B2BJ.jar`.
- Actual packaged-JAR rerun from `/tmp`: PASS for `SlimeDepthRenderingTest`,
  `AbsorbedTraitTest`, `TideCrowdTest` and `MotionPolishRenderingTest effects`.
  These used the JAR's classes and embedded art, not loose source-tree assets.
- Integrated captures use `tools/PreviewSlimeDepth.java` with an in-memory campaign;
  no user save is read or changed. Reviewed final trait, Tide and finisher captures,
  including the new margin-corrected body poses and supported Jet description.
  Both attack journeys defeated their target; fixture player health remained 5.0.
- Existing 50 named admin/gameplay and 40 enemy-facing scenarios remain part of
  the full build. New water rendering exercises five kinds across eight headings.
- No coverage instrumentation is configured; no numeric coverage percentage claimed.
- Physics here means swept collision, finite projectile momentum/drag, terrain
  clipping and bounded knockback, not a full fluid simulation.
- No XP tree or enemy-morph system added. Human difficulty/fun testing remains
  necessary; automated fixtures are not a complete unassisted playthrough.
- No third-party configuration edits or pushes. Budget preference saved through
  knowledge-ops in private project memory; no creative budget cap for needed art,
  but live provider balances remain finite and must be checked.

Reproduce the isolated visual journey after building (run from the repository):

```sh
rtk proxy bash -c 'set -eu
preview_dir=$(mktemp -d /tmp/b2bj-slime-preview.XXXXXX)
javac --release 17 -cp build/B2BJ.jar -d "$preview_dir" tools/PreviewSlimeDepth.java
java -ea -Djava.awt.headless=true -cp "build/B2BJ.jar:$preview_dir" PreviewSlimeDepth "$preview_dir/scenes"'
```
