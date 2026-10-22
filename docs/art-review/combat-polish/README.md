# Combat UI and Slime balance — 2026-10-22

- [Player HUD](player-hud.png): original ornate frame, readable HP/Ichor counts, recessed shaded meters with stepped ends.
- [Ranged windup](enemy-windup.png): full game renderer, locked three-shot enemy windup. Short ground glints and charge sparks replace the old 400-world-pixel lines. Boss and melee area warnings retain their geometry.

Controlled Java renderer captures with temporary in-memory campaign state. No player save was read or written. These stills show appearance; they are not a claim of a complete encounter playtest.

Slime: 18 basic ATK, 0.60-second casting cooldown, 65% walking speed for the first 0.20 seconds of each cast. Dash cancels the slowdown immediately. Finisher/Tide remain 32 ATK; projectile speed, range and release timing remain unchanged. Regular enemies take 3–5 connected noncritical combo hits. See [balance notes](../../design/attack-feel.md).

Final Java 17 build passed all 81 assertion-test programs, including 112 warning/hit-geometry cases, and packaged asset loading. `git diff --check` clean.


Dash cooldown is now 0.32 seconds in both forms. Slime uses a longer 144-pixel water burst lasting 0.16 seconds; Blade uses a sharper 120-pixel quickstep lasting 0.12 seconds with blue pose afterimages. Both keep a vulnerable gap after movement. Reduced Effects retains a single starting Slime ripple and removes Blade echoes.


[Dash comparison](Dash-Variants.mp4) — four seconds at normal gameplay speed, 60 fps. Controlled campaign renderer captures show right/down/left/up in Slime, then Blade, with no save access. [Slime still](slime-dash.png) · [Blade still](human-dash.png).


## Sprite recovery — 2026-10-22

[Restored Rainoray, four directions](restored-four-directions.png) was rendered from the rebuilt JAR after recovering eleven accidentally replaced sprites from checkpoint `dee64ef`. The pre-repair files are preserved online in `0d6f3ff`. Historical source art remains available; the shared packer now blocks old three-facing sheets from replacing current human/morph assets. The real legacy packing command was tested and left all 371 runtime assets unchanged. All 81 game checks, packaged assets and the separate packing-tool regression passed. The screenshot uses temporary in-memory state with no save access.

Sword slash redesign remains pending. The existing Blade timer/aura draft was preserved, not completed as part of this recovery.
