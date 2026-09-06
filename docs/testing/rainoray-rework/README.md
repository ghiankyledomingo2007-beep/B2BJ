# Rainoray / Ruined Outpost pass — 2026-09-07

## Result

Rainoray now uses the ivory mask, black eye slits and teal forehead crest from the supplied concept reference, with a distinct teal/navy outfit. Only the mask reference was taken from the concept document; it did not replace the existing game's design.

- Seven human animation sheets: idle, run, dash, three-stage cuts, cast, guard and hurt. Native 80×80 cells, shared 2× character pixels, ground row 72; three authored directions with west mirrored from east.
- Six directional, 16-pose transformation/reversion sequences. Combat interrupts the visual morph immediately rather than locking input.
- Three 16-pose whole-body engulf sequences. The body follows the corpse during the splash, swallows it, then returns; cancelling restores uneaten remains.
- Human RMB: Ichor Crescent, 5-second cooldown. F: one-hit Riposte, 0.3-second window and 6-second cooldown. Each costs 8 Ichor and requires more than 8 remaining. Reduced effects moved to V.
- Pixel-art warning rings, direction arrows, aim reticle, fissures, parry/impact effects and masked portrait. Ring margins no longer shrink the displayed danger radius; Spitter tells cover the actual 400-pixel shot path.
- 26 map-dressing placements, including an eight-pose brazier. Upright cart/brazier feet block movement and shots; flat rubble/shields remain walkable.
- At half health, the Warden adds alternating horizontal/vertical fissures: 1.1-second committed warning, 0.32-second active window. No extra boss health or damage multiplier.

The open outdoor routes remain. This is still the Ruined Outpost, not a new biome.

## Verification report

- Build / Java types: PASS — Java 17, `-Xlint:all`, no compile errors or warnings.
- Tests: PASS — all 42 Java assertion programs, plus packaged-asset checks from outside the repository.
- Helper tests: PASS — three Python credential/allowlist tests and the pixel-preserving packing check.
- Coverage: not instrumented; no percentage claimed.
- Security: no credential patterns found in scoped source, tests, tools, new art manifests or reports. No third-party runtime dependencies were added; npm/pip dependency audits are not applicable to this Java/stdlib-only change.
- Diff whitespace: PASS. Visual fixtures are staged screenshots, not a claim of a complete human playthrough.
- Build was verified with a 256 MB Java heap. Game window stayed closed.

The final screenshot pass also corrected disconnected horizontal fissures, oversized shockwave rims and a map marker that mistakenly used Rainoray's portrait for the Warden. The full build passed again after those fixes. See the [final visual review and screenshots](../rainoray-pass/README.md).

Six deterministic boss scenarios completed: five wins, one death, zero timeouts, all reaching phase two. See [scenario evidence](boss-scenarios.md) and [gameplay details](gameplay.md). The strongest remaining balance concern is damage during reversion; the automated policy only landed one natural parry, so Riposte still needs hands-on timing tests.

## Art and credits

114 generations were used in this pass: the current token's 34 remaining, then both 40-generation backups. All three finished at zero remaining. The ledger contains 82 image/animation jobs (660 returned PNG frames), plus one character-rotation job. Alternatives were reviewed; rejected artwork was not silently substituted into the game.

[Generation ledger](../../art-review/rainoray-rework/jobs.json), [character reference](../../art-review/rainoray-rework/characters.json), [visual acceptance/rejection notes](art-review.md), and [reproducible native-frame packer](../../../tools/pack-rainoray.sh).

Examples of rejected work: north animations turning sideways, a pentagram that obscured terrain, a downward lane arrow, and a side-engulf retry that stayed stretched too long. Accepted art keeps transparent margins and exact native pixels; no invented duplicate poses were used to fill sheets.

PixelLab's [official MCP documentation](https://api.pixellab.ai/mcp/docs) was read for generation, animation, status and balance operations. Keys stayed in temporary process environments; no shared Codex configuration was changed, and those temporary sessions are now closed. Tokens pasted in chat should be rotated before future use.

## Checkpoints and laptop state

The pre-pass checkpoint `446420e` was pushed to [the owner's personal repository](https://github.com/ghiankyledomingo2007-beep/B2BJ); [its GitHub verification run passed](https://github.com/ghiankyledomingo2007-beep/B2BJ/actions/runs/34048774096). This pass is saved locally; no new release tag or installer was published.

The game, Spotify and Discord were closed. Claude/Codex CLI agents and terminals were preserved. Browsers, open documents and Claude desktop were left alone to avoid interrupting other work or losing unsaved state.

When ready to test, run `java -jar build/B2BJ.jar` from the project. It is intentionally not running overnight.

## What still needs a person

Test parry timing, reversion pressure, fast directional changes during combat, and whether the mask/engulf motion feels right at play speed. Audio tests passed, but this headless pass did not include listening through the laptop speakers. No claim that the whole game is finished or bug-free.

TDD drove regressions for invisible-player fallback, corpse coverage/interruption, exact warning boundaries, Spitter range and brazier frame cropping. Ponytail kept these inside existing combat, map and rendering systems without a new engine layer or dependency. The verification-loop gate and [self-evaluation](self-evaluation.md) explicitly separate passing checks from unverified live feel.
