# Character Animation TDD Evidence

Source: [Ruined Outpost animation plan](../ruined-outpost-animation-plan.md)

## User journeys

- Player and tutorial enemy render at one consistent 2x pixel scale.
- Blade selects correct run, dash, and slash frames for each facing direction.
- Wisp visibly patrols the Ruined Outpost using its matching east/west row.
- Wisp plays one directional telegraph, lunge, and recovery cycle instead of
  repeating its walk frames.
- Runtime character sheets load with exact grids, transparency, and project palette.

## Evidence

| Guarantee | RED | GREEN |
| --- | --- | --- |
| Character sheets meet grid, alpha, and palette rules | `CharacterAssetQualityTest`: `slime sheet must not contain green screen pixels` | `CharacterAssetQualityTest passed` |
| Blade and Wisp directional controllers exist | `CharacterAnimationTest`: `cannot find symbol BladeAnimation` | `CharacterAnimationTest passed` |
| Wisp stays inside its patrol lane and turns visibly | `WispTest`: `cannot find symbol Wisp` | `WispTest passed` |
| Slime, Wisp, and Blade use one 2x render scale | `CharacterAnimationTest`: `cannot find symbol RENDER_SIZE` | `CharacterAnimationTest passed` |
| Slime dash uses directional squash/stretch frames | `SlimeAnimationTest`: `method update ... cannot be applied to given types` | `SlimeAnimationTest passed` |
| Dedicated Wisp attack sheet keeps grid, alpha, and palette | `CharacterAssetQualityTest`: `wisp_attack.png must load` | `CharacterAssetQualityTest passed` |
| Wisp attack advances once through telegraph, lunge, and recovery | `WispCombatTest`: `cannot find symbol usesAttackAnimation` | `WispCombatTest passed` |
| Wisp attack keeps the locked `96x96` render size | Old telegraph/lunge path scaled the sprite to `0.85x`/`1.1x` | `WispCombatTest passed` |

Targeted and full validation used Java 17-compatible commands:

```text
rtk javac -Xlint:all -d <classes> src/*.java
rtk javac -Xlint:all -cp <classes> -d <tests> test/*.java
rtk java -Djava.awt.headless=true -ea -cp <classes>:<tests> <TestClass>
```

Full result: **10/10 test programs passed**. Runtime window inspection confirmed
Slime and Wisp at `96x96`; Blade contract is `96x128` from its taller `48x64`
native frame. Project has no line-coverage runner, so no numeric coverage claim
is made. Checkpoint commits were skipped because the working tree already
contained unrelated owner changes; RED/GREEN proof is preserved here instead.

Wisp attack sheet was also reviewed at native resolution before promotion;
white-flash artifacts from generated source frames were rejected.
