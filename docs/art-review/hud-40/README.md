# HUD and absorption batch

**First allowance: 40/40 used, 0 remaining. All 21 jobs completed.**
The provider charged 20 one-generation jobs plus one 20-generation, seven-icon
consistent-edit job. Queued work was counted against the limit even before the
provider balance charged completion. Four initial submissions hit the eight-job
concurrency limit, were not charged, and were retried after slots cleared.

[Requests](requests.json), [orb/remains animations](animation-requests.json),
[feeding requests](absorption-requests.json), [icon polish](polish-request.json),
[job ledger](jobs.json). Credentials were not stored in these files or config.
The first credential-bearing process was closed before using the second allowance.

## Accepted and rejected

- Orb pulse: reviewed eight selected phases. Pickup: selected contraction/spark
  phases, excluding **frame 14's opaque background**. Both crop rows 27–31 to
  remove an unwanted baked shadow; no scaling or repainting of source pixels.
- Remains idle: selected eight subtle pulses. Consume animation did not dissolve
  completely, so the runtime draws it toward the Blob and fades its final stage.
- Feeding: front poses with protruding gold strands are excluded. North frames
  with more than three pixels of floor drift are excluded; selected poses receive
  integer translation to foot row 47. Side source faces west and mirrors for east.
  Concurrent job return order differed: front is `bcc9...`, side is `655b...`.
  Archive names were corrected after comparing returned frame 0 with input art.
- Initial absorption-effect anchor produced a solid circular base and is rejected.
  The replacement thin tendril source is animated in the second allowance.
- Icon polish: frames 1, 2, 4, 5, 6 improve the Blob portrait, helmet, wave, dash
  and absorption icon. Original vitality and slash are retained because the edits
  reduced readability. Original framed HUD and slot art are accepted.

[`pack-hud.sh`](../../../tools/pack-hud.sh) contains exact frame selections.
Raw rejected art remains here, not silently inserted into the game. All sprite
rendering uses native 1×/2× pixels; Warden remains the earlier requested 3×.

See [verification and design changes](../../testing/hud-40/README.md) and the
[second allowance ledger](../hud-finish-40/jobs.json) for finishing assets.
