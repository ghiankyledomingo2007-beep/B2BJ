# Attack effects — 2026-10-23

Removed the red circles, bands, rectangles and long lunge arrows. Ordinary enemies
keep a small direction glint during windup. Bosses use their windup poses and
effects; targeted slams and fissures now disturb the ground with dust before impact.

The blue bars under the Warden's spikes were part of the sprite sheet. Both Warden
and Briarheart spikes now have broken rubble and small glints instead. Shockwave
spikes follow the circular damage path; the previous effect was vertically squashed
and shifted downward. Damage, attack timings and saves were not changed.

- [Outpost shockwave](outpost-shockwave.png)
- [Outpost targeted-slam windup](outpost-target.png)
- [Forest fissure windup](forest-fissure.png)

These are deterministic renders of the packaged game with a temporary test session,
captured from `/tmp` so assets must load from the JAR. They do not represent a full
manual fight playthrough.

Validation: all 81 Java test programs and packaged-asset checks passed. Updated
coverage checks absent boss overlays, compact enemy headings, unchanged charge and
fissure hit boundaries, spike placement and sprite art, and dust windups for both bosses.

[Tilesets by area](../../../assets/tilesets/README.md) documents the separate asset
organization change. Existing campaign tile pixels and map layouts are unchanged.
