# Motion polish visual review

Status: all four requested preview groups executed and inspected against their final selected art: run, side-engulf, Guardian motion/death and Tide. This is staged visual evidence, not a claim that the entire game is visually finished or human-playtested.

`tools/PreviewMotionPolish.java` produces only files in this directory; it does not overwrite earlier Rainoray or HUD review captures. It creates a headless Swing panel, mutes `GameAudio` before constructing it, and never opens a window. Use a 256 MB heap.

## Planned evidence

- Four human running facings, eight actual `BladeAnimation` poses each. Initial 100 Ichor is a labeled fixture; Q and directional input use the normal action bindings.
- East/west engulf, sixteen channel poses each, plus before/after. The corpse is created by a real LMB kill; E starts the normal channel. Every captured pose asserts the player collision position stays fixed.
- Seven Guardian motion rows, eight normalized samples each. These are explicit reflected state/time fixtures to isolate renderer poses, not boss combat simulations. This prevents attack movement from changing framing and lets death samples be inspected without running repeated fights.
- Tide in four cardinal and two diagonal directions: actual RMB release, midflight, enemy impact or natural expiry, and foam finish. Enemy-hit fixtures require exactly the existing two damage; no forced hit or Tide event injection.

Every pose has a full 1280×720 staged screenshot. Contact sheets use native-size crops without resampling and identify pose/direction/phase. They are presentation aids; pose labels are not proof of correct artwork.

## Run after assets are ready

```bash
rtk proxy bash -c 'preview_build=$(mktemp -d /tmp/b2bj-motion-preview.XXXXXX); javac --release 17 -Xlint:all -d "$preview_build" src/*.java tools/PreviewMotionPolish.java && java -ea -Djava.awt.headless=true -Xmx256m -cp "$preview_build" PreviewMotionPolish'
```

Optional final argument: `run`, `engulf`, `boss`, or `tide` for a bounded subset. The default runs all four groups. Asset loading uses current project files, not an older packaged jar.

## Review checklist

Inspect the generated images before recording a result: four run silhouettes/mask stability/foot sliding; engulf side reach and complete silhouette return; Warden pose differentiation, anchors, clipping and death readability; Tide fixed-source release, directional crest, visible impact versus quiet expiry, and foam finish. Check native pixel alignment, collision readability and whether busy terrain hides the effect. Production regression tests and actual human playtest remain separate.

## Run and side-engulf review

Compiled current `src/*.java` and the tool with Java 17 `-Xlint:all`, then executed `PreviewMotionPolish run` and `PreviewMotionPolish engulf` with assertions, headless mode and a 256 MB heap. Both exited successfully. Produced 74 PNG files: 32 run poses plus four contacts; 32 engulf poses, four before/after images and two contacts. All 32 feeding poses verified stationary player collision; both actual kills and completed E channels succeeded.

Viewed all four running contact sheets, both 16-pose engulf contact sheets, both full engulf-after images and one full running screenshot:

- The initial run candidate kept the mask/scale but repeatedly raised a lowered sword during its cycle, reading as a draw/salute. Reported to the main agent and replaced. The linked [south](run-south-contact.png), [east](run-east-contact.png), [north](run-north-contact.png) and [west](run-west-contact.png) contact sheets now contain the final selected held-blade cycle reviewed below, not that rejected first candidate. Static contacts still cannot establish motion smoothness or absence of foot sliding at full speed.
- [East engulf](engulf-east-contact.png), [west engulf](engulf-west-contact.png): body flattens and spreads toward the remains, covers most of the purple silhouette by poses 4–6, hides it around the middle, then reforms. Early purple edges remain visible during the approach, appropriately preceding full cover. The original standing Blob is not left behind as a second body. Both [east after](engulf-east-after.png) and [west after](engulf-west-after.png) return to the original gameplay position with the correct facing and no corpse. Pickup art briefly overlaps the Blob after completion; this is existing feedback rather than an extra body.

The animation cadence under review is the current 14 FPS run code; the sampler observes actual `frame()` values rather than assuming the old 10 FPS. No GUI or audio output was opened. This initial run/engulf batch did not cover Guardian, Tide or the packaged build; the later Guardian/Tide batch is recorded below.

## Guardian and first-candidate Tide review

