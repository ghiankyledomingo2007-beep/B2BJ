# Corrupted Forest completion — 2026-10-26

## Follow-up: softer indicators

User found the full-area warnings too aggressive. Reduced warning fills from25–50 alpha to8–14, narrowed outlines, replaced bright gold/orange with muted taupe/rose, and softened timing bands, claw marks and root-vein emphasis. Active hazards retain stronger cues than windups. Hit footprints, timings and behavior are unchanged. ForestCombatTest, BriarheartCombatTest and TelegraphGeometryTest passed; actual warning captures reviewed. Verified that only `Telegraphs.class` differs in the packaged JAR, then restarted the separate-save playtest. Evidence: `/home/ghiankylledomingo/tmp/b2bj-subtle-indicators-2026-10-26/`. Full suite not repeated for this visual adjustment.

## Follow-up: repeated leaps and unclear warnings

User playtest rejected the repeated jumping/rushing and faint attack indicators. Imp now sidesteps into two planted claw strokes, with distinct visible slashes and a safe gap; it has no forward attack travel. Wolves approach on foot and bite; only every third attack can become a pounce if the player is distant. Shaman stays planted while casting.13 focused regression programs passed, including102 rendered hit-area cases and tests proving two claw-effect bursts and zero Imp lunge travel. The full112-test suite was not repeated for this focused correction.

Forest melee warning and collision share `Wisp.meleeContains`: a broad half-ellipse for claws and a narrow bite wedge. Native-pixel dark rims, restrained fill, advancing timing bands and amber-to-orange commitment replace the former thin direction glints for these two roles. Ground blooms/landings have fixed rims and expanding timing rings; root lanes have dark-edged boundaries and growing root veins. Actual in-game Imp/Wolf/Shaman warning captures were reviewed. Final packaged checks passed from `/tmp`; all277 voice hashes preserved. The playtest reuses its existing separate save.

Evidence: `/home/ghiankylledomingo/tmp/b2bj-forest-attacks-2026-10-26/`. Human difficulty/visual preference remains a playtest judgement.

Scope: the authorized Area Two goal; Chapter One and real save preserved. The game has two playable areas. Areas Three/Four have not been implemented or presented as complete. User is concerned about submission time; no deadline/rubric requirement was supplied.

## Gameplay and presentation

-20 forest enemies with Imp sidestep/double-claw, Wolf bites with an occasional committed pounce, and interruptible Shaman ground bloom. Different group compositions introduce/combine these behaviors. Existing two-attacker cap remains; the updated four-enemy ridge test observes29 attacks with all four taking turns in20 simulated seconds.
- Rooted Briarheart has three separate attacks, committed shared hit/visual geometry, a reachable cage gap, a3.8second exposed-heart window, half-health escalation and no Warden minion waves. Ground hazards pause and clear on death/area change/victory.
- Natural boss dialogue starts with analysis, learns after two observed releases per pattern, persists knowledge separately from Warden and alternates warnings. Health/phase/opening/victory lines use the existing non-overlapping scheduler.
- Sage remains visible in both regions, including actual conversation screens. Idle movement, speaker-only pulse, boss offset, pause and Reduced Effects have direct checks.
- Sedge dialogue/portrait/idle, optional discoveries and survivor ending complete the stretcher-track story. Temporary-save route test includes death/retry, retained upgrades, discoveries, boss knowledge and reload after victory.
- New imported sprites use controlled palettes, binary alpha and fixed native cells. Terrain/corrupted props toned down for attack contrast; groupmate environment imports retained. `tools/PreviewForest.java` produces20 staged render captures under `docs/art-review/area-two/renders/`. These are staged evidence, not a human playthrough.

## Voice evidence

100 new forest lines:44 Rainoray,45 Sage,11 Sedge. All82 prior Rainoray lines also replaced;126 Rainoray lines now share the same approved voice ID, multilingual-v2 model, profile and seed. Generation used sequential fresh context per speaker. Official ElevenLabs request-stitching guidance says previous request IDs must be from the last two hours; historical generation scripts had reused older context. A fixed profile and recent context reduce avoidable variation but cannot guarantee identical acting.

All182 changed dialogue clips passed automated transcription. One invented-name spelling: “Rainoray” recognized as “Raynor Rey”; the rest of that sentence matches. All270 scripts/speakers and shipped-file hashes match `runtime-voices.json`; all files are mono PCM16 at22,050Hz, with dialogue mastered toward-18LUFS/-4dBTP. Long barks receive at most1.08521× tempo; the exposed-heart sentence was shortened and rerecorded instead of sped up excessively.

