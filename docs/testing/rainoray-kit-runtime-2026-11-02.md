# Rainoray kit runtime — 2026-11-02

Owner approved installing the void sound audition and the full proposed skill kit. The taskbar game's `build/B2BJ.jar` now contains the implementation; the gallery videos remain the original staged reference.

## Playable changes

- RMB in human form is Void Sever: earn readiness through landed chain finishers (34%), successful parries (50%) and absorbed corpses (25%). Aim during the0.48s wind-up; commit to a collision-limited320px advance over0.18s, then rupture at0.80s. Ends at1.22s. One32-damage contact and one48-damage rupture per target before defense. Only the advance protects Rainoray.
- F keeps the0.30s parry window/6s cooldown, without extra Ichor spending. Ordinary enemies get1.15s recovery after a successful counter. Guardian TARGET/SWEEP strikes are parryable and lead into normal bounded recovery; charge, shockwave, fissure and ground hazards remain dodge decisions. Taking an unparryable hit cancels the guard.
- R keeps the existing Beelzebub harvest and sustain behavior, adding one stored Devoured empowerment. Next committed charged cleave or ultimate consumes it. Cleave adds one delayed18-damage hit; ultimate rupture becomes66 damage and opens a bounded ordinary-enemy punish. No stacked charges. Charged cleave no longer spends additional Ichor.
- Readiness/empowerment survive form changes. Death, retry and chapter entry reset them; no save schema change. Interruptions do not refund a committed ultimate or empowered strike.
- Native-pixel blade gathering, dark lane/rift, small gold parry, contact silhouettes, afterimages, hit stop and existing feedback shake. Reduced effects omit the added silhouette/dimming/afterimages/sparks.
-15 approved cue WAVs copied exactly plus a deterministic sustained hold loop. Human charge, cleave, ultimate, parry/counter and empowerment use the new sounds. Fixed-pitch contact alignment trims pre-roll on immediate impacts. Slime audio, dialogue and music are retained. HUD RMB icon/readiness and controls text updated.

## Verification

Java17-compatible scoped compilation with lint passed.142 test programs were exercised.140 distinct programs passed across the initial run and focused corrections. Two failures reproduce on the unchanged pre-task JAR: `RuinedOutpostGameTest` cannot clear its old scripted route target; `TideLayerTest` fails its old impact-anchor assertion. These are recorded, not suppressed or changed as part of this kit.

Updated AnimationPlaybackTest/CampaignMenuTest expectations for the intentionally replaced Crescent/Riposte labels. The first broad invocation exposed the repository asset folder on the classpath, causing archive/packaging failures; rerunning with only the candidate JAR resolves these. AssetArchiveTest must run from the repository because it reads an archive manifest; PackagedAssetLoadingTest runs from `/tmp`.

18 distinct packaged programs passed across verification. After the final empowered stagger adjustment, reran RainorayKitTest, RainorayVoidAudioTest, RainoraySkillsTest, BeelzebubTest, AssetArchiveTest and PackagedAssetLoadingTest against the final candidate using the launcher's Java21 runtime. New tests cover earned resource, missed finishers, two-hit limits, wall collision, aim lock, interruption/protection, parry openings, boss armor/hazard rules, one-time empowerment, cue variants, exact sample delays, mute and charge-loop cancellation.

Actual game renderer/mixer fixtures exercised ordinary ultimate, empowered ultimate, parry→charged punish and Beelzebub→empowered cleave in normal/reduced modes. Targets take80,98,96 and78 damage respectively in the no-critical fixture. No clipped PCM samples; final powered-ultimate peak23820/32768 and empowered-cleave peak22759/32768. Inspected normal/reduced effect and contact frames. Fixtures used isolated saves in staging, never the player save. These are scripted encounters, not a human balance playthrough or subjective speaker audition.

`install_rainoray_void_sfx.py --check` verifies all16 runtime files. Candidate contains no duplicate entries/test classes/source archive; all changed classes use Java17 bytecode.736 pre-existing asset entries remain byte-identical.38 changed/new archive entries. Scoped diff whitespace check passed.

## Install and remaining limits

Installed JAR SHA256: `4732544b457972069e0e2188742302fef667b8fb04737997572e6c028a7890fe`.
Prior JAR backup and all logs/test results/render frames: `/home/ghiankylledomingo/tmp/b2bj-void-runtime-2026-11-02/`.

Started the taskbar target using `/usr/bin/java -jar build/B2BJ.jar --fullscreen`; process remained running and startup log was empty. Save matched pre-task SHA256 through installation. After launch, the running game updated its campaign save; that live progress was left untouched. No forced shutdown, save restore, commit or push.

Manual encounter balance and physical speaker quality remain unchecked. The two pre-existing test failures above remain. The approved movies have choreographed camera/target positioning; gameplay effects follow actual contacts and collisions.
