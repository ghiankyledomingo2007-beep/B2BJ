# Admin controls and gameplay audit

Date: 2026-09-08. Scope: the Java campaign, admin controls, gameplay state, local saves, combat, world collision, Swing input/rendering, options, and audio. No external service, asset generation, credentials, or `.claude` files were used. Independent QA compiled into `/tmp/b2bj-admin-quality.pVFIfW`; it did not rebuild the running JAR or take native window focus.

The collective source review covered all 23 production Java modules. The build uses the Java standard library without third-party dependencies, so a package-manager dependency advisory audit was not applicable. No numeric code-coverage percentage is claimed.

## Build result

The first final `rtk proxy ./build.sh` run completed with exit code 0: all 72 assertion-based test programs passed, including the exact 50-scenario registry, followed by the packaged-asset check from `/tmp`. All ten reproduced audit defects have passing focused regressions and were included in that combined run.

One final admin message check then exposed inaccurate checkpoint wording: sandbox camp rest claimed a checkpoint was saved even though save isolation correctly preserved the normal save. `DebugSafetyTest.cachedContinueRemainsPristine` reproduced that message failure before the conditional notice was implemented. Camp rest now states `TEST SESSION NOT SAVED` in a debug session. This is finishing the new admin feature, not an eleventh pre-existing audit defect.

The final rerun after that notice change also completed with exit code 0: **72/72 test programs and 50/50 named scenarios passed**, including the packaged-asset check from `/tmp`. `javac --release 17 -Xlint:all` produced no errors or warnings; `bash -n build.sh` and `git diff --check` passed. A task-diff credential-pattern scan found no matches. This is not a comprehensive security certification.

The native Swing controls and paused/playing save-protection badge were rendered headlessly and visually inspected. Reproduce those captures by passing an output directory to `DebugPanelTest`. No new dependencies were added: the testing menu reuses native Swing, and the test-first workflow retained reproducible failing checks before each implementation/fix.

The verified JAR is `build/B2BJ.jar`; it contains `DebugPanel.class`. Packaging now completes in a separate temporary file before replacing the verified artifact. The already-running game was not restarted; close and relaunch it to use F1 testing controls. Other CLI agents and their worktrees were left untouched. All commits are local; no push or publication occurred in this pass.

## Verification method

`DebugSafetyTest` first compiled against the existing game and failed at runtime with `Debug API missing: debugAvailable`, establishing the missing-feature check before implementation. After the controller and panel landed, `DebugSafetyTest` and `DebugScenarioTest` both passed. The latter executes exactly 50 explicitly named scenarios, prints `SCENARIO 01 PASS` through `SCENARIO 50 PASS`, and asserts the count is 50.

These are deterministic automated gameplay scenarios, not 50 human playthroughs. Fixtures create temporary saves and place actors at known, collision-free positions. Enemy and boss health changes use real attack, projectile, dash, and parry commands; the tests do not directly subtract target health. The wall test repositions one enemy using reflection to construct an otherwise valid obstruction case. Reflection also lets the admin boundary tests report missing APIs without requiring production edits.

## Fifty executed scenarios

All entries below passed in `DebugScenarioTest`.

