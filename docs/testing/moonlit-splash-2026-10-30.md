# Selected Moonlit Gate loading logo — 2026-10-30

Owner selected the concept03 title-logo image. The boot splash now uses that exact logo on a
plain dark navy background with the existing thin turquoise loading line. Preserved source:
`assets/source/ui/splash/moonlit-gate-logo.png`, byte-identical to the approved gallery asset.
Rendered at448×149 within640×360, preserving aspect ratio/alpha. No additional art generation.

Updated the splash generator and current art documentation. Removed the superseded unused pixel
crest method; old art and a complete before-edit source/JAR snapshot are retained. Main menu,
chapter loading scenes, audio, game logic, animations and saves were not edited.

Verification: art generator compiles all current production/art sources; scoped splash generation
passes. Viewed native640×360 output. An assertion-based check ran against the candidate JAR from
`/tmp`, checking image loading, dimensions, background, loading-line bounds and logo presence.
The installed JVM loading-line constants match the generator. The approved source matches by bytes.
Every JAR entry compared: only `assets/ui/splash.png` changed. `git diff --check` passes.

Installed JAR SHA256: `be00dcc966965399b76db0a98e80dbf71ee5dd37a4165abc0368536c10f5316c`.
Campaign-save hash unchanged across installation. No forced game restart; appears on next launch.
No full game-suite or gameplay replay. Backups and checks:
`/home/ghiankylledomingo/tmp/b2bj-moonlit-splash-2026-10-30/`.
