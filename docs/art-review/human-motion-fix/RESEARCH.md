# Combat research and recommendations — 2026-09-16

Research only. These are proposed additions, not approved implementation scope.
Generation remains paused. The requested core remains four individual M1 clicks
with a finisher, a separate hold/release cleave, dash, parry and Crescent.

## Findings from primary sources

**Enemy reactions deserve their own animation work.** Bayonetta 2's lead motion
designer describes changing an enemy's reaction according to the attack received,
rather than using the same response throughout a combo. For B2BJ, this supports
distinct light-cut recoil, thrust recoil and heavy finishing reactions.
[PlatinumGames: Figuring Out Damage Motion](https://www.platinumgames.com/official-blog/article/6647).

**Attack movement and knockback need to work together.** Santa Monica Studio's
combat-design presentation explains both the appeal of attacks that move the
player forward and the problems caused by excessive movement and enemy displacement.
For B2BJ, use a small collision-checked step on the thrust and restrained early-hit
knockback, so the target remains reachable for the next move. The finisher can
create more separation. Do not copy the presentation's 3D targeting system.
[GDC 2019: Evolving God of War's Combat, slides 20–21 and 129–138](https://media.gdcvault.com/gdc2019/presentations/Sheth_Mihir_EvolvingCombat.pdf).

**Connect defense to offense.** An interview published by PlatinumGames discusses
Bayonetta's Dodge-Offset as a way to combine evasion with attacks. Our adaptation
would preserve a pending combo step through one recovery-cancelling dash, without
requiring M1 to remain held. This is a B2BJ proposal, not an exact reproduction.
[PlatinumGames: Interview with Saurian Dash](https://www.platinumgames.com/official-blog/article/6421).

**Input reliability is part of combat feel.** Hades' High Speed update specifically
worked on buffering basic actions and dash follow-ups. Retain a short, single-input
buffer in B2BJ, and test rapid clicks, release, interruptions and pause. More poses
cannot fix dropped inputs or an attack firing unexpectedly afterward.
[Supergiant: High Speed Update](https://www.supergiantgames.com/blog/hades-the-high-speed-update-patch-notes/).

**Effects must communicate the fight.** Hades' Nighty Night notes include clearer
effects, improved visual/hitbox agreement, and tuning to hit pause and buffering.
Our blue effects should distinguish cut, thrust and heavy cleave, preserve enemy
telegraphs, and match actual reach. A charge-ready sound/flash and restrained combo
step indicator are our proposed applications, not copied Hades UI features.
[Supergiant: Nighty Night Update](https://www.supergiantgames.com/blog/hades-the-nighty-night-update-patch-notes/).

## Best additions for this project

| Priority | Proposed addition | Concrete behavior |
| --- | --- | --- |
| First | Three enemy reaction families | Light recoil, narrow thrust reaction, heavier stagger/knockback; armor and bosses keep resistance |
| First | Attack footwork | Small forward step on thrust; planted pivot on rising cut; no sliding through walls or oversized early knockback |
| First | Clear combo and charge cues | Four small step marks by the M1 slot; distinct finisher cue; charge-ready pulse/sound; no new resource meter |
| Next | Continue a combo after one dodge | Click twice, dodge after contact, then click for step three; no automatic attack or infinite timer extensions |
| Next | Slime-flavored dash | Brief cyan liquid afterimage and a reform ripple using the existing dash duration and immunity rules |
| Useful for presentation | Small practice fixture | Reusable dummy/test encounter showing combo step, contact and charge state, using the existing testing mode |

The first three primarily improve readability and existing interactions. Review
them with the base combo before adding the next group. Cue design and the slime
dash are creative recommendations for B2BJ, not claims that research proves one
specific visual solution is best.

### Dodge/combination rule if selected

- With no active combo, dash then a short M1 click uses the proposed thrust opener.
- During an active combo, one legal recovery dash preserves the next step instead.
  A fresh short click after the dash advances it; holding still selects charging.
- Preserve for a short capped window after the dash. Further dodges do not keep
  extending the chain; hurt, charge release, timeout and form loss still reset it.
- Do not cancel an unlanded attack into free damage or extend invulnerability.

### Small demonstration sequence

Demonstrate four individual clicks and the finishing recoil; compare a missed
attack; show partial and full charge releases; then show the selected dash follow-up
and one parry/counter. The observer should be able to identify the current move,
contact moment and enemy response without an explanation of internal state.

## Existing code to reuse

- `B2BJ.attackHeld` and `RuinedOutpostGame.attack()` already repeat/advance attacks;
  the requested change is short-click sequencing plus hold/release distinction.
- `RuinedOutpostGame` already provides hit pause, Crescent, Riposte, a Blob dash
  counter window and collision-aware damage checks. Refine these rather than
  introducing duplicate combat systems.
- `Wisp.hurt()` already enters HURT with a stagger cooldown; `Wisp.push()` supplies
  knockback. Enemy reactions must respect that cooldown and normal armor rules.
- `CombatFeedback` supplies shake/flash, and reduced-effects mode already exists.
- Absorbed Membrane/Jet traits already exist in Blob form and clear on transformation.
  A human power-copying tree would be a separate gameplay change.
- Fallen Knight shields currently reject slime damage; human sword damage already
  bypasses that particular shield rule. A new "charge breaks shields" feature would
  require redesigning that relationship, so do not advertise it as a missing mechanic.

## Keep optional until the core is convincing

A perfect-dodge reward could be a later skill layer, but it needs reliable near-hit
detection and a clearly bounded benefit. Aerial launchers, juggling, weapon swapping,
execution cameras, a large power-copying tree and a separate stamina system add
substantial state and balance work. They are not recommended for the current
Ruined Outpost presentation scope. These scope choices are engineering judgment.

## Important prototype check: tap versus hold

The proposed shared M1 mapping commits a normal attack on a short release, while
showing preparation on press. That can feel less immediate than the current
press-to-attack control. Test it before commissioning final charge animation.
The 0.18s/0.8s thresholds in PLAN.md are trial values, not researched universal
optima. Do not silently fire an initial normal hit and a charged hit from one press.
