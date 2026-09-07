# Motion polish handoff (historical pause)

The user subsequently authorized **continue without Fable**. The Fable-only restriction and authentication blocker below describe the earlier pause, not the current task constraint. See `docs/testing/motion-polish/` for the final implementation and verification record.

## Dispatch blocker

Claude Code 2.1.263 supports `--model fable`, but the read-only model probe failed before inference: `Failed to authenticate: OAuth session expired and could not be refreshed`. No Fable worker was dispatched. User must restore Claude CLI authentication. No auth settings or existing CLI agents were changed.

## Current state

- Project: `/home/ghiankylledomingo/gamedev-vault/Projects/B2BJ`.
- Before the Fable-only instruction, scoped Java work completed: `c6174b5` / `f03ed83` add tested exact-once Tide release/natural-expiry events; `fc2efe5` / `66a3221` add tested seven-row/eight-pose Warden animation selection. Damage, movement, attack timing and cooldowns unchanged.
- Root-authored `test/MotionPolishRenderingTest.java` remains untracked and intentionally RED: cadence, broad low side engulf coverage, new native effect assets. All three targets executed and failed for their intended missing behavior. No production renderer/cadence change made yet.
- `requests.json` contains proposed jobs. **Not all were submitted.** Only the eight entries in `jobs.json` were accepted: sprint south/east/north, side engulf, Warden idle/approach/windup/recover. These cost 9 generations of the verified 40 allocation; expected remainder 31, verify server before any spending.
- Server concurrency cap is 8. Further attempts for Warden charge/death and Tide crest/release were rejected for concurrency; no IDs returned. Tide break/foam and Warden impact were not submitted.
- Job IDs are durable. Poll accepted jobs and download before resubmitting anything. Existing `tools/pixellab-call.py` reads temporary `PIXELLAB_AUTH_HEADER`; never place credentials in project files, logs, prompts, or shared config. Orchestrator's temporary credential session is closed at handoff.
- Existing game may still be running an older built JAR. Do not close unrelated agents/apps or rebuild/restart the game without coordinating with user.

## Ownership / next work

Original proposed split: one worker owns art generation/review/packing; one owns B2BJ renderer, BladeAnimation cadence, GameAudio and integration tests. They share a worktree: do not revert one another's work, stage/commit only owned files, no push.

Art: side engulf should flatten into a broad puddle over the remains, not stretch into an upright neck. Human run must show actual sprint strides and airborne phases, preserving the existing mask/outfit/direction. Warden motion atlas is 512x448 (64px cells, eight columns; rows IDLE, APPROACH, WINDUP, STRIKE, RECOVER, CHARGE, DEATH). Generate strike from the reviewed windup endpoint; recovery must start coherently after strike. Preserve native ground row56 and3x draw size. Human cells80/ground72/2x; engulf cells80/16columns/three rows, canonical west mirrored for east.

Renderer: `Guardian.animation().ordinal()` and `animationFrame()` expose new atlas contract. Keep death full192px size, not legacy shrinking scale. Preserve old sheets as missing-art fallback. Source-anchored Tide release burst via existing bounded impacts; distinct rolling crest/foam wake; impact splash versus quieter expiry foam. Proposed new effect sheets: tide_crest48, tide_release64, tide_break64, tide_foam32, warden_impact64, each8frames. Root test expects these paths. Leave normal LMB behavior intact.

Tide events already implemented: `TIDE_RELEASE` at actual heavy projectile spawn, `TIDE_DISSIPATE` once on natural expiry, `TIDE_IMPACT` collision only. Use event coordinates, not moving player coordinates. Add distinct Tide release/impact sound cues using existing synthesis (no dependency). Keep .18s windup,4.5s cooldown,2damage,440initialspeed,28radius,420impulse,280range/.8s lifetime.

## Verification

All shell commands via `rtk proxy`; local text/code edits via `apply_patch`. Follow applicable skills. Do not claim measured coverage: no coverage instrumentation yet. Use Java17 assertions/headless checks with256MB heap; `build.sh` is full verification. Main RED command compiled all source plus MotionPolishRenderingTest and ran separate `cadence`, `engulf`, `effects` arguments. Review generated contact sheets AND staged runtime frames before selection. Raw returned frame0 is input; animate_image returns requested_count+1 frames. Existing tools CropSprite/ReviewAnimation/PackFrames preserve source pixels. Do not floor-lock airborne motion blindly.

Identity for scoped local checkpoint commits only: `git -c user.name='Ghian Kylle Domingo' -c user.email='259288340+ghiankyledomingo2007-beep@users.noreply.github.com' commit ...`. No global git config changes. Existing dirty files belong to this unfinished pass and must be preserved.
