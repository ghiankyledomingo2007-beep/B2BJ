# Guardian preview — original player combat

2026-11-02. This replaces the browser movement study for gameplay evaluation. The browser study had simplified shots; its damage figures were not evidence of balance against the real player kit.

`build.py` patches copies of five game sources in a temporary directory and compiles them against the installed JAR. It builds a separate `Guardian-preview.jar`; it never replaces `build/B2BJ.jar`. The campaign has no save path. Other working-tree edits are left alone. No new dependencies.

```sh
rtk proxy python3 tools/guardian-preview/build.py
rtk proxy java -jar /home/ghiankylledomingo/tmp/b2bj-guardian-native-2026-11-02/runtime/Guardian-preview.jar
```

The preview uses the real `RuinedOutpostGame` input, player damage, animation, audio, hitboxes, armor, collision, cooldowns and earned ultimate readiness. LMB tap/hold, mouse aim, RMB skill, Space dash, F parry, R Beelzebub and Q transform are the original controls. Select either form and either phase with the buttons beneath the game. Refill restores the original training resources/cooldowns. Blade duration is unlimited in this practice session; HP and damage are normal. Ultimate readiness is earned normally. Existing boss minions supply corpses for Beelzebub.

Phase I: a stepping fist, frontal backhand with a safe rear, and a committed shoulder rush. Phase II alternates those with two detached fist strikes and a short turning chest beam. Violet armor, gold edges and cyan core energy match the game sprite. No large warning circles. Attack aim locks before release. The beam visual and damage both respect cover; boss travel uses the actual arena obstacles and a 900-unit leash. Wall-stopped rushes grant extra recovery. Half-health transition is safe.

| Move | Preparation | Aim locks at | Active | Recovery | Damage |
|---|---:|---:|---:|---:|---:|
| Fist | .86s | .56s | .18s | 1.70s | 24 |
| Backhand | 1.00s | .68s | .30s | 1.75s | 22 |
| Rush | 1.12s | .78s | .55s | 2.00s | 28 |
| Detached fists | 1.10s | .72s | .96s | 2.00s | 20 per fist |
| Core beam | 1.25s | .91s | .90s | 2.10s | 24 |

Phase II subtracts .15s recovery; blocked rush adds .40s. Pursuit is 174/192 units/s versus the original player's 260 units/s. Early lead is capped at 48 units (96/120 for the two fists); releases do not home. Original invulnerability and one-hit-per-impact rules apply. Fist/backhand can be parried; beam and detached fists cannot.

The draft suppresses old move-specific Raphael callouts because their original names describe different attacks. Generic existing dialogue remains. Replacing these callouts is deferred until the new moves are approved.

`GuardianDraftTest` checks pursuit, commitment, hit shapes, parry, safe phase transition, terrain/leash, original inputs, no save path, and exact detached-fist shake timing. `GuardianAudioTest` checks the packaged bank and mixer. The original player regression tests are run against this preview too. Render fixtures use the actual game renderer; their boss selection and player inputs are scripted. Human balance and subjective speaker quality still need a play/listening pass.

Shortcut: this preview subclasses the Guardian and builds source-copy patches; migrate approved changes into the main source when the owner chooses to install the encounter.
