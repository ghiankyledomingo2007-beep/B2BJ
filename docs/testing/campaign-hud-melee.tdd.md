# Campaign HUD and encounter repair

User journeys: preserve the approved health/skill interface in campaign play;
encounter actual ranged Outpost enemies; distinguish a guard's stationary bash
from a scout rush and wolf charge; read direction and attack phase visually.

RED checkpoint: `f9fa470`. Compiled the two new test programs against the previous
JAR. `CampaignHudRestorationTest` failed on the missing vitality asset in actual
campaign painting. `OutpostVarietyTest` independently failed missing spitter,
guard forward bash and scout attack-pose checks.

GREEN: both new programs pass after the fixes, along with `CampaignEnemyTest`
and `HudOverhaulTest`. `./build.sh` then passed all 76 test programs, including
the 50 admin scenarios and packaged loading from outside the working directory.

- Actual campaign paint in all four biomes uses the existing vitality and skill
  assets, eight upgraded health icons, damage state, Tide/Crescent/Riposte/dash
  cooldowns and recovery dimming. Screenshots in `/tmp/b2bj-restored-hud` were
  inspected; prompt, camp and debug panels no longer overlap restored UI.
- Outpost remains 21 authored enemies, now 11 scouts, six guards, four spitters.
- Guards track then lock a 100-pixel forward bash with a 55-pixel half-width,
  no rush propulsion, 0.65-second tell and one-second recovery. All four facing
  directions, safe rear/flanks, range, stagger cancellation and punishable
  recovery are checked. Wolf and legacy long-charge profiles remain unchanged.
- Placeholder melee bodies have separate grounded anticipation/contact/recovery
  poses and committed facing. New PixelLab animation work is a separate stage;
  this checkpoint does not claim those pending jobs are integrated.

No numeric coverage instrumentation is installed; no percentage claimed.
New tests use memory-only campaigns and do not touch the player's disk save.
