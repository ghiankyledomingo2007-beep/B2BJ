# Ruined Outpost: map and environment art audit

Date: 2026-09-07. Audited runtime baseline: `3a4c264`.

## Verdict

The outpost is mechanically connected, but it is not yet a visually authored military compound. Most objects are plausible individually; their repeated placement, incompatible ground bases, inconsistent detail density and missing architecture make them feel assembled from separate asset packs.

**Keep the twelve connected outdoor sections. Rebuild their shared terrain/boundary language before buying a larger prop library.** Preserve combat space; replace filler with small, purposeful groups. Do not turn every section into an enclosed room or scatter more objects to fill empty space.

This is an audit and proposed build plan, not an implemented redesign. No runtime files changed, generations submitted, credentials accessed, GitHub pushes made or running game disturbed.

## Scope and evidence

- Reviewed all twelve current maps, including Field Infirmary and Signal Tower. Later biomes have no implemented maps in this outpost build and are not certified by this audit.
- Generated fresh full-world fixtures through the actual `enterRoom` and `drawWorld` methods against the built JAR. Each uses four fixed-camera renders at the same simulation state; inspected every map overview below. These are staged layout views, not screenshots from an unassisted playthrough.
- Inspected the six obstacle prop types, four decoration types in their rendered contexts, plus native cart/rubble/shield/wall/barrel/cot assets. This is not an exhaustive animation-frame audit.
- Traced `RuinedOutpostMap.java` placement, terrain, links and footprints; `B2BJ.java` terrain, banks, barriers, dressing, signs, remains and depth rendering; `RuinedOutpostGame.enterRoom` encounter setup.
- Read GCD v1.0 sections 4.4 and 5.2, pages 16 and 18; visually inspected page 18 using the PDF skill. Source: `/home/ghiankylledomingo/Downloads/01-School/Game-Dev/Blob-to-Blade-Game-Concept-Document-1.pdf`. The GCD informs setting and art constraints, not a rollback of later owner-approved combat, character or engine decisions.
- Ran `RuinedOutpostMapTest`, `TerrainRenderingTest`, `DepthRenderingTest`, `ArenaTwistTest`: **all four passed**. These check selected connectivity, terrain, collision/depth and arena behavior; they cannot approve composition or certify all live gameplay scenarios.

## Highest-impact findings

| Priority | Finding and evidence | Decision |
| --- | --- | --- |
| P0 | All twelve maps are 1920x1152 with repeated rectangular bank segments. `drawBanks` makes these read as oversized panel walls, not irregular earth. | Replace the visual border with a coherent earth-bank/ruined-defence kit. Keep explicit readable blocking boundaries and broad road openings. Do not simply hide colliders. |
| P0 | Every non-start room uses the same central paving ellipse plus exit spokes (`stone`). Blue brick pavement has a wall-like reading; cracked/collapsed sections are not strongly authored. | Build one wet earth / flat worn flagstone / broken edge palette, then author different road and footprint shapes per area. Preserve 32px source tiles at 2x. |
| P0 | Terrain receives a dark overlay before props are drawn. Props do not receive that same pass. Bright grass/sand bases on cart, rubble and shield cache visibly sit like stickers on dark soil. | Review common environment values in-scene; replace baked grass islands with matching dark contact bases. Do not blanket-darken the player, Ichor or attack tells. |
| P0 | Seven lit brazier placements across six areas use bright orange flames. GCD reserves red-orange for danger; rubble gold highlights and ornate gold shields also compete with resource signaling. | Use unlit/damaged braziers for abandoned areas; remove nonessential placements. Keep any proposed checkpoint lamp subdued and neutral, below pickups in brightness. No decorative danger-red or Ichor-like glow. |
| P1 | `drawFieldRemains` repeats the same equipment at the same three coordinates in rooms 1–11 (one in room 0). Large surrounding sprites expose the simplistic shapes. | Replace universal scatter with a few authored fallen-equipment arrangements, specific to the failed defence or room purpose. No gore; distinguish scenery from edible corpses and loot. |
| P1 | The map has 22 barriers/primary props and 26 dressing placements, but most rooms have only two of each in opposing positions. Five carts, eight shield caches and six identical rubble heaps are spread widely. | Keep useful inventory; concentrate supplies where people used them. Remove decorative duplicates that explain neither function, route nor history. Counts are not completion targets. |
| P1 | Muster has two brick blockouts; Camp has a blockout with a bed-like overlay; Signal Tower has two upright rectangular blockouts and no recognizable tower. | Replace these with named-purpose landmarks. A taller sprite is not a substitute for a clear footprint and usable approach. |
| P1 | Gatehouse is another paved clearing with two braziers and no gatehouse silhouette. Its clear floor is valuable for the boss. | Frame the existing arena with ruined gate piers and perimeter architecture, keeping the fight floor and approaches open. No new central obstacle gimmicks in this art pass. |
| P1 | `PASS`/`HOLD` placards, procedural closure fences and identical gold-striped rest/dressing rectangles do not match the prop kit. | Build coherent gate/open-breach and interactable states. Keep lock state and interaction feedback legible; remove floating permanent instruction clutter only after visual replacements communicate it. |
| P2 | Paired barrels overlap vertically enough to read like stacks in the overview; cots are isolated outside any shelter/foundation. | Restage barrels as staggered stock groups with visible bases, and cots along a shelter footprint. Test both actor forms around them. |

