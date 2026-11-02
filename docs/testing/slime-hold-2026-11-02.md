# Slime hold attack — 2026-11-02

The owner asked where the hold attack was after the combo preview, then requested a powerful buildup/release with appropriate screen shake. Added the missing hold-to-charge path.

## Behavior

- Slime LMB now shares the existing press/release input path with Blade. A quick tap releases the existing three-hit combo; a hold no longer auto-fires repeated basic shots. A late tap can buffer one next hit during the final 0.12s of recovery. Tap time receives partial wind-up credit to avoid adding unnecessary input delay.
- Holding past 0.18s prepares Water Jet. Full charge takes 0.8s; the ready chime fires once. Aim can change while holding. Movement stays available at45% speed. Releasing early produces a weaker jet; full charge deals52 raw damage, has440px range and420 impulse. A half charge deals38 damage. The focused jet stops on its first target or cover and respects armor; it does not replace Tide's crowd-control role.
- Release has0.04s wind-up and0.58s recovery. Actual release performs one collision-safe backward recoil of10–18px. A successful hit staggers and holds impact for0.10s. Dash, getting hurt, pause/focus loss, transformation, death/reset and successful Tide cancel the stored hold. No Ichor cost or new binding.
- Native-pixel charge orb grows beside the slime as the body compresses; converging motes and a steady ready rim indicate charge. The released jet has a pale core and thin water streaks, a source splash and a heavier water burst on impact. It uses existing authored water art plus code-drawn pixels, with no external asset generation.
- Existing charge loop, ready chime, finisher release and impact recordings supply sound. Fixed an existing end-of-update cleanup that would otherwise immediately stop the slime charge loop. Brief release camera kick uses existing CombatFeedback; damaging hits keep heavier feedback. Reduced-effects rendering suppresses camera shake and additional particles while preserving the charge core/readiness cue.
- Controls already described LMB as “Attack / hold to charge”; the gameplay HUD now shows “Water Jet · Charging” and “Water Jet · Release LMB”. Approved HUD/layout, voices, music, enemy AI and asset files remain unchanged.

## Verification

Java17 scoped compilation (`--release 17 -Xlint:all -implicit:none`) passes.21 focused test entrypoints pass across hold/combo/casting/rendering, Blade animation/input, water/Tide, gameplay integration/stats, dialogue, Beelzebub, HUD/menu, feedback and sound. Final packaged checks from `/tmp` pass: SlimeHoldTest, SlimeActionTest, SlimeComboTest, SlimeAttackRenderingTest, SlimeDepthRenderingTest, BladeCombatTest, OutpostDialogueTest, HudOverhaulTest and PackagedAssetLoadingTest.

SlimeHoldTest exercises actual mouse press/release, tap buffer, hold without auto-fire, charge cue once, aim, partial/full damage, recoil/recovery, interruption, fast-projectile cover collision, input validation, charge sound loop start/stop, visible reduced-effects charge and bounded camera kick. The previous build fails the new hold-input check. SlimeComboTest's all-projectile-kind expectation now includes CHARGED_JET. Existing eight-heading rendering checks cover the new jet.

Rendered102 frames each for normal/reduced effects using real campaign art, actual mouse input and offline game audio mixer against a passive high-health target. Each full jet deals52 damage once. Normal capture's maximum camera displacement is6px; reduced capture is0px. Audio3.4s, peak23135, RMS2374, no hard-clipped samples. Preview mutes dialogue/music to isolate game SFX and does not open an audio device.

Updated the existing gallery with the hold preview first and a sound button: `docs/art-review/slime-action-2026-11-02/index.html`, `http://127.0.0.1:4517/artifact/8ef436118e17/`. Chrome tab1863068459 retained; video decoded/playing/no overflow verified. Playwright clicks timed out, so used documented CDP mouse input at the observed sound-button bounds; verified “Mute sound”, muted=false, playing=true. Reduced-effects video is separately linked. Other earlier clips remain silent.

## Installed build and limits

Installed launcher JAR SHA256 `ad6dd2eeab384d8c56a60a85bdaab543cb0c2ee7043153cc3c3aa53b75693ef9` after a concurrent-build hash guard and atomic replacement. Only14 class entries across B2BJ, RuinedOutpostGame and WaterProjectile changed; Java17 class major61; all asset/audio entries remain byte-identical. No new dependency. Save hash stayed `d90b2c68f96a8fd1396a0046f728391a4395257b79721872652431e915d5f26d`.

Backup, exact diff, candidate, logs, audio/render evidence and preview helper: `/home/ghiankylledomingo/tmp/b2bj-slime-hold-2026-11-02/`. No forced native game restart, full-suite run, manual full combat/balance playthrough, commit or push. Restart loads this build. Hold replaces previous automatic repeated LMB firing; click for individual combo strikes. Human feel/balance still needs playtesting.

## 2026-11-02 follow-up — Orb placement and formation

Owner’s play screenshot showed the charge overlapping the face and requested a more visible forming animation. Changed only B2BJ’s drawSlimeCharge: aim-relative center now moves78–84px forward horizontally and64–70px vertically (previously44/36), with12px vertical art offset. Eight-heading normalized aiming retained. Three short curling water streams gather around a growing irregular shell; bright center arrives after42% charge and densifies toward full charge. Full charge keeps water circulation. Reduced effects omit streams and settle motion at full charge while keeping growth/core/readiness readable. No input, charge duration, damage, collision, sound or camera tuning changed.

Expanded existing SlimeHoldTest with sparse-to-solid growth, bright-core clearance in all eight headings, flowing full charge and settled reduced-effects checks. Previous build fails the forward-position check. Four focused entrypoints pass (hold, attack rendering, depth rendering, casting); three packaged entrypoints pass from/tmp. Actual renderer produced102 frames each for normal/reduced effects; both still deliver52 damage once, camera displacement6/0px. Early/late frames visually inspected. Updated existing browser preview to hold-forming.mp4 and hold-forming-reduced.mp4; retained Chrome tab1863068459 and verified new720×420 video decoded/playing.

Installed SHA25600512af836ff978e175bb1df9e4ecb1bb96eef91bb36ae3abb760173adbb9315. ZIP audit: five entries in B2BJ class family only; Java17; assets unchanged. Save unchanged during this task. Backups, exact diff, logs and renders: /home/ghiankylledomingo/tmp/b2bj-charge-forming-2026-11-02/. Running game not forcibly restarted; next launch loads update. No full playthrough, full suite, commit or push.
