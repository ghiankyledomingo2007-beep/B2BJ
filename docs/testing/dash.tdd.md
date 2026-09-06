# Dash TDD Evidence

## User journey

As a player, I want to dash with Space so I can evade threats without crossing solid obstacles.

## Evidence

| Guarantee | Test | Result |
|---|---|---|
| Dash moves over multiple frames and enforces the 0.4-second cooldown | `PlayerMovementTest.dashesSmoothlyAndRespectsCooldown` | PASS |
| Dash advances toward a pillar without entering its collision area | `PlayerMovementTest.dashStopsAtPillar` | PASS |
| Idle dash can reuse the slime's last facing direction | `SlimeAnimationTest.keepsFacingDirectionWhileIdle` | PASS |

RED: `rtk javac -Xlint:all -d out src/*.java test/*.java` failed because dash methods and constants did not exist.

GREEN: the same compile command passed, followed by all four executable test classes passing with assertions enabled.

Coverage tooling is not configured in this direct-`javac` project. Checkpoint commits were skipped because Git author identity is not configured; no user Git config was changed.