## Shared visual contract for the rebuild

These are proposed production constraints grounded in the GCD, not claims that every current asset satisfies them.

- **Setting:** rain-soaked abandoned military defences at the kingdom entrance. Timber, slate, tarnished iron, faded cloth, mud; evidence of a failed retreat. No decorative shrine/treasure-room theme or later-biome asset dumping.
- **Camera:** consistent top-down 3/4 view with readable top surfaces. Match perspective and light direction to an approved in-scene reference sheet, not independent prompts.
- **Pixels and scale:** native 32x32 terrain tiles rendered at 64x64 world pixels. Props retain integer 2x rendering; choose their native canvas around the intended physical object. Current `authored()` multiplies coordinates by 1.5, while sprite art scales by 2—placement scale is not sprite scale. Do not enlarge all props or shrink characters to make mismatched assets fit.
- **Size references:** preview every selected prop beside both Slime and Rainoray, and arena architecture beside Warden. Beds must accommodate the human silhouette; gates must read as traversable openings; barrels must remain supplies, not towers. Final visible bounds and feet matter more than PNG canvas size.
- **Palette:** dark navy/slate backgrounds, muted grey-green mud and brown timber. Cyan/teal remain player-associated; gold stays resource-associated; red-orange stays danger-associated. Dull metal may have restrained warm accents but must not resemble a collectible.
- **Detail:** a few deliberate pixel clusters and restrained highlights. Neither bright miniature dioramas with grass pedestals nor large single-color blockouts. No baked checkerboard, painted blur or smooth scaling.
- **Grounding:** consistent contact shadows; separate upright solids from low walkable debris. Author collision and depth anchors together with art. A cart/brazier blocks at its base; a tall canopy must not block at its full image rectangle.
- **Composition:** one clear room landmark plus supporting evidence, with quiet ground around combat. Reuse materials and modular pieces, not identical corner arrangements.

## All twelve areas: keep, remove/replace, build

The linked previews show the current state. Proposed landmarks below are new design work, not existing features or exact GCD requirements.

