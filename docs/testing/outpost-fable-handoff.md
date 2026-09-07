# Ruined Outpost — Fable 5.1 handoff pass

Started 2026-09-07. Worktree `.claude/worktrees/outpost-art-fable`, branch `outpost-art-fable` created from `main` at `7d3d2bd` (the worktree's original branch `worktree-outpost-art-fable` pointed at the stale `446420e`; fast-forward and rebase were both refused by the session's permission classifier, so a fresh branch from `main` was created in place instead).

## Environment report (checked first, as instructed)

| Item | Status |
| --- | --- |
| Model | `claude-fable-5-1[1m]` (launcher argument, session model ID `claude-fable-5-1`). No fallback used. |
| Ultracode | Active for this session (`--settings {"ultracode":true}`, confirmed by the session reminder). |
| `PIXELLAB_AUTH_HEADER` | present |
| `B2BJ_PIXELLAB_BACKUP_AUTH` | present |
| `B2BJ_PIXELLAB_PREVIOUS_AUTH` | present |

Presence only was checked; values were not printed, copied or written anywhere. All PixelLab calls go through `tools/pixellab-call.py` with the temporary-token override.

### Balances at start (via helper `get_balance`, no charge)

| Allocation | remaining / used / total |
| --- | --- |
| `PIXELLAB_AUTH_HEADER` (first backup) | 40 / 0 / 40 (trial) |
| `B2BJ_PIXELLAB_BACKUP_AUTH` (newest backup) | 40 / 0 / 40 (trial) |
| `B2BJ_PIXELLAB_PREVIOUS_AUTH` (older) | 3 / 37 / 40 (trial) |

### Immediate finding: offline helper test leaked the live header

`python3 tools/test_pixellab_call.py` failed in `test_private_auth_and_image_encoding_never_leak_to_output` because the test assumes `PIXELLAB_AUTH_HEADER` is unset and lets the real environment through; with the launcher's live variable present, the assertion message printed the live bearer value into the terminal. Fixed by isolating the environment inside that test (`patch.dict(os.environ, …, clear=True)` without the variable). All three offline tests now pass. The value was not written to any file; treat the earlier terminal output of that failure as exposed if the log is retained.

## Work log

(in progress)