| # | Scenario |
| --- | --- |
| 01 | Legacy game rejects admin mutations |
| 02 | Title rejects admin mutations |
| 03 | NPC dialogue rejects admin mutations |
| 04 | Dead player rejects admin mutations |
| 05 | Final choice rejects admin mutations |
| 06 | Completed campaign rejects admin mutations |
| 07 | Active campaign exposes read-only availability |
| 08 | Paused campaign accepts admin toggles |
| 09 | Invalid warp IDs leave state untouched |
| 10 | God mode rejects lethal health damage |
| 11 | God mode rejects Blade hit Ichor penalty |
| 12 | God mode preserves natural transformation drain |
| 13 | Disabling god mode restores incoming damage |
| 14 | Disabling one-shot restores knight armor |
| 15 | Refill restores health and Ichor |
| 16 | Refill resets all combat cooldowns |
| 17 | Shard button grants exactly twenty-five |
| 18 | Turning toggles off keeps sandbox sticky |
| 19 | Debug rewards, boss clear, and ending preserve disk save |
| 20 | Title and Continue restore pristine cached save |
| 21 | Outpost camp warp is collision-free |
| 22 | Forest camp warp is collision-free |
| 23 | Catacombs camp warp is collision-free |
| 24 | Citadel camp warp is collision-free |
| 25 | Outpost boss warp preserves pause and wakes Warden |
| 26 | Forest boss warp preserves pause and wakes Briarheart |
| 27 | Catacombs boss warp preserves pause and wakes Oathkeeper |
| 28 | Citadel boss warp preserves pause and wakes Golem |
| 29 | Encounter reset restores enemies and defeated boss |
| 30 | Normal portal travel retains sandbox flags |
| 31 | One-shot Water Slash defeats raised knight shield |
| 32 | One-shot Tide Wave defeats raised knight shield |
| 33 | One-shot Blob dash defeats raised knight shield |
| 34 | One-shot Blade cut defeats raised knight shield |
| 35 | One-shot Ichor Crescent defeats raised knight shield |
| 36 | One-shot Riposte defeats knight through real impact |
| 37 | Connected water one-shot defeats armored awake boss |
| 38 | Connected dash one-shot defeats armored awake boss |
| 39 | Remote cast cannot kill dormant boss |
| 40 | Real terrain stops one-shot projectile |
| 41 | Wrong-facing one-shot Blade cut cannot connect |
| 42 | Expired water range cannot hit distant target |
| 43 | Dash cancels pending one-shot water windup |
| 44 | Pause freezes pending one-shot until resume |
| 45 | Reversion cancels pending Blade strike |
| 46 | Death cancels pending one-shot release |
| 47 | Warp clears transient projectiles and pending combat |
| 48 | Focus loss clears held movement before resume |
| 49 | Actual admin checkbox callbacks apply while paused |
| 50 | Death retry remains sandboxed until normal Continue |

Scenario 14 also checks that disabling one-shot before an in-flight projectile lands restores normal impact-time armor rules. Scenario 36 checks a real Riposte counter while god mode is also enabled. Opening the admin panel is read-only; the first mutating action starts a session that cannot overwrite either disk progress or the cached normal Continue snapshot. Turning both toggles off does not remove that save restriction.

## Supplemental audit findings

These checks supplement the 50 named scenarios; they are not counted as additional entries in that registry.

| Finding | Reproduction and verification |
| --- | --- |
| Wallet overflow crashed reward autosave | At the valid 100,000-shard save limit, landmark, quest, and boss rewards exceeded save validation. Shared bounded rewards are covered by `SaveAuditTest`. |
| Dialogue hid the title after returning from pause | NPC dialogue, pause, then return-to-title left the dialogue overlay active. `SaveAuditTest` checks the title clears it. |
| Knight overhead could damage through a wall corner | A knight tracked a moving player behind real catacomb terrain during its tell. Incoming melee now uses terrain visibility; `WorldAuditTest` covers actual game damage. |
| Catacomb generation omitted trailing eastern wall spans | The wall-row loop did not reach its closing sentinel. The corrected bound and expected spans are checked by `WorldAuditTest`, with campaign reachability rechecked. |
| Dead boss summons accumulated indefinitely | Repeated summon, kill, and expiry cycles retained dead bodies/profile entries. `CombatAuditTest` checks expiration removes summons while keeping authored enemy reward indices stable and preserving death animation. |
| Dialogue click queued a later attack | A blocked mouse click could become Water Slash after closing dialogue. `UiAuditTest` exercises the actual callback sequence. |
| New journey retained old visual effects | Transformation state survived the title/new-game sequence. `UiAuditTest` checks visual reset. |
| Invalid UI frame duration poisoned animation clocks | Invalid `B2BJ.step` input reached rendering timers. `UiAuditTest` checks finite clocks and rendering. |
| Ordinary enemy death pose disappeared immediately | Rendering used `alive()` instead of the still-visible death phase. `UiAuditTest` compares real painted frames during the death interval. |
| Transformation reused stale attack facing | Reverting after an eastward Blade attack, moving left, transforming again, then dashing without movement could still dash right. `UiAuditTest` checks the actual callback and dash-position sequence. |

All ten concrete audit defects were reproduced, fixed, and passed their focused regressions plus the first complete 72-program build. The five UI cases use real Swing callbacks or painted-frame comparisons; save and collision cases exercise the controller rather than merely checking copied helper logic.

## Scope and limits

The prior complete-campaign route, both endings, save recovery, boss economy measurements, and performance evidence remain documented in [campaign-quality.md](campaign-quality.md). Admin tests cover the actual six damage paths and preserve collision/range checks; they do not certify every possible input sequence, native display/audio device, or an absence of all bugs. QA's new admin tests were headless and did not take native focus or validate audible playback. Placeholder art and unassisted human difficulty remain outside these automated claims.