| Area / current preview | Keep | Remove, relocate or replace | Intended build |
| --- | --- | --- | --- |
| [00 Rain Ditch](00-rain-ditch-overview.png) | Safe awakening, two bare trees, unobstructed eastern approach. | Unmotivated bright rubble pile and isolated cart; straight brick-road tongue and boxed banks. | A shallow drainage depression with wet reeds/mud and a subdued trace of fallen-knight Ichor. One abandoned wheel/cart remnant only if it explains the retreat. Make the ditch visible without adding water hazards. |
| [01 Broken Palisade](01-broken-palisade-overview.png) | Two palisade pieces, first simple fight, clear east-west route. | Two detached identical fence islands; ornamental shield display and bright rubble. | An interrupted defensive line with one obvious breach, snapped inward-facing stakes and a small fallen-equipment group showing where the line failed. Keep enough bypass space for dodge practice. |
| [02 Muster Ground](02-muster-ground-overview.png) | Broad first mixed fight and southern optional route. | Two arbitrary masonry blockouts; corner cart/cache that do not establish a muster area. | Worn drill/parade ground, a damaged weapon rack at the margin and modest formation/boot-wear traces. Use a broken sign or pennant to distinguish the infirmary branch without blocking it. |
| [03 Abandoned Barracks](03-abandoned-barracks-overview.png) | Cots and five-enemy encounter space. | Isolated outdoor beds in opposite corners; lit decorative fire. | Roofless barracks foundation along one edge, grouped bunks, torn canvas/roof debris and personal equipment. Suggest the building with low walls and posts, not a roof hiding combat. |
| [04 Standard Yard](04-standard-yard-overview.png) | Standard as the main landmark; safe transformation area. | Spare ornate shield pile and orange brazier with no tutorial purpose. | A scarred flag footing with restrained fallen equipment facing it; open transformation ring of space, not a luminous magic altar. Preserve exit lock feedback until the existing tutorial condition completes. |
| [05 Supply Lane](05-supply-lane-overview.png) | Barrels and cart have a strong reason to be here; current combat route. | Symmetric barrel pairs reading as stacks; unrelated bright rubble heap. | A broken supply train: cart adjacent to staggered barrels, a torn sack and wheel tracks along the lane. Show why supplies were abandoned. Keep cover bases honest and avoid a new choke point. |
| [06 North Watch](06-north-watch-overview.png) | Low stone defences and mixed ranged encounter. | Two disconnected wall samples; random fire/rubble corners. | A collapsed watch platform at the boundary, connected low parapet remnants and a clear viewing direction. Leave both exits and the ranged counterplay lane exposed. |
| [07 Captain Camp](07-captain-camp-overview.png) | Safe checkpoint function and quiet pacing. | Brick/bed blockout, separate generic gold rectangle, arbitrary shield cache and open-rain fire. | One legible rest station: bedroll beneath a torn lean-to, small command table and restrained personal standard. Integrate the interaction into the bedroll. If shelter looks dry, weather occlusion must support that claim. |
| [08 Inner Court](08-inner-court-overview.png) | Wider pre-boss encounter and low defensive materials. | Near-copy of North Watch with a swapped cart; unrelated cache. | A final defensive fallback line: connected partial wall ends, abandoned arms facing the gatehouse, more exposed paving. Stronger approach framing, not more clutter in the central fight. |
| [09 East Gatehouse](09-east-gatehouse-overview.png) | Clear boss floor, west approach, northern branch and forest exit. | Symmetric orange braziers and generic corner loot/rubble display. | Ruined gate piers and broken portcullis/hinge pieces at the eastern perimeter; damaged guard recesses at margins; a readable forest threshold after victory. No giant arch drawn over the player/tells. Keep Warden movement, summon routes and all dodge lanes clear. |
| [10 Field Infirmary](10-field-infirmary-overview.png) | Two cots, optional recovery and safe return. | Ornate shield cache; generic shared rest rectangle; unrelated cart unless used as a stretcher/supply cart. | A ruined treatment shelter with a medical chest, bandage bundle and one empty stretcher. Distinguish available versus consumed dressing visually. No decorative edible-body cues and no invented permanent healing service. |
| [11 Signal Tower](11-signal-tower-overview.png) | Optional backstory stop and a clear return route. | Two tall brick rectangles, rubble trophy and ordinary lit brazier. | A collapsed tower footing with broken ladder, fallen signal mast and extinguished beacon basket. A compact message/dispatch prop makes the story beat discoverable. No climbable tower or new vertical gameplay required. |

## Runtime prop family decisions

| Asset / family | Verdict | Treatment |
| --- | --- | --- |
| `barricade.png` | Keep silhouette; revise base and placement. | Weathered wood already fits the outpost. Add damaged ends/orientation pieces only where the authored breach needs them. |
| `wall.png` | Keep as stone reference; extend carefully. | Existing low wall reads better than procedural masonry. Need matching ends/corners and collapse fragments, not a new stone style for every room. |
| `cot.png` | Keep subject and proportions; harmonize. | Current relatively plain shading differs from the dense cart/cache art. Let final shared detail treatment settle this; place inside believable foundations. |
| `standard.png` | Keep as yard landmark. | Muted cloth fits. Establish one emblem/faction language and reuse it on military equipment. |
| `supply-barrel.png` | Keep; restage. | Dull base, visible contact and non-overlapping layout. A fallen variant only if needed for the supply scene. |
| `dead-tree.png` | Keep sparingly. | Match base color to new terrain; do not use a forest density in the outpost. |
| `broken-cart.png` | Keep subject, revise rendering/base; reduce scatter. | Best home is Supply Lane. Secondary use requires a specific retreat or treatment story. Bright grass pedestal must go. |
| `shield-cache.png` | Replace most instances. | Ornate gold object reads closer to treasure/shop stock than fallen common equipment. Build dull, battered shield/spear group; reserve a meaningful emblem variant for the standard/captain. |
| `rubble.png` | Replace runtime candidate. | Gold cap and grassy miniature base read like valuable ore/decorative rock. Need irregular collapsed slate matching the wall kit, with no loot shine. |
| `brazier.png` | Keep source archived; remove most lit placements. | Unlit versions suit abandonment and danger-color rules. A future actual hazard may justify fire, but that needs explicit mechanics, not an incidental art change. |
| Procedural banks/brick blocks | Replace presentation. | Shared boundary kit and purpose-specific structural ruins; preserve valid solid footprints. |
| Procedural remains | Replace universal stamping. | Small authored groups tied to each area's story, muted and visibly noninteractive. |
| Procedural gate/sign/rest graphics | Replace with functional state art. | Gate open/closed, checkpoint active, dressing available/used; keep feedback and collision synchronized. |

