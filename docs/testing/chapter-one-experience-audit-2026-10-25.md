# Chapter One experience audit — 2026-10-25

Verdict: a stronger foundation, but not a finished first chapter. The best material is Rainoray's unfinished duty, Mara's lamp and the Warden's obsolete order. The current route does not reliably connect those ideas to what the player does. More decoration alone will not resolve that.

Read the current campaign implementation and `docs/design/area-one-story.md`. Evaluated the open-world campaign, not the legacy room tutorial. Reproduced selected states against the current packaged game using an isolated, save-free Java harness. This is a narrative/mechanics audit, not a timed human playthrough.

## Priority findings

| Priority | Player scenario and evidence | Likely experience | Smallest useful improvement |
| --- | --- | --- | --- |
| High | Follow every main objective: camp → Muster → Standard → Inner Court → Warden. None points toward a survivor or Rainoray's delivery. `outpostWaypoint()` only follows landmarks; the map has no discovered survivor/key/tin markers. | The player can experience most of the chapter as road fights, missing the reason to care about the outpost. | Place a visible/audible rescue opportunity beside the first route, with a short Mara hint. Keep it optional. Use the existing survivor and camp systems. |
| High | Rescue all three, then meet the Warden: Rainoray still says Mara is the only one left. Go straight to the Warden before meeting Mara: the same line names her anyway. Both reproduced. `OutpostDialogue.WARDEN` is fixed. `CAMP_RESCUED` also always describes a fence even if Ilse was rescued from thorns first. | The story ignores choices the game explicitly supports. | Choose a few lines from existing met-Mara/rescued flags. No new branching quest framework needed. |
| High | Carry the cache key, return to title, Continue: it goes back to the original storehouse. Reproduced. `enterBiome()` clears carried/dropped state; saves retain completed fields only. | A successful trip appears lost after a normal save/quit. The key is recoverable, so this is not a permanent softlock. | Persist the carried object's stable ID (and dropped state if intended), with safe validation and old-save defaults. |
| High | Die with a camp checkpoint: all 21 ordinary enemies respawn, Ichor becomes zero, and rest only heals HP. Reproduced. New braziers must first be lit with Blade attacks; no checkpoint sits directly at the gate. | A learner may spend retries rebuilding resources and repeating the approach instead of learning the boss. | Make a clear pre-boss retry point and define a consistent retry resource baseline. Preserve successful rescues, upgrades and discoveries as now. |
| Medium | Find a cold brazier or thorns while low on Ichor. Transform needs 100; rest does not refill it. Passive-only refill from zero is 166.7 seconds, while base Blade duration is 12 seconds. Enemy drops and absorption shorten this substantially. | A curious player may wait or search for enemies merely to use an environmental interaction; using Blade too early feels wasteful. | Provide an intentional first transformation opportunity and a resource source near form-dependent exploration. Do not simply extend every combat transformation without balance testing. |
| Medium | Three supply tins, one carry slot, each delivered to the same camp for one shard. Straight-line walking baselines for separate camp round trips: 17.3, 17.2 and 28.7 seconds; 63.3 total before walls/combat, with no dash. Real combined exploration routes differ. | Repeated delivery can feel like low-value errands after the route has already been cleared. | First tin should demonstrate a meaningful camp change or recovery benefit; make later deliveries fit a natural return route. Avoid adding more scattered collectibles. |
| Medium | After the three introductory solo threats, five landmarks each use three enemies with the same small offset formation and rotating kinds. | The next named location can feel like another version of the previous fight. This is a design risk, not a demonstrated player reaction. | Give existing groups different jobs: one ranged flank, one narrow guard position, one optional protected rescue. Reuse enemies; change encounter context. |
| Medium | Warden takes normal damage only in RECOVER, has 1,200 HP and periodically adds guards. The HUD does change color in recovery and the boss has recovery poses; failed hits outside that state do not produce an explicit armor response in `hurtGuardian`. | A new player can mistake invulnerability for unreliable attacks; an inefficient fight becomes prolonged dodge/wait/add cycles. | Make closed-armor hits visibly/audibly deflect and the exposed core unmistakable. Measure first-player completion time before changing HP. |

## What already works and should stay

- Rainoray's blankets-before-weapons delivery and Mara restoring his name give the chapter a human motive without a lore dump.
- Rescued people appear around camp; rescue and upgrade flags survive death. The report and three-beds scene already provide a payoff.
- First remnant, guard and spitter are introduced separately. Committed tells, attack-slot limits and varied Warden recoveries support learning.
- Dialogue freezes combat and resource drain. The Warden introduction is remembered, avoiding repeated full conversations after death.
- Optional exploration stays optional. Fix narrative ordering and discoverability instead of forcing every player through every side activity.

## Suggested chapter rhythm

Wake → Mara's lamp → first readable fight → spot/help one survivor using slime form → see camp respond → learn Blade against an obstacle → mixed encounter → discover the evacuation order → visible pre-boss rest → Warden → tracks into the forest, with an optional camp farewell.

This is a proposed arrangement of existing content, not an implemented route or a requirement to visit every stop. The main priority is one meaningful rescue and an easier retry loop, followed by dialogue/persistence fixes. More enemy count, another UI redesign or longer cutscenes would not solve these findings.

## Verification and next playtest

Scratch reproduction: `/home/ghiankylledomingo/tmp/b2bj-chapter-one-audit/ChapterOneAudit.java`; output: `results.txt` beside it. Assertions passed for boss-first dialogue, all-rescued dialogue, death/resource/enemy reset, and carried-key loss on Continue. Positions were set directly to isolate states, so these do not establish player route times or difficulty. The nearest existing brazier is about 4.8 seconds from the Warden in straight-line walking terms; its actual approach includes terrain and encounters. Do not describe every retry as a long walk.

Give a new player no spoken coaching. Observe: do they notice one rescue; understand why the Warden refuses them; understand when hits work; find a checkpoint; and return to a second boss attempt without doing unwanted chores? Ask which moment felt meaningful and which they wanted to skip. Record actual fight/retry time rather than treating passing tests as evidence of fun.

No runtime code, assets, saves or running game changed for this audit. No full manual playthrough or balance measurement performed.

## Sound reference shortlist requested during the audit

Checked The Sounds Resource on 2026-10-25. [Slime Rancher — Slimes](https://sounds.spriters-resource.com/playstation_4/slimerancher/asset/417569/) lists 227 files, including `bounce_01`–`04`, `damage_01`–`02`, `chomp_03`–`06`, `feral_bash_leap_01` and `feral_bash_land_01`. These are specific candidates to audition for movement, hurt, absorption and enemy pounces. Selection is based on file listings, not an audio audition.

The site's [reuse FAQ](https://sounds.spriters-resource.com/page/help/) says the assets belong to their original copyright holders and the site cannot license them. It encourages free fan projects but that is not a rights-holder license for B2BJ. Treat these as reference candidates until reuse permission is established; no archive audio was downloaded or added to the game. Current slime recordings already have separate source credits in `assets/audio/CREDITS.md`.

## Implementation follow-up — 2026-10-25

The accepted fixes are implemented and verified by all 101 test classes plus the packaged asset smoke check. See [implementation details and game captures](../art-review/chapter-one-experience/README.md). Dialogue playback is ready; voice recordings are a separate audition step. Full human pacing/balance playtesting remains outstanding.
