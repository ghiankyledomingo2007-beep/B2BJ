# Companion voices and boss presence — 2026-10-26

The 75–105-second companion cooldown and combat cancellation could make Rainoray/Raphael sound absent or cut off. Automatic speech now follows each full WAV plus a 0.30-second reply gap (minimum display 1.2 seconds). Ongoing exchanges finish through ordinary combat; boss milestones wait for current speech. Explicit NPC interaction, skip/exit, death and pause keep their expected control. Post-fight conversation becomes eligible within 12 seconds; quiet exploration normally starts a new exchange every 24–34 seconds. Identification no longer erases pending fight context. Unheard exchanges are preferred; after the bank is heard, each pair has a 180-second session repeat gap. Only completed casual exchanges are marked heard in the save.

Added 16 two-line exchanges, six Raphael hurt responses, four Warden attack/low-health lines, and 11 Rainoray combat efforts. Rainoray and Raphael retain their approved cast profiles. All Kaijin, Eren, Souei and Warden dialogue was recast consistently to Bill, Jessica, Eric and Callum respectively. Warden pitch distortion was removed. Current runtime: 331 dialogue clips (211 Outpost, 120 Forest) and 18 efforts (6 light, 4 heavy, 8 hurt). Eight Raphael hurt responses rotate with a 15-second cooldown and a brief delay to leave room for Rainoray's own hurt effort.

Claude Code implemented 16 original procedural boss effects (8 families, 2 takes), plus physical attack visuals in `BossFx`: Warden blade arcs, stone chips/dust, rush furrows, moving wave debris and fissure fragments; Briarheart root bows, torn fibres, seed husks and closing cage branches. Windup sound starts once 0.62 seconds before release. Actual impact events dispatch exactly one matching release, hit or miss, within the existing 700-pixel enemy audio range. No added warning overlays or camera shake. Pause, death, region/session reset, particle bounds and reduced effects are covered.

## Validation

- Java 17 build with `-Xlint:all` passed.
- 23 focused test programs passed against the staged JAR from `/tmp`: CompanionCadenceTest, CombatVoiceTest, OutpostBanterTest, InnerVoiceOverlayTest, SageCombatTest, MobIdentificationTest, ChapterOneVoicesTest, ChapterTwoVoicesTest, ForestChapterTest, GameAudioTest, BossSfxTest, BossEffectsTest, WardenPacingTest, GuardianTest, ForestCombatTest, CombatSfxTest, ForestSfxTest, ReincarnationOpeningTest, ForestAttackSoundTest, BriarheartCombatTest, WardenFxTest, GuardianPresentationTest, BeelzebubTest.
- `GuardianPresentationTest` had an old 1.3-second Forest expectation that also failed against the previous JAR. Updated it to the already implemented 1.65-second preparation; no combat timing was changed in this pass.
- All 331 dialogue filenames, speakers and text match the Java script export. All 349 runtime voice/effort hashes match the manifest; native mono PCM16 / 22,050 Hz and peak checks pass.
- All 64 new/replacement dialogue clips were transcribed; only lifeform/life form tokenization differs. The 11 short efforts were also checked: interjections have ASR spelling differences and one nonverbal Ugh is untranscribed. Automated checks do not certify acting or pronunciation.
- `generate_boss_sfx.py --check` matches all 16 native WAVs. Claude's checks also confirmed the existing Forest/combat generator outputs were unchanged.
- 28 boss frames were rendered from the packaged game. Warden sweep/fissure and Briarheart roots/cage inspected visually. Frame sets and test logs: `/home/ghiankylledomingo/tmp/b2bj-companion-flow-2026-10-26/`.
- Installed JAR atomically, retaining the previous build in staging. SHA-256: `835828ca6b7b8f7253f91885e4a4da08cf5fcd82b0d91a0b24824f00ab401dee`.
- Opened an actual 1920×1080 separate Warden playtest. Its audio stream is unmuted; no launch errors. Normal campaign save hash remained unchanged. The pinned launcher uses the updated JAR.

The full repository suite and a continuous human playthrough were not repeated. Voice acting, final sound character and perceived combat readability still need player listening/playtesting.
