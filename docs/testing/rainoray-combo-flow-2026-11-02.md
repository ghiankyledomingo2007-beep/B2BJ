# Rainoray combo flow — 2026-11-02

Owner found human-form combos stiff. The existing0.12s input buffer only opened near full recovery; every cut returned to neutral, and click wind-up credit shortened the pose without shortening its input cooldown.

## Change

- First three normal cuts and the dash-slash opener accept one next LMB click within0.24s of their link point. They connect after68% of post-contact recovery: approximately0.314/0.330/0.381s for cuts1–3, instead of waiting0.40/0.42/0.48s. The finisher, charged cuts and other abilities keep full recovery. Unbuffered cuts still play the full authored recovery.
- Chained cuts begin at ready pose1 rather than neutral pose0; body and sword trail share the same selection. Contact remains frame4 at the original wind-up timestamp. Existing standalone timing/weight tests remain unchanged.
- Press-time credit now also shortens Player attackCooldown to match bladeTime, removing the invisible input lock after the pose. Swing audio scheduling subtracts elapsed wind-up credit too.
- One queued click, retained aim, hit pause, hold-to-charge behavior, interruption, collision-safe steps, damage, armor and resource costs remain. Only B2BJ.java and RuinedOutpostGame.java production sources changed; no new art/audio/dependency.

## Verification

Java17 scoped compilation with -Xlint:all. Twelve focused entrypoints pass: BladeComboFlowTest, BladeCombatTest, AnimationPlaybackTest, BladeTrailTest, SlimeHoldTest, SlimeComboTest, CombatLoopTest, CombatAuditTest, BeelzebubTest, CombatSfxTest, CombatStatsTest and DashVariantsTest. Five packaged checks from/tmp pass: BladeComboFlowTest, BladeCombatTest, AnimationPlaybackTest, SlimeHoldTest, BeelzebubTest. Previous build fails the new early-input assertion. Existing tests were not weakened. New test covers earlier input, only one buffered follow-up, retained aim, full finisher recovery, cooldown/pose synchronization, holds, pause/dash/hurt/reversion and connected contact poses.

Rendered210 frames per version at60fps using actual campaign art, real mouse events and offline sound mix against a passive high-health scout. Both deal108 damage. Old move starts0.25/0.70/1.167/1.70s; new0.25/0.567/0.95/1.383s. Driver attempts each follow-up at0.15s elapsed and retries a rejected press; old build required39 attempted clicks vs4 accepted immediately in the new build. This is staged input/render evidence, not a human gameplay timing measurement. Source sound recordings unchanged; combat/dialogue voices and music muted for this preview.

Comparison: docs/art-review/rainoray-combo-flow-2026-11-02/index.html; http://127.0.0.1:4517/artifact/30a1019388dc/. Opened explicitly in Ghian’s Chrome, tab1863068512. Both720×420 videos decode; no page overflow. Started both via visible controls; updated sound unmuted, old muted. Reduced-motion preference is respected on initial load; explicit Replay starts the clips.

## Installed build

SHA2561ea2ee831b6a6b2eed94ce07fab56bfc580addbc833937c54a0e2dd9d79d8a26, replacing00512af836ff978e175bb1df9e4ecb1bb96eef91bb36ae3abb760173adbb9315. Atomic install after checking the previous JAR hash. Seven class entries across B2BJ/RuinedOutpostGame changed; Java17 major61, all asset/audio entries identical. Save unchanged during this task. Backups, exact diff, tests, candidate and comparison frames: /home/ghiankylledomingo/tmp/b2bj-rainoray-flow-2026-11-02/.

Running game not forcibly restarted; next launch loads update. No full-suite run, human sound audition, manual full playthrough, commit or push. Faster linked cadence increases damage rate without changing damage per hit; full encounter balance still needs player testing.
