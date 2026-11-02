# Selected loading-screen logo — 2026-10-30

Ghian selected concept03, Moonlit Gate, from the loading/logo gallery. `moonlit-gate-logo.png` is a
byte-for-byte copy of the approved transparent logo at
`docs/art-review/loading-gallery-2026-10-30/assets/logo-moonlit-gate.png`.
The original built-in imagegen prompt/provenance remains in that gallery's `concepts.json`.

`UiArt.splash()` centers this artwork at448×149 on a640×360 dark navy background. A thin128×2
loading line uses the unchanged geometry from `Splash.java`. Aspect ratio and alpha are preserved;
only display scaling and background composition are applied. No new artwork or lettering generated.

Rebuild only the splash with `tools/art/build-art.sh splash`. The source master is not packaged;
the JVM reads the generated `assets/ui/splash.png` through the existing JAR manifest.

## Gatekeeper main menu selected — 2026-10-30

Ghian chose concept01 from `docs/art-review/main-menu-gallery-2026-10-30/`.
`TitleScreen` now uses this same logo above the left-side menu. The packaged copy,
`assets/ui/title/moonlit-gate-logo.png`, is byte-identical to this source master;
the game preserves its aspect ratio and scales it only for display. If the approved
master changes, copy it to that runtime path and regenerate the splash so both stay
consistent. `TitleArt` continues to regenerate only the background illustration.
