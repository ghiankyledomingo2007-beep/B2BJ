# Area One — The Lamp Still Burns

Current story revision: 2026-10-26. This replaces the former outpost-quartermaster origin. The accepted [opening draft](reincarnation-opening-draft.md) records the direction; this document describes the implemented game.

## Rainoray's second life

Rainoray dies in the modern world while trying to open an overturned relief van. His last question is whether the other person escaped. Raphael cannot give him that answer. She can help him survive in another world, in a body that initially has no hands.

The new journey opens with a rainy road memory, darkness and Raphael's voice. Her gold seal forms around a suspended slime. Analysis, the slime body, absorption and regeneration appear one by one. A faint masked human form hints at what Ichor will allow later. The scene returns to the actual outpost, where a camp lamp and someone calling for help give Rainoray a reason to move.

The fifteen voiced beats last 56 seconds at their nominal timings, plus at most one frame per transition. Enter/E/click advances a beat; X or the visible Skip target ends the opening; Escape pauses it. Skipping and watching to completion write identical progress. New Journey plays it; Continue does not replay it or reset old progress. Reduced Effects freezes rain drift, decorative motes, seal rotation and sprite cycling; necessary scene fades and reconstruction transitions remain.

This is an alternate-continuity Tensura fan story. Rainoray remains the protagonist. Raphael appearing from the start is a deliberate adaptation, not a claim about the anime's chronology. The dialogue is newly authored for this game.

## The refuge and the broken order

Shuna does not recognize Rainoray. She stops a small monster approaching the wounded; he explains that he heard someone calling. Raphael notices he is also looking at the food. Their first exchange gives him a little dry humor without pretending he understands his situation.

Shuna maintains the camp's protective ward. The wounded were sent east before the rift struck, and the Warden has since sealed their road. She cannot leave the refuge unprotected. Rainoray can search the nearby yards and follow the torn banners toward the gate. The ward is a story responsibility, not a new combat system.

The discoveries offer different pieces of the evacuation:

| Place | What Rainoray learns |
| --- | --- |
| Signal Tower | A damaged order forbids Ichor-bearing life, including his new body. Missing names remain on the dispatch. |
| Field Infirmary | One bed and dry dressing were kept ready for whoever returns. |
| Supply Reserve | Heavy weapons were left behind to make room for wounded people. Rainoray remembers packing his modern van, not owning these crates. |
| Evacuation cache | The ledger contains Kaijin, Eren and Rigurd, never Rainoray's name or handwriting. |

Kaijin is an injured craftsman who recognizes the sound of the damaged gate relay. Eren was scouting the road when moving roots trapped her. Rigurd stayed with the final evacuees before becoming separated. Each has a new world sprite and close-up portrait, and rescued people visibly return to camp. Shuna's reunion branches follow the actual rescue count.

At the gate the Warden rejects a contaminated lifeform. It believes sealing the road protects the refuge. Raphael explains that it cannot distinguish a rescue from an attack; she needs to observe its fighting pattern. The conflict follows from a failed order rather than a forgotten personal relationship.

The introduction retains its camera reveal, frozen combat and return pan. The first unobstructed approach within 520 world pixels triggers it once. The gate opens after victory. Optional discoveries and rescues never gate the boss or the forest, and repeat discoveries never award duplicate shards. Meeting Shuna after defeating the Warden uses the late-arrival branch.

## Into the forest

Beyond the gate, stretcher tracks continue into the trees. If Rainoray returns to camp first, Shuna offers a dry scarf; she keeps it for him when he doubts it would stay on his slime body.

Souei has crossed the evacuees and marked their route with white cloth, but Briarheart's roots keep closing it. He guards their crossing while Rainoray reaches the tree. Defeating Briarheart opens the route. Raphael reports that the final stretcher has crossed; Souei confirms everyone they found is at camp. Rainoray finally receives the answer he could not get at the van. The scene does not claim that skipped optional rescues occurred.

## Raphael during play

Existing companion and combat behavior remains. First encounters identify nearby visible enemy types once. After witnessing two releases of a boss attack, Raphael can recognize its pattern and give short directions on later telegraphs. Knowledge persists across retries and loading. Boss warnings share a 15-second cooldown; accepted-hit reactions have a 25-second cooldown and expire after 2.5 seconds if the voice channel stays busy.

Optional post-fight, low-health, exploration and camp exchanges share the existing channel and saved no-repeat flags. Post-fight speech waits for five quiet seconds; exploration requires 25 quiet seconds and 700 world pixels of travel. Optional exchanges wait 75–105 seconds between starts. NPC conversations and combat interrupt them. Standing still or teleporting does not produce exploration banter. Exhausted pools stay quiet.

Rainoray uses the compact lower subtitle; Raphael uses the voice-reactive gold seal at the top, below the boss bar during fights. Full dialogue pauses gameplay; roaming conversations and combat warnings do not. All audio is bundled and plays offline. Music, voice volume, mute, ducking and Reduced Effects settings remain available.

Passive recovery starts after five seconds without damage and restores 2 HP/second, including in combat. Damage restarts the delay; pause, dialogue and death stop recovery. The HP countdown, green healing label, particles and hit feedback remain. This revision changes presentation and story, not combat balance.

## Voices, art and save compatibility

- 185 Chapter One clips and 104 Chapter Two clips cover every authored spoken line and branch. Seven existing combat efforts remain separate. This revision generated 88 new or replacement takes.
- Rainoray and Raphael retain their approved voice profiles. Shuna, Kaijin, Eren, Rigurd and Souei retain the existing premade NPC cast identities; these are not new anime-actor clones. Rainoray has no pitch/formant processing; Raphael retains her chime/echo treatment.
- The final dialogue master target is mono PCM16 at 22,050 Hz, -18 LUFS / -4 dBTP. [Runtime manifest](../art-review/voice-auditions/runtime-voices.json) stores scripts, profiles, measurements and final hashes. [Chapter One script](chapter-one-voice-lines.tsv) and [Chapter Two script](chapter-two-voice-lines.tsv) use stable internal keys.
- [Art source guide](../../assets/source/characters/npcs/README.md) records the new cast atlas and imports. Existing native sprite/portrait dimensions and restrained palettes are retained. Portraits remain static.
- Public labels changed through `Speaker.label()`. Legacy enum names, clip keys, sprite filenames and save unlock keys deliberately remain stable. Existing checkpoints, upgrades, boss knowledge, shards and rescue flags are preserved.

Verification and remaining limitations: [opening test report](../testing/reincarnation-opening-2026-10-26.md).
