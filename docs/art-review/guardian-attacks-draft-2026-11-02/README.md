# Guardian attack drafts — 2026-11-02

Corrected real-engine preview and new audio: http://127.0.0.1:4517/artifact/3833bd09a217/native.html

The native build in `tools/guardian-preview/` supersedes Fight flow for gameplay testing. The browser damage measurements below used placeholder shots and are not real-kit balance evidence.

Archived study: http://127.0.0.1:4517/artifact/3833bd09a217/

**Fight flow** now demonstrates a moving boss against a circling player. Click **Take control**: WASD/arrows move, Space dashes, hold left mouse to shoot toward the cursor. Use the phase buttons to compare physical pressure with phase-two detached fists and a turning chest beam. Watch demo and Replay remain available. Manual play pauses when the window loses focus.

The boss closes distance, predicts a capped point ahead during preparation, then locks attacks. Rush cannot steer; the backhand leaves the rear safe; all attacks expose the core during recovery. Phase II alternates physical attacks with core spells. Original violet/gold/cyan character colors are retained. Earlier individual effect studies remain in the other tabs.

This is a local playable prototype, not an installed game update. Scenery props have no collision; player attacks are simplified and the full player skill kit is absent. Live game balance and a physical-input playthrough remain untested. Existing game sprites and six Warden sound cues are reused.

Checks: verifyGuardianDraft() covers seven staged variants; verifyGuardianPressure() covers12 movement/commitment/recovery/input-rule scenarios,60-second arena runs and phase-two demo coverage. Scripted browser input events exercised movement, dash, firing and release. See design.md for exact proposed timings, controls, observed simulation outcomes and limits. verification.json records the checks and unchanged production hashes.

Baseline and pre-change files: /home/ghiankylledomingo/tmp/b2bj-guardian-pressure-2026-11-02/. No game restart, save edit, commit or push.
