# Retired assets — 2026-10-23

These 22 files were moved out of the game's asset folders without changing their
bytes. [manifest.tsv](manifest.tsv) records each original path, archived path,
SHA-256 hash and reason. The archive is versioned but excluded from the JAR.

- Five old Blade sheets, two old slime sheets, three unused effect sheets, five
  prototype environment props, one stone pillar and two old UI icons had no runtime
  consumer. The unused vitality image load and tests requiring retired art were removed.
- Two concept sheets are reference artwork, not game assets.
- `supplies.png` and its editable project duplicate the active `set_supplies.png`
  artwork. Both supplies render paths now use that canonical sheet.

The current `rainoray_*.png` sprites, active legacy room tiles and Lab 6 assets stay
in `assets/`. Character generation masters moved to `assets/source/characters/`;
they remain inputs to the native-size importer.

For a deliberate restoration, copy only the needed archived file and update its
consumer, build checks and this manifest together. Do not copy the whole archive
back into runtime folders. `AssetArchiveTest` verifies the hashes and rejects
accidental restoration or packaging of these retired paths.
