# Ruined Outpost — 27-generation batch, 2026-09-06

Owner instruction: use the entire remaining PixelLab allowance for this game.
Starting balance: 27 remaining / 13 used. Final live balance: **0 remaining /
40 used**, $0 purchased credit. Exactly 27 accepted one-generation jobs completed;
no subscription upgrade, new credit purchase, credential change or limit change.
Four animation requests initially hit the eight-job concurrency limit, created
no jobs, and were retried after slots became available.

The batch contains eight props, twelve character motion studies, three effect
anchors, three effect animations, and one flag animation. All **155 PNG results**
are saved locally. Exact IDs, prompts, receipts and individual decisions are in
[jobs.json](jobs.json). Download links may expire; local PNGs are authoritative.

## Integration decisions

- **7 jobs contributed to runtime:** barrel, dead tree, Blob idle, Warden slam,
  Warden recovery, teal impact anchor and its animation.
- **8 jobs staged:** waypost, oversized fallen equipment, south/north sword cuts,
  south thrust, flag flutter, dust anchor and dust breakup. These are not approved
  as complete runtime assets; individual remaining checks are recorded in the ledger.
- **12 jobs rejected:** four wrong-perspective props, three wrong-facing/clipped
  Blade attacks, three weak/artifacted Blob motion studies, and the solid-disc
  stone effect plus its non-dissipating animation. They remain review evidence,
  not runtime packaging.

Spending the allowance was not permission to include every generated image.
In-scene review removed the helmet/spear prop because it was oversized relative
to Blade. The removed runtime copy is recoverable as `oversized-remains-runtime.png`;
the original `fallen-knight.png` also remains here.

## Runtime changes

- Blob south idle: eight distinct blink/wobble poses at 6 fps, native 48×48
  rendered at 2×. Source frames 0–7; duplicate endpoint 8 excluded. Two frames
  move down one source pixel during packing to keep their ground line at 47.
- Warden: unchanged 64×64 cell / 2× scale and combat timings. Existing four-frame
  state rows now use reviewed raised-arm anticipation, downward impact and
  settling recovery. Original sheet preserved as `guardian-actions-before-batch.png`.
  Row selections: slam study [0,1,2,3], [4,5,5,6], then slam [6,7] and recovery [1,8].
  Repeated impact frame intentionally holds contact for 90 ms; it is not a loop.
- Blob hits: nine distinct 32×32 frames, 27 fps over the existing one-third-second
  impact lifetime, native 2×. Visible pixels decrease 238→222→163→117→77→54→35→13→13.
  Gold Blade impacts and red danger effects keep their separate palettes.
- Supply Lane: four individually grounded barrels replace two long procedural
  fence strips. Barrel footprint 64×32; source anchor 24,53.
- Rain Ditch: two bare trees with 48×24 root/trunk footprints; source anchor 31,102.
  Tree/root decoration and barrel moss do not turn the full canvas into collision.

## Checks and reproduction

`./build.sh`: all 17 assertion programs pass; Java 17-compatible compile with
all lint warnings; packaged assets also load outside the repo. Six standing prop
types now have cardinal approach checks; the barrel fixture approaches the outer
barrel instead of walking through its neighbour. All section exits remain reachable.
No numeric line-coverage claim, Windows installer run, or full unassisted playthrough.

`tools/ReviewAnimation.java` generates nearest-neighbour contact sheets and
reports opacity, palettes, bounds and distinct frames. Empty terminal dust frames
are valid transparent end states, not opaque-background failures.
`tools/PackFrames.java` joins selected frames without resizing/recolouring; optional
floor correction rejects shifts greater than three pixels. `tools/CropSprite.java`
extracts unchanged source anchors. `tools/fetch-outpost-batch.sh` downloads only the
recorded results, skips existing files, and creates no generation jobs.

Final-JAR scene fixtures were inspected and saved as `rain-ditch-runtime.png`,
`supply-lane-runtime.png`, `warden-windup-runtime.png`, `blob-impact-runtime.png`.
Reproduce, for example:

```sh
javac --release 17 -cp build/B2BJ.jar -d /tmp/b2bj-preview tools/PreviewOutpost.java
java -Djava.awt.headless=true -cp build/B2BJ.jar:/tmp/b2bj-preview PreviewOutpost 5 /tmp/supply-lane.png
```

These deliberate scene/actor fixtures do not certify boss difficulty. Running
game windows were left untouched; restart to load the new JAR, losing session-only
progress. No push, commit, release or publishing was performed.