Seven short combat efforts use the same Rainoray voice, with separate peak limiting and no pitch/formant treatment. Accepted events only; spacing, form rules, speech priority, pause and volume controls tested. Speech does not overlap these efforts; efforts do not pulse Sage.

Raw MP3s, per-request settings and transcript comparisons: `assets/source/audio/consistent-cast-2026-10-26/`. Previous runtime takes preserved at `/home/ghiankylledomingo/tmp/b2bj-voices/forest-2026-10-26/runtime-before-install/`. API key remains private; no account/dashboard/credential changes. Runtime playback is offline.

Sources: [ElevenLabs request stitching](https://elevenlabs.io/docs/eleven-api/guides/how-to/text-to-speech/request-stitching), [text-to-speech API](https://elevenlabs.io/docs/api-reference/text-to-speech/convert).

## Final verification

All112 test programs passed, including101 telegraph/hit-geometry cases, whole-chapter route/retry/save checks, palette/alpha/clearance/flood checks and dialogue timing. Three obsolete fixtures were updated: four enemies are now at the ridge instead of the introductory clearing; the ending resolves the survivors; Briarheart waits until its committed pattern ends before enrage. After the final fixture correction, the remaining tests and isolated asset check ran against the same compiled build and the JAR was packaged. A final visible-screen correction shares the camp E Talk prompt with Sedge; SagePresenceTest, CampaignDesktopTest, ForestChapterTest and VisibleMenuTest passed again afterward.

Final `build/B2BJ.jar`: all277 packaged WAV hashes match the manifest; all270 exported scripts/speakers match; no source archives or test classes are bundled. Staged `ForestCombatAudit` run from `/tmp` with JAR first defeated Briarheart through normal attack/projectile code in both forms (70 slime /58 Blade exposed hits in that run; critical rolls vary), with zero armored hits, hazard cleanup and reachable ending. No direct boss-health mutation or one-shot mode in this final damage audit.

Opened the final game at Ferry Refuge in a separate temporary-save playtest; real campaign save and old normal game process were retained. Sedge's talk prompt and persistent Sage verified on the visible game screen. Logs, launcher/audit source, screenshot and artifact SHA-256 are preserved at `/home/ghiankylledomingo/tmp/b2bj-area-two-final-2026-10-26/`.

Human difficulty/pacing and subjective listening remain unverified. Staged god-mode damage runs establish reachability and damage rules, not fair difficulty. No commit or push requested.

## 2026-10-26 — Mob effects and Sage first encounters

Added native-pixel caster charge/release, rising and withering thorns tied to live ForestHazard lifetime, hooked double-claw trails, closing Wolf jaws and pounce debris. Outpost melee now has weapon-coloured arcs/chips; spitters gather ichor and scatter droplets when firing. Subtle Telegraphs warnings and collision/damage timings are unchanged. Reduced effects retain visible hazard thorns and suppress orbit/bolt motion.

Sage identifies the six currently spawned campaign enemy types plus Fallen Knight support with a one-time voiced name and tactical hint. Uses clear line of sight and a local screen-sized range, nearest unknown type first, eight seconds between identifications, existing speech/overlay channel and saved bark unlocks. NPC conversations and existing active speech take priority. Boss-warning cooldown is independent of identification. Seven approved-profile Sage clips bring dialogue to277 plus7 combat efforts; final recordings transcribe exactly after punctuation/case normalization. Source takes/provenance preserved; runtime manifest and both chapter TSVs updated.

Validation: Java17 lint compile;16 focused test programs against the staged JAR from `/tmp`, including first sight/occlusion/cadence/persistence, hazard visuals/expiry/reduced effects, forest combat, story/boss flow, Sage presence, banter, all chapter voice PCM/timing, audio priority and packaged assets. All284 runtime voice hashes match. Six actual game frames captured and three strike frames visually reviewed. Full suite and continuous human playthrough not repeated.

Rebuilt `build/B2BJ.jar`; reopened and focused PlayForest PID475255 on the same separate playtest save. Normal save untouched. Evidence: `/home/ghiankylledomingo/tmp/b2bj-mob-visuals-2026-10-26/`. No commit or push. Subjective voice delivery and combat readability remain for player feedback.
