# Independent Fable 5.1 review — 2026-09-06

Completed through installed Claude Code CLI 2.1.263. The substantive assistant
response identified model `claude-fable-5-1`; the result reported success, exit 0,
and canonical model `claude-fable-5-1` from the first-party provider. No fallback
model was requested. CLI usage also lists Haiku auxiliary usage; this was not a
second substantive review. No credentials are recorded here.

The first read-only tool-enabled attempt timed out. The completed attempt used
safe mode, no tools or MCP access, and a supplied packet of numbered source excerpts
and scenario results. It was an independent interpretation of that packet, not
an independent gameplay run or full-repository inspection.

## Primary-auditor adjudication

- Accepted: frozen dash during recovery; overlapping summons; static boss and
  ineffective pressure in the measured scenarios; last-frame lunge retarget;
  failed obstacle pursuit; ranged attacks exceeding awareness.
- New source-traced consequence: `RuinedOutpostGame.tick` still invokes
  `damageEnemiesFromDash` while the frozen dash flag is true. After reversion,
  its Blade guard no longer applies. Unhit enemies entering contact can therefore
  receive stale dash damage. This consequence was not separately scenario-tested.
- Rejected: "240 HP is unreachable for a fresh player" and "any moving player
  never gets hit." These generalizations do not follow from these limited bots.
  The fresh continuation is also scripted, not "unscripted." Staged wins show
  that specific repeatable tactics can win, not typical human performance.
- Qualified: overlap is measured, but the evidence does not establish that
  synchronized stacked attacks specifically caused the fresh-route death.
  All five hits were attributed to adds; causation beyond that needs event traces.
- Corrected: the resource-bank probe used a 10-Ichor ordinary-enemy drop, not 15.
  Pickup waste/stockpiling remains a policy mismatch; indivisible pickups alone
  are not proof of a defect. Skipping all Blade pickups or requiring room for the
  whole drop could damage the intended transformation economy.
- No defect: `OutpostStory.playerDied()` is idempotent (lines 44–47).
- Not adopted: arbitrary boss HP reduction to about 100, or simply increasing
  slam radius. First fix behavior and measure readable pressure/resource pacing.
  A line-of-sight attack gate alone does not provide navigation around cover.
- Damage-induced Blade expiry shares the recovery assignment, but `hurt()`
  rejects damage while dashing. Do not claim the exact dash exploit was reproduced
  through damage-induced expiry; test the shared boundary when implementing a fix.

## Raw review (contains the overclaims corrected above)

**Verdict:** the boss "wins" are scripted fixed-radius orbits that finish at 1-2 HP after ~210 s; the only unscripted continuation died. The Guardian itself dealt zero damage in every scenario (boss=0 in all seven boss rows), so the fight is really "three stacked adds versus a 240 HP wall". Two real state bugs, one collision/spawn bug, two design gaps.

**1. Defect: blade reversion mid-dash freezes the dash and grants full invulnerability.** `src/Player.java:48-53`. Line 43-47 expires the blade and sets `recoveryTime`, then line 48 returns before line 49 decrements `dashTime`. `dashing()` stays true for the 1.5 s recovery, so `invulnerable()` (line 101) is true and `hurt()` rejects damage; the dash then resumes after recovery. Matches `reversion-during-dash` (damage accepted=false, invulnerable after 1 s=true, dash still active=true). Same path triggers from `hurt()` line 86. Side effect: `RuinedOutpostGame.java:166` keeps calling `damageEnemiesFromDash` on a frozen player. Fix: decrement `dashTime` before the recovery gate, or zero it wherever `recoveryTime` is set.

**2. Defect: boss adds spawn on top of each other.** `src/RuinedOutpostGame.java:187-193`. Spawn point depends on `impactNumber % 2`, but impacts change every ~2.58 s while adds spawn every 3 s with an identical patrol band, so consecutive adds land at the same pixel (minimum spacing 0.20-0.30 px in every boss row). Wisps have no wisp-wisp separation (`Wisp.java:136-157`). Stacked adds telegraph and lunge in lockstep, which is what killed `fresh-run-boss-continuation` (hits=5, boss=0). The 0.65 s invulnerability (`Player.java:11`) only masks it as one hit per wave. Fix: alternate spawn by add count, offset by existing add positions, and reject spawns inside `WISP_CONTACT_RADIUS` of another wisp.

**3. Design gap with an overclaim attached: 240 HP is unreachable for a fresh player, and the Guardian is harmless.** `src/Guardian.java:14` says "several transformations", but the fresh route arrives with ichor=70 and made zero boss transformations; adds bank 15 each (`RuinedOutpostGame.java:310`), and the guardian only takes damage in RECOVER (`RuinedOutpostGame.java:256, 286`), roughly 58% uptime. Blob DPS is ~1 per 0.48 s in that window, hence 414-420 casts over ~210 s. The orbit rows are not evidence of viability: constant radius, scripted cast cadence, still ending at 1-2 HP. Conversely, `Guardian.java:65-71` latches the target at telegraph start, and 0.9 s at 260 px/s is 234 px against a 112 px radius, so any moving player never gets hit; hp-240 and hp-60 rows are behaviourally identical and travel=0. Direction: cut HP to ~100 or open a HURT/TELEGRAPH damage window; give the slam a tracking phase or larger radial radius. Note `Guardian.State.HURT` is unreachable since `hurt()` (lines 118-128) never enters it.

**4. Defect: wisp lunge direction is sampled at the last frame of the telegraph.** `src/Wisp.java:114-118` computes `lungeX/Y` from the current target when the timer expires, not at telegraph entry (line 106). `last-moment-lunge-retarget` shows a 180-degree flip from a 20 ms crossing, which contradicts the counterplay philosophy stated at `Guardian.java:65`. Related: `Wisp.java:102-103` triggers on distance only, so `enemy-obstacle-routing` shows 29 lunges into a barrier without line of sight while the player is safe. Fix: latch the lunge vector on entering TELEGRAPH; gate the trigger on `map.clearLine`.

**5. Defect: banked ichor is wasted after transform.** `src/RuinedOutpostGame.java:318-321` treats any drop as indivisible: at ichor 99.25 the 15-point banked drop is consumed for 0.75 (`full-meter-bank-consumption`, blade seconds=11.910). The "bank while full" behaviour at line 318 is intentional, but the pay-off is immediately destroyed. Fix: skip pickup while `bladeForm()`, or only consume when `drop.amount() <= 100 - ichor`.

**Minor:** light water range 340 exceeds wisp aggro 280 (`WaterProjectile.java:18`, `Wisp.java:21`), producing the 0-hit 80 s routes and a difficulty cliff at room 9; `RuinedOutpostGame.java:144` calls `story.playerDied()` every tick after death, which is only safe if it is idempotent.

