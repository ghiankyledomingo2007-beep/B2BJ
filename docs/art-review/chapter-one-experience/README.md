# Chapter One pacing and spoken-line support — 2026-10-25

Changes following the [experience audit](../../testing/chapter-one-experience-audit-2026-10-25.md):

- Mara points toward the storehouse survivor; the map marks known people needing help. Nearby survivors call out visually. Carrying the key marks its cache.
- Three quiet exploration lines: Mara calls after Rainoray when he leaves camp, Rainoray hears someone south of the road, and he reflects after rescuing someone. Each fires once per campaign, leaves movement active, freezes on pause, and yields to combat/full dialogue.
- Warden, signal dispatch, survivor directions and gate dialogue account for whether Rainoray met Mara. Warden dialogue also acknowledges rescues. Leaving during Mara's final reward scene cannot duplicate its reward.
- Carried and dropped objects survive save/Continue and death. Optional fields retain compatibility with old saves and validate item IDs/positions. Completed objects cannot also be carried. Changing regions still resets uncompleted loose objects to their authored positions.
- A fourth, already lit brazier sits on the gate approach at (5280,2320). Chapter One rests and checkpoint retries refill HP/Ichor; new journeys still start empty. Ordinary enemies continue to respawn, but the gate checkpoint avoids repeating the main route. Tested stationary for 30 seconds after respawn without patrol aggro/damage.
- Supply tins can be secured at a lit brazier, with the existing one-time shard plus 25 HP, instead of always carrying them back to camp. The original camp supply display counts secured tins.
- Later patrol formations are spread differently; count, kinds and authored reward indices are retained. The final spitter is farther from the retry point.
- Warden armor blocks produce a guard sound/impact, with explicit ARMOR CLOSED / CORE EXPOSED labels. Damage windows, attack tells, HP and phase effects remain intact.
- Recorded dialogue has its own mixer channel and volume slider. Advancing replaces the line, skipping ends it, pause freezes it, missing files remain silent, and subtitles remain usable. Recordings and voice selection are still pending. [Recording instructions](../../../assets/audio/dialogue/README.md), [exported script](../../design/chapter-one-voice-lines.tsv).

Images are actual headless game renders, not concept art: `mara-farewell.png`, `gate-rest.png`, `boss-armor.png`, `rescue-map.png`. Captures use isolated game state, never the user's save. No audio audition or complete human balance playthrough is claimed.

Verification: all 101 test classes passed in `build.sh`, including the new ChapterOneExperienceTest and DialogueAudioTest. The packaged asset smoke check passed and the verified JAR was published to `build/B2BJ.jar`.

The user also proposed an inner companion inspired by the dynamic between Rimuru and Raphael. A permanent companion remains a proposed story addition. The user states permission for English performance cloning and approved their Inovision ElevenLabs account. The English Raphael clone and [audition](../voice-auditions/README.md) are ready; Rimuru is pending an accessible English reference. No audition has been installed into runtime dialogue.
