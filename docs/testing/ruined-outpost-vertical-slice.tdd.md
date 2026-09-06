# Ruined Outpost Vertical Slice — TDD Evidence

> Historical record of the discarded three-Wisp courtyard prototype.
> Its story, room design, boss health and completion claims are superseded by
> [the current GCD audit](../gcd-alignment-audit.md). Do not use this as current design authority.

Date: 2026-09-05

## Critical player journey

Wake at the mask altar, purge three Wisps, survive contact attacks, collect
100 Ichor, transform into the Blade, defeat the 12-health Outpost Warden,
open and cross the eastern gate, see the ending, then restart. Death must also
return to a clean opening state.

## RED / GREEN record

| Guarantee | RED evidence | GREEN evidence |
| --- | --- | --- |
| Player health, damage invulnerability, and death | CombatLoopTest did not compile: health API and constants were missing | CombatLoopTest passed |
| Altar, barricade, and gate collision | RuinedOutpostMapTest did not compile: obstacle and gate APIs were missing | RuinedOutpostMapTest passed |
| Ordered prologue, hunt, boss, escape, ending, and death states | OutpostStoryTest did not compile: OutpostStory was missing | OutpostStoryTest passed |
| 12-health Warden with telegraph, slam, recovery, hurt, and death | GuardianTest did not compile: Guardian was missing | GuardianTest passed |
| Distinct click-free sound cues that fail safely without an audio device | GameAudioTest did not compile: GameAudio was missing | GameAudioTest passed |
| Timed hit, hurt, transform, victory, and camera-shake feedback | CombatFeedbackTest did not compile: CombatFeedback was missing | CombatFeedbackTest passed |
| Three two-health Wisp scouts and Ichor drops | WispCombatTest did not compile against the configurable-health API | WispCombatTest passed |
| Complete encounter controller and victory critical path | RuinedOutpostGameTest did not compile: RuinedOutpostGame was missing | RuinedOutpostGameTest passed |
| Story overlay and health HUD render from live state | B2BJIntegrationTest did not compile: test-safe constructor and game access were missing | B2BJIntegrationTest passed |
| Wisp lunges respect visible map obstacles | WispTest did not compile: map-aware update and collision constants were missing | WispTest passed |
| Fatal contact emits one sound/flash event | RuinedOutpostGameTest failed: fatal contact must not duplicate sound and screen feedback | RuinedOutpostGameTest passed |
| Closed eastern gate cannot be bypassed | RuinedOutpostMapTest failed: closed gate wall has a bypass at y=24.0 | RuinedOutpostMapTest passed |
| Visible Warden body is solid until defeat | RuinedOutpostMapTest did not compile: Warden collision API was missing | RuinedOutpostMapTest passed |

## Final verification command

    rtk javac --release 17 -Xlint:all -d <clean-dir> src/*.java test/*.java
    rtk cp -R assets <clean-dir>/assets
    rtk java -ea -Djava.awt.headless=true -cp <clean-dir> <TestClass>

Result: **16/16 executable test programs passed** with assertions enabled and
no compiler warnings. A real Swing window also launched successfully on the
desktop, and the prologue/gameplay frames were inspected at 1280x720.

The direct-javac project has no line-coverage tool, so no numeric coverage
claim is made. Checkpoint commits were skipped because the working tree already
contains active owner changes; this document preserves the RED/GREEN evidence.
