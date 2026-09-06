# Rainoray skills and engulf simulation

Scope: two human-form skills and a fixed visual anchor for the existing interruptible feeding channel. Journeys were derived from the current user request and the bounded implementation assignment. No dependencies, additional resource systems, or changes to Blob attacks were needed (Ponytail reuse of the existing game loop and damage gateway).

## Balance and behavior

- **RMB — Ichor Crescent:** costs 8 Ichor, 5-second cooldown, 3 damage, radius 22, speed 560 world units/second, 0.65-second maximum flight. One target only; solid terrain and Guardian armor stop the arc. The Guardian takes damage only during its existing recovery opening. Enemy knockback uses existing collision-constrained movement.
- **F — Riposte:** costs 8 Ichor, 6-second cooldown, 0.3-second guard. Negates exactly one incoming damaging hit, then counters visible enemies within 120 units for 3 damage and knockback. No residual invulnerability; a second simultaneous attack may hurt. A distant projectile is stopped, not reflected into a distant enemy. Guardian attacks are negated, but armored Guardian states do not take counter damage.
- Both costs shorten the existing 12-second Blade reservoir by 0.96 seconds. Both require **more than 8** Ichor remaining so the skill cannot start in the same instant as an empty-meter reversion. They share the existing 0.35-second attack recovery; guard prevents attacks and is canceled by dash. Cooldowns persist across room changes and freeze with the game. Restart clears them.
- Feeding remains 0.8 seconds, stationary player collision, +5 Ichor and an ordinary corpse's existing half-heart recovery. Fixed `absorptionAnchorX/Y()` exposes the original corpse location while the existing inward-remains simulation continues. Renderer can spread the slime body over that anchor without moving its collision body. Existing interruption and anti-farming rules are unchanged.

## Renderer API

`RuinedOutpostGame.ichorCrescent(int,int)`, `riposte()`, `crescentCooldown()`, `riposteCooldown()`, `guarding()`, `guardProgress()` (elapsed fraction while active), `crescents()`, `absorptionAnchorX()`, `absorptionAnchorY()`.

`Crescent` exposes `x/y`, `age`, `directionX/Y`, `radius`, `opacity`. Events: `CRESCENT_CAST`, `CRESCENT_IMPACT`, `RIPOSTE_START`, `RIPOSTE_COUNTER`. All damage still routes through `hurtPlayer`, including enemy lunges, hostile projectiles, Guardian slams and the arena agent's fissure.

## TDD evidence

RED: before production edits, compiled `src/*.java test/RainoraySkillsTest.java` using `javac --release 17`. It failed with 37 missing-method/event errors, including `cannot find symbol: method ichorCrescent(int,int)` and `method riposte()`. These were the intended new public behavior, not missing dependencies.

GREEN: compiled the non-UI Java sources and targeted tests with `javac --release 17 -Xlint:all`, then executed each with `java -ea -Djava.awt.headless=true`. `RainoraySkillsTest passed`; `AbsorptionTest passed`; `WaterCombatTest passed`. The focused build excluded the concurrently edited `B2BJ.java` because its new animation enum switch was temporarily incomplete. Final integrated build is the main agent's responsibility.

`RainoraySkillsTest` verifies:

- Blob, dead, paused, zero-direction and low-resource rejection without charging.
- Exact transformation-time deduction; cooldown and guard freeze on pause; six-second guard reuse; dash cancellation; restart reset.
- Invalid resource amounts cannot corrupt Ichor; intentional depletion enters reversion.
- Arc travel before impact, damage once, finite lifetime and swept wall collision.
- One-hit parry, guard expiration, no lingering immunity, no counter through solid cover.
- Actual simulated Wisp lunge, hostile projectile and Guardian slam use the shared guard gateway.
- Fixed engulf anchor and stationary player while the target remains move inward.

Final focused regression run passed 14 programs: RainoraySkills, Absorption, WaterCombat, CombatLoop, ReversionSafety, PlayerMovement, RuinedOutpostGame, SpitterCombat, IchorMagnet, Guardian, GuardianPressure, GuardianMobility, SummonSafety and ArenaTwist. The concurrently edited `ArenaTwistTest` initially failed `blocked perpendicular escape at 224,96 horizontal=false`; its owning agent corrected the overly restrictive straight-line fixture to verify short navigable escape routes. The complete focused run above was then repeated successfully.

No numeric coverage, human playtest, generated-art quality, or final integrated build claim is made here. Checkpoint commits and final verification are coordinated by the main agent; this worker neither stages nor commits shared files.
