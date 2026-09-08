# Fallen Knight animation pass — 2026-09-08

RED `017dabe`: the expanded melee render matrix failed because FALLEN_KNIGHT
did not render reviewed row 1, frame 0 at native 2x. Guard cases remained green.

The existing guard render/edible-corpse path now also handles the knight. Its
atlas uses 92px cells, foot anchor 76, rather than the guard's 88px/72. PixelLab
export JSON fixes the row mapping: march 1–4, collapse 5–8, hurt 9–12, cleave
13–16, in South/East/North/West order. Both weapons remain in-frame.

After contact-sheet review, cleave anticipation uses frames 0–8, contact 9–11,
recovery 12–15. The south sword is still raised at frame 8, so copying the
guard's 8/4/4 split would make damage start before the cleave. Existing knight
shield/damage/recovery rules are unchanged. March, hurt and collapse retain
separate cycles and fixed native pixel scale. The shield label clears the helmet.

`CampaignGuardArtTest` now exercises both identities through four facings,
walk/tell/contact/recovery/hurt, direction lock, corpse identity and final-pose
hold. `OutpostVarietyTest` and `CampaignEnemyTest` pass without combat changes.
`./build.sh` verifies the complete suite; no numerical coverage is claimed.

Character `c812f94a-5210-4309-8aed-488660a091c3`; source and complete animation
group IDs are in `assets/characters/campaign/fallen_knight.json`.

## Wolf work not accepted for runtime

- `005035db-8585-44e3-aae1-8597e69d8d18`: detailed Pixen wolf reference;
  good side silhouette, but not the requested front view.
- `012df782-74a0-41b0-87fd-33afc954f6f1`: reference rotations retain incorrect
  facing; not suitable for directional attack art.
- `1190deeb-4902-4178-a98e-166f5b7b520a`: correctly oriented standard quadruped,
  but flat fur shading lacks the accepted guard/knight detail.
- `e6df7a18-d2e5-474d-8ba0-ac87a32a7136`: four-view Pro restyle, output64px,
  20 generations from backup2. Facing improved but the detail remains too flat.
  Not integrated. No wolf animation was commissioned from these rejected bases.

The Pro job exceeded the small remaining allowance on backup1, so backup2 was
used for that batch without discarding backup1's remaining generations.
Live balances after this review: backup1 8, backup2 20, backup3 40 (68 total).
Balances may change on this shared account; do not attribute every provider debit
to this session. Private keys remain outside the repository.
The three backups are in `~/.config/pixellab/b2bj-session/keys.json`, in supplied
order. The existing `tools/pixellab-call.py` accepts a temporary
`PIXELLAB_AUTH_HEADER` for scoped calls; global Codex/Claude config was not
changed and other CLI agents were not interrupted.
