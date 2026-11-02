# Beelzebub anime-reference revision — 2026-10-28

## Request and reference

Owner clarified that the prior R-key fix did not address the expected animation or visual identity. Inspected the licensed [Muse Philippines episode 56](https://www.youtube.com/watch?v=x8J-KpX2oHo&t=1196s), particularly 19:56 and 20:16: black flame-like fringes and strong violet energy around Rimuru's extended hand. Also inspected the [official VIBRATION STARS scene reference](https://www.ten-sura.com/goods_event/goods/goods/post/10149), explicitly described as Beelzebub during the season-three Hinata fight. Crunchyroll's Clayman clip was region-restricted here; no restriction was bypassed.

This is a pixel-art adaptation for the existing human-form corpse-harvest ability, not a claim to reproduce every anime use or grant instant kills/projectile absorption.

## Change

- Replaced thin purple lines and outline rings with a larger, filled black/violet hand vortex, thick shaded inward tendrils, engulfing clouds and restrained moving motes.
- Cast has a short opening beat, accelerated draw-in, then a collapsing vortex. Remains lift toward the visible hand position and shrink away inside it. Motion is driven by simulation progress, so pausing freezes it.
- Kept human-form R, radius420, cast1.15s, cooldown10s, existing rewards, cover checks and interruption rules. First18% of the cast opens the vortex before moving bodies; pull finishes at88% to leave a closing beat.
- Added an original deterministic1.15s suction/tear/collapse recording through the existing stoppable sound channel. Separate from blade charging; stops on interruption, death/reversion, pause and completion. Uses existing stdlib DSP; no runtime dependency or anime audio sample.
- Reduced-effects mode retains the readable core and thinner currents while omitting engulfing clouds, motes and glow; spinning is disabled.

## Validation

Java17/-Xlint:all build passed all126 test programs and packaged-assets verification. After the final clouds-only visual refinement, recompiled the renderer and passed BeelzebubTest plus PackagedAssetLoadingTest again against the candidate JAR from outside the repository; installed that verified candidate. Skill test covers actual R bindings, corpse movement/rewards/cooldown, cover, summons, expiry, interruptions, pause, reduced effects, sound and1280/1920 rendering. Recorded fixture also asserts the cast and devour sound both end.

`generate_beelzebub_sfx.py --check` passes deterministic bytes, native format, duration, peak and faded edges. `check_japanese_bank.py` confirms all349 final Japanese lines/cast records; all349 packaged voice WAVs and the new sound hash-match runtime assets. `git diff --check` passes. Final preview is `/home/ghiankylledomingo/Downloads/B2BJ-Beelzebub-Preview/Beelzebub-preview.mp4`.

Restarted only the previously owned regular-game process into the refreshed fullscreen JAR. Startup remained alive without log errors; saved campaign bytes were unchanged. PID/log and pre-restart save copy are in staging.

Preview uses the actual campaign renderer and audio mixer in an in-memory fixture with three remains, not the player's save. Four seconds,1280x720,60fps. Mixed PCM peak23635/32768, zero saturated samples. Reviewed wind-up, pull-in and collapse frames. Capture tool/logs and before-edit copies are in `/home/ghiankylledomingo/tmp/b2bj-beelzebub-anime-2026-10-28/`.

Limits: no full manual chapter playthrough or hardware-speaker listening review. The effect is an adaptation of the reference; visual preference still requires the owner's judgment. No commit or push.

## Trailer coordination

Owner is building the trailer with Claude. Existing approved voice handoff remains authoritative; added `docs/design/claude-trailer-voice-prompt.md` so Claude replaces its temporary Sora/Hijiri choices with selected Rainoray B and Raphael, reuses final game recordings, keeps Japanese speech/English captions, and writes trailer outputs separately from the game.

Owner asked about actual anime music/SFX. Explained that the permission would need to cover those recordings too; school/personal use is not blanket clearance ([IPOPHL FAQ](https://www.ipophil.gov.ph/help-and-support/copyright/)). Recommended instrumental gameplay beds to preserve dialogue clarity. No anime soundtrack was downloaded or installed in this task.