After the seven-row Guardian atlas was packed, compiled the current source and ran `PreviewMotionPolish boss` then `PreviewMotionPolish tide`, both headless with assertions and a 256 MB heap. Both exited successfully: 63 Guardian images (56 normalized poses plus seven contacts) and 60 Tide images (six directions × hit/miss × four stages plus twelve contacts). All six actual enemy-hit fixtures received exactly two damage; all twelve projectiles ended. No injected Tide effects, forced damage, or GUI/audio output was used.

Viewed every Guardian contact and every hit/miss Tide contact, plus the full final death sample. Guardian rows: [idle](boss-idle-contact.png), [approach](boss-approach-contact.png), [windup](boss-windup-contact.png), [strike](boss-strike-contact.png), [recover](boss-recover-contact.png), [charge](boss-charge-contact.png), [death](boss-death-contact.png).

- Guardian windup raises its arms, strike compresses the body, and recovery returns upright; those stages are visually differentiated. Horns/armor remain recognizable and the sampled death row stays visible without blinking. The initial death candidate read as a half-buried torso with a flat lower edge. Reported as an art concern and subsequently replaced; the linked death captures now show the final side-collapse reviewed below. These reflected samples do not prove transitions are smooth in live movement.
- First Tide candidate demonstrates source burst, moving crest, distinct enemy impact and quieter expiry in cardinal/diagonal directions. The visible ocean-strip rectangle and portal-like impact were already rejected by the main agent and are awaiting replacements; these screenshots must not be presented as final approved art.
- Additional first-candidate readability concern: the opaque source burst completely hides the Blob at the release and midflight sample times (about 0.04 and 0.20 seconds after release). Reported for a behind-body layer or more transparent treatment. Expiry foam is deliberately much quieter and can be difficult to see over dark terrain; a downward miss also reaches the bottom HUD region, which can occlude it.
- No functional runtime exception, failed input fixture, missing projectile termination, incorrect damage, or duplicate body was observed in these first-candidate runs. The later replacement Tide run is recorded below. No full build was run by this preview worker.

## Explicit raised-carry acceptance and final run review

The original test used an arbitrary maximum five-pixel absolute vertical excursion of the top bright pixel. That conflated a rigid sword position with the actual requirement: do not lower and redraw the sword during every run cycle. The main agent explicitly approved changing the contract to **the bright blade tip remains above the visible hood in every pose**, permitting ordinary wrist movement. This is a deliberate acceptance-criterion change, not a claim that north's movement was caused by torso bob or that the old test passed.

The revised `MotionPolishRenderingTest` scans the reviewed hood color `#45416d` for its top visible pixel and the existing bright-blade threshold (all RGB channels above 185). Both must exist, and the blade's top must be strictly above the hood. Mask highlights lie below the hood top, so the visible mask cannot by itself satisfy this criterion. This palette-specific check should be revisited if the character palette changes; it does not measure sword angle, temporal smoothness or foot sliding.

Measured source frames 0–7:

| Sequence | Absolute bright-tip excursion | Hood-relative excursion | Poses not raised above hood | Selection |
|---|---:|---:|---:|---|
| sprint-cycle-south | 2 px | 1 px | 0 | Accepted |
| sprint-locked-east | 6 px | 5 px | 0 | Accepted |
| sprint-cycle-north | 6 px | 6 px | 0 | Accepted |
| sprint-north | 33 px | 34 px | 5 | Rejected redraw control |
| sprint-locked-north | 13 px | 13 px | 3 | Rejected downward-tilt control |

Before changing the test, `MotionPolishRenderingTest carry` failed the original absolute gate on the then-packed atlas. The revised `carry-candidates` check read all five archived eight-pose sequences: the three selections passed and both rejected controls failed the raised-carry criterion. After the main agent packed cycle-south / locked-east / cycle-north 0–7, `MotionPolishRenderingTest carry` passed for all 24 atlas poses plus the same positive/negative controls. These small source-frame controls remain under `docs/art-review/motion-polish` and are intentionally exercised by the test.

Then recompiled and reran `PreviewMotionPolish run` with assertions, headless mode and a 256 MB heap: 36 updated run screenshots/contact sheets, exit 0. Viewed all four updated contacts (32 poses). All facings now keep the blade raised; the large lowered-to-upright redraw is gone. South has a stable carry, east/west share a consistent upright side carry, and north retains modest wrist tilt without dropping the tip below the hood. Mask, body scale and native pixels remain coherent, with no visible clipping in these staged samples. No asset or gameplay changes were made by this worker.

