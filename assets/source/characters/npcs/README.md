# Reincarnation cast atlas — 2026-10-26

`tensura-cast.generated.png` is the preserved transparent 1619×971 master generated with the image-generation tool for the accepted alternate-continuity cast. Columns, left to right: Shuna, Kaijin, Eren, Rigurd, Souei. Rows: standing/injured, alternate/resting, close-up portraits. The prompts requested a consistent pixel RPG cast with small readable faces, a limited palette, front/three-quarter silhouettes, pink-haired shrine-attendant Shuna, a brown-haired craftsman, a blonde adventurer, a green goblin elder and a blue-haired scout. No existing runtime artwork was overwritten until the master had been generated and inspected.

`ImportedArt.reincarnation()` cuts rows at 40% and 75% of the master height, trims transparent margins, centre-crops square headshots, samples at native resolution, quantizes to a fixed 15-colour palette per character and applies hard alpha. Standing survivors fit 60×52, resting survivors 60×50, with opaque feet grounded at y=63 in 64×64 cells. Shuna and Souei use 80×80 cells; portraits use 64×64 cells. Shuna retains the existing procedural breathing/wind animation; Souei uses two idle poses. Portraits are static.

Runtime files retain historical names to preserve code/asset contracts:

| Runtime file | Current content |
| --- | --- |
| `assets/characters/npcs/mara_idle.png`, `mara_portrait.png` | Shuna |
| `assets/characters/npcs/sedge_idle.png`, `sedge_portrait.png` | Souei |
| `assets/characters/npcs/survivor_portraits.png` | Kaijin, Eren, Rigurd |
| `assets/props/outpost/field_survivor.png` | Same three people; injured and camp rows |

Rebuild with `tools/art/build-art.sh reincarnation`. Use `--force` only when deliberately replacing manually retouched runtime images. The `all`, `portraits`, `survivors`, `sedge` and `field` scopes also finish with this import so historical exporters cannot restore the retired cast. Earlier generated masters are retained as historical sources, not current character depictions.

Generation output: `exec-7679e4c3-9858-49a0-90e0-1ea4fe1bb7c1.png` from imagegen session `01a14f3c-ce5f-7ea1-8a0d-f73da12fb96d`. The source master is not included in the playable JAR.