Unused concept sheets and the legacy `assets/props/ruined_outpost/ruined_stone_pillar.png` are not evidence of current placement: this map's prop enum does not use that pillar. Archive is not runtime approval. Do not delete source assets during this plan.

## Build sequence and generation queue

1. **Lock one reference scene.** Use Broken Palisade to approve the terrain, wall/timber materials, palette, contact bases and player scale together. Existing wall, palisade, standard and both player forms are the references. Do not generate twelve unrelated scene packs.
2. **Shared foundation first.** Wet mud, worn flagstone and transitions; irregular earth-bank edges/corners; matching ruined wall ends; grounded broken wood/rubble. Reuse the current tileset plumbing and integer rendering. Review tile seams and repeated patterns across a scrolling viewport.
3. **Correct the reused objects.** Dull shield/remains set, cart base treatment, unlit brazier, gate states and readable bedroll/dressing items. Restage existing barrels/cots. Reject bright grass pedestals, unintended gold loot cues and off-angle candidates before integration.
4. **Author the twelve layouts.** First Rain Ditch–Standard Yard, then Supply Lane–Inner Court, then the two optional rooms and Gatehouse framing. Each layout gets its planned landmark and clear entry/exit composition. Retain outdoor openness and existing encounter roles while testing changes incrementally.
5. **Generate only missing landmark pieces.** Roofless barracks/lean-to components, collapsed watch/tower mast and beacon, command/rest items, medical chest/stretcher, ruined gate piers. Prefer shared timber/stone modules. No new character generation, animation overhaul, music work or whole new biome in this pass.
6. **Validate states and movement.** See gates below. Only then consider restrained ambient animation. More flames, particles and motion are not a substitute for coherent ground and architecture.

Generation allowance was last verified in the preceding rain pass as **27 remaining**. No provider balance request or spending occurred in this audit; shared usage may have changed it. Recheck before starting. Do not assign an invented exact generation count to this plan: candidate rejection and service costs vary. Generate small reviewed batches from one reference, not the whole allowance blindly.

## Acceptance gates before calling the art pass complete

- Hide area-name UI: each section should still suggest its purpose through its landmark and composition.
- Every object has a job: gameplay boundary/cover, navigation, room function or environmental story. Otherwise remove it from placement, not necessarily from archives.
- Compare every new asset against the reference scene at native 2x, next to Slime and Rainoray; check Warden clearance separately.
- Test front/behind/both sides of standing props and gate segments. No actor sitting on top of a fence, floating feet, invisible full-image colliders or hidden exits.
- Test full actor-radius paths at all exits, locked and unlocked, plus projectiles, dash and enemy pursuit around moved objects. Preserve the exact visible-versus-solid distinction for low debris.
- Play Warden charge, sweep, shockwave and fissure with the new perimeter: unobscured tells, usable dodge space, safe summons, no snagging on architecture. Headless screenshots alone cannot certify this.
- Check Camp and Infirmary before/after interaction; a consumed resource must not look available. Shelter requires a weather mask if it implies dry space.
- Review scenery with pickups and danger tells present: gold resources and red danger must remain immediately distinguishable from props.
- Inspect all twelve maps at normal gameplay viewport size while scrolling, not just the overviews. Repeat with reduced effects; measure frame pacing on the user's laptop after larger scenery changes.
- Run the full regression suite after implementation. Existing placement-count tests may need intentional updates, but retain their reachability, grounding and gameplay invariants rather than deleting failing checks.

## Reproduce these audit captures

`MapAudit.java` is an evidence-only headless renderer, not runtime code. Compile against a fresh build and write captures into a temporary folder:

```bash
rtk proxy javac --release 17 -cp build/B2BJ.jar -d /tmp/b2bj-map-review docs/testing/map-art-audit/MapAudit.java
rtk proxy java -Xmx256m -Djava.awt.headless=true -cp build/B2BJ.jar:/tmp/b2bj-map-review MapAudit /tmp/b2bj-map-review/captures
```

The twelve checked-in overviews are half-size for layout comparison, not evidence that the game renders at a changed resolution. The renderer also emits full-resolution world captures when rerun. No game session input is sent.