## Final Tide replacement review

After the main agent packed the isolated projectile loop and scatter finish and finalized the ground-layer burst rendering, recompiled current `src/*.java` plus `PreviewMotionPolish.java`, then ran only `PreviewMotionPolish tide` with assertions, headless mode and a 256 MB heap. Exit 0; all 60 Tide images were refreshed. These current captures replace the provisional first-candidate images described historically above.

Viewed all twelve current contact sheets: hit/miss pairs for [east hit](tide-east-hit-contact.png) / [east miss](tide-east-miss-contact.png), [west hit](tide-west-hit-contact.png) / [west miss](tide-west-miss-contact.png), [north hit](tide-north-hit-contact.png) / [north miss](tide-north-miss-contact.png), [south hit](tide-south-hit-contact.png) / [south miss](tide-south-miss-contact.png), [north-east hit](tide-north-east-hit-contact.png) / [north-east miss](tide-north-east-miss-contact.png), and [south-west hit](tide-south-west-hit-contact.png) / [south-west miss](tide-south-west-miss-contact.png).

- The initial release sample (0.04 seconds after actual release, within its first animation pose) now leaves the Blob silhouette and face readable in every facing. The burst sits behind the body instead of replacing it with an opaque bright disc. That readability issue is resolved in these samples.
- The clipped ocean-strip rectangle is gone. The isolated bright moving projectile has a trailing wake and consistent cardinal/diagonal aim. It reads more like a magical water bolt/orb than a rolling ground-surf wall; that is a remaining style distinction, not a collision or execution defect.
- Enemy impact is now a brief crown-like splash followed by scattered droplets; the persistent dark portal appearance is gone. Expiry remains a quieter, low-contrast foam finish rather than a false hit. The scattered droplets have a somewhat regular grid-like arrangement in the sampled late frames; this was reported as a remaining art-polish note, not hidden by a pass claim.
- All six real enemy-hit fixtures still received exactly two damage, and all twelve projectiles terminated. No runtime exception or missing input effect occurred. The downward miss's distant foam can still overlap the bottom HUD region, an existing layout/readability limitation.

No game source, assets or boss captures were changed during this final Tide review. No GUI/audio output or full build was run. Sound quality and live combat readability still require a human playtest; the main agent owns integrated release verification.

## Final Guardian death review and scope closeout

After `warden-final-death` source poses 1–8 were packed into the death row, recompiled current source plus the existing preview tool and reran only `PreviewMotionPolish boss` with assertions, headless mode and a 256 MB heap. Exit 0; all 63 Guardian files refreshed. Viewed all seven current contacts and the full death start/end screenshots, including the windup-to-strike and strike-to-recovery pose endpoints.

The final [death contact](boss-death-contact.png) now moves from an upright, recognizable Warden through a lateral turn and tip into a full [side-lying armored body](boss-death-7.png), with small fragments beneath it. The earlier flat-bottom buried-torso appearance is gone. All eight sampled death poses remain visible; no blink or missing body was observed. Horns, limbs and the final footprint remain inside the rendered sprite in these captures. The fixed source-to-ground alignment does not create a visible new cutoff. The last windup/first strike retain the raised-arm setup; the low end of strike transitions into the matching recovery rise without an obvious pose mismatch in the static contacts.

Current directory contains 197 staged PNGs across the four groups: 36 run, 38 side-engulf, 63 Guardian and 60 Tide. Every requested contact sheet has been visually inspected against its final selected pack. Run and engulf use normal movement/attack/E inputs after labeled initial fixtures; Tide uses actual RMB projectiles and impacts; Guardian pose sampling remains explicitly reflected state/time fixtures. Earlier rejection notes above are retained as review history, while current same-named images contain the reviewed replacements.

Remaining limits: no GUI/audio output, on-screen frame-pacing measurement, full build, or unassisted human playthrough was performed by this worker. Static pose contacts cannot certify temporal smoothness, exact foot contact through movement, or combat readability under many overlapping enemies. The Tide bolt-like silhouette, regular late droplet arrangement and occasional bottom-HUD overlap remain minor art/readability notes. Final automated suite, packaging and release claims belong to the main agent. This closes the bounded preview request without claiming the whole Ruined Outpost is finished.
