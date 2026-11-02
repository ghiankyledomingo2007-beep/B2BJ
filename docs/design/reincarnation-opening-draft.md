# Reincarnation opening — story draft, 2026-10-26

> Historical draft: the owner replaced this rescue-van origin on 2026-10-27 with an overworked Japanese office worker collapsing at his desk. See [the current story](area-one-story.md).

Status: accepted and implemented on 2026-10-26. This preserves the original proposal. The [current story](area-one-story.md) and [verification report](../testing/reincarnation-opening-2026-10-26.md) describe the shipped scope and exact timing. The implementation uses one illustrated road memory with rain/push-in, the existing sprite animation and seal, and the actual world reveal; it is not a separately animated film.

## Direction

Rainoray dies in the modern world while helping someone escape a wrecked relief van. His final concern is whether the other person got out. He awakens in another world with a slime body and Raphael's voice. He cannot know whether he succeeded in his first life. Finding the people missing from the Ruined Outpost gives him a concrete reason to act in this one.

Keep Rainoray as the player's name unless the user chooses otherwise. Replace the prior history in which he was this outpost's quartermaster: these people have never met him. The missing-person ledger belongs to the evacuation, not to Rainoray, and his name is not in it. The old theme of unfinished work survives through his actions rather than a contradictory shared past.

This is an alternate-continuity Tensura fan story. Raphael at the beginning is an intentional adaptation: in the series, Raphael is the evolved form of Great Sage. Do not describe our sequence as the anime's exact chronology. Reference: [official Raphael profile](https://www.ten-sura.com/character/raphael). All dialogue below is newly written for this game.

## Opening: “One More Chance”

Target: roughly 50–60 seconds, adjusted to the final voice takes. Three small pixel-art memory shots, the existing gold seal, the current slime sprite and the existing outpost are enough. Present one readable system result at a time; each result visibly changes the forming body. No percentage counter pretending to measure real asset loading.

| Beat | Picture and sound | Spoken lines / system result |
| --- | --- | --- |
| 0–8s: the last attempt | Rain on a modern road. An overturned relief van. A hand reaching through its broken window. Rainoray pulls at the door; metal shifts, then cut to black before the impact. Rain and strained breathing; no triumphant music. | **Rainoray:** “Stay with me. I've almost got it open.” |
| 8–17s: something remains | Black. The rain becomes distant. One heartbeat fades. A small gold point appears, followed by the first broken segment of Raphael's seal. | **Rainoray:** “Did they get out?” **Raphael:** “I cannot confirm. Your previous body has been lost.” **Rainoray:** “Then... who am I talking to?” **Raphael:** “Raphael. Stay with my voice.” |
| 17–32s: reconstruction | Gold arcs connect around a suspended blue droplet. Each completed ring produces one soft chime. The droplet splits and rejoins, then becomes the player's slime silhouette. A quiet sustained musical note enters. | **Raphael:** “Reconstructing a body that can survive here.” Show sequentially: `ANALYSIS — ACTIVE`, `SLIME BODY — STABLE`, `ABSORPTION — AVAILABLE`, `REGENERATION — ACTIVE`. **Rainoray:** “A slime? That was your best option?” **Raphael:** “It was the one that survived.” |
| 32–44s: the promise | A brief outline of the existing masked human form appears behind the slime, then breaks into particles and returns to the blue body. A small empty Ichor ring remains for one beat. | Show `HUMAN FORM — INSUFFICIENT ICHOR`. **Rainoray:** “I can still change back?” **Raphael:** “For a while. Once we have enough Ichor.” |
| 44–58s: a new problem | The gold point becomes a reflection in an outpost puddle. Rain returns at normal volume. The slime stirs, tries to reach forward, then discovers its whole body moves instead. A camp lamp glows beyond the ditch. The seal shrinks into its normal HUD position. | **Rainoray:** “Where are my hands?” **Raphael:** “Try moving toward the light.” A faint call for help. **Rainoray:** “Someone's still out there.” **Raphael:** “One step at a time.” **Rainoray:** “You'll have to talk me through the steps.” |

Give movement control immediately after the last line. The objective becomes reaching the camp lamp. Teach absorption beside an actual eligible fallen enemy; explain regeneration after the first real hit; explain Q when the Ichor meter is ready. The film establishes possibilities, and play teaches the controls.

Raphael should sound calm and precise. Rainoray gets one dry joke while genuinely disoriented. Leave short gaps for the images and sounds; do not fill every second with narration. Do not reuse anime dialogue or insert an unearned victory speech.

## Cast adaptation

These are proposed roles in our alternate story, not claims about canonical events. Names alone are insufficient: dialogue, portraits, world sprites and spoken references must change together. Official naming reference: [Tensura character index](https://www.ten-sura.com/character).

| Existing character | Proposed character | Role in this story |
| --- | --- | --- |
| Sage | **Raphael** | The voice that helps Rainoray inhabit his new body. Can identify creatures and learn observed boss attacks, but cannot know unseen survivors' conditions or solve the whole mystery immediately. |
| Mara | **Shuna** | Maintains the refuge and a protective ward around its wounded. Leaving would expose them, giving her a concrete reason to ask Rainoray to search outside. Warm, composed, willing to challenge his recklessness. |
| Tobin | **Kaijin** | An injured craftsman stranded beside the collapsed storehouse. Understands the Warden's damaged gate mechanism; supplies useful evidence after rescue rather than merely thanking the player. |
| Ilse | **Eren** | An exhausted adventurer caught while scouting the brambles. Has noticed the roots moving against the wind and brings that clue back to camp. Keep her observant and resourceful. |
| Venn | **Rigurd** | Stayed with the last evacuees and became separated. Knows who is still missing. His return changes the ledger and gives the camp a visible reunion. |
| Sedge | **Souei** | Tracks the missing group through the forest and keeps the injured together near the crossing. Gives a concise route report. The root network threatens the people he is guarding, so the player tackles its heart while he protects the return route. |

The ward, injuries and evacuation duties are story constraints, not new combat mechanics. Avoid making capable characters helpless simply to create errands. They should do useful work before and after the player's intervention. Existing NPC art cannot be presented as a finished depiction of these characters.

## Chapter One: The Lamp Still Burns

Shuna does not recognize Rainoray. She first sees a small monster approaching injured people; he sees someone preparing to drive him away. He speaks before she attacks.

> **Shuna:** “Stop there. There are wounded behind me.”
>
> **Rainoray:** “I heard someone calling. I wasn't coming for the food.”
>
> **Raphael:** “You are looking at the food.”
>
> **Rainoray:** “I'm having a strange morning.”
>
> **Shuna:** “Then start with your name.”
>
> **Rainoray:** “Rainoray. That's the part I'm sure about.”

She explains the evacuation in a few concrete facts: three people have not reached camp; the others were sent east; the Warden closed the road after the rift struck. She remains because the refuge's ward needs tending. Rainoray offers to look before she can promise payment.

The optional discoveries answer different questions:

- **Signal Tower — why the road closed.** A damaged order lists Ichor-bearing creatures as contaminated. Raphael identifies the same signature in Rainoray's new body. The player learns why the Warden will reject him.
- **Infirmary — why Shuna stays.** Beds and supplies are reserved for the missing. A patient tells the player that Shuna has kept their places ready. Returning survivors visibly occupy those places.
- **Supply Reserve — what the evacuation cost.** Weapon crates were left behind so medical supplies could fit. Kaijin's notes show a damaged control relay; they explain the Warden's failure without introducing a mandatory key hunt.

At the gate, give the Warden an intelligible, failed purpose rather than generic evil. It is protecting the refuge from a threat category that now includes the protagonist.

> **Warden:** “CONTAMINATED LIFEFORM. PASSAGE DENIED.”
>
> **Rainoray:** “There are people on the other side.”
>
> **Warden:** “THE ROAD IS SEALED FOR THEIR PROTECTION.”
>
> **Raphael:** “The order is damaged. It cannot distinguish a rescue from an attack.”
>
> **Rainoray:** “Can you shut it down?”
>
> **Raphael:** “Not from here. Keep moving. I need to see how it fights.”

After victory, Raphael confirms only that the gate can open. She cannot confirm the missing people's fate. Rainoray finds stretcher tracks continuing into the forest and chooses to follow them. If he returns to camp, Shuna hands him a scarf with no expectation that he can wear it yet. He asks her to keep it until he returns.

## Chapter Two payoff

Souei's trail leads to living evacuees trapped behind Briarheart's roots. His short report makes it clear the survivors need a safe route, not a lone hero showing off. Existing boss pattern-learning and root-lane mechanics support this directly.

At the ending, give Rainoray the answer he could not get in his previous life: this time he sees the people he helped reach safety.

> **Raphael:** “The last stretcher has crossed.”
>
> **Rainoray:** “All of them?”
>
> **Souei:** “Everyone we found. They're at the camp.”
>
> **Rainoray:** “Good. I wanted to hear someone say it.”

Do not claim that every person in the world was rescued, or that optional outpost rescues happened if the player skipped them. Camp reunion dialogue must reflect the actual saved rescue count.

## Implementation consequences

- Replace `WAKE` with the new opening sequence. New Journey plays it once; Continue preserves existing progress. Skip must stop its audio and reach exactly the same playable state as natural completion. Escape pauses it. Reduced Effects removes flashes/shake, keeps readable system messages and body changes.
- Use the existing gold overlay, native pixel rendering, slime animation, human-form outline, voice playback and transition hooks. Fit the opening to the viewport using the same scaling convention as the title screen. A separate authored film or new engine is unnecessary.
- Rewrite quartermaster recognition in camp, dispatch, supply cache, survivors, Warden, forest orchard and optional banter. Rainoray can remember packing medical supplies in his modern life; he cannot recognize these crates, his handwriting or these people from that life.
- Keep enum IDs, save unlocks and voice IDs stable where practical; update public labels through the existing `Speaker.label()` mechanism. A story restart should remain an explicit New Journey, never an automatic save reset.
- Audit every spoken name, including the existing “Sage. That name suits you.” exchange. Replace affected WAVs and their transcript/hash manifest in the same release as the text. Existing recordings naming Mara/Tobin/Ilse/Venn/Sedge must not play under the new identities.
- Update quest objectives, map descriptions, scene headings, portraits, world prompts, both script exports and their tests. Treat any temporary old art as an unfinished production asset.
- Verify new game, skip, pause/resume, reduced effects, 720p/1080p, existing-save Continue, alternate rescue branches and voice/subtitle agreement before calling the rewrite installed.

Implementation follows the cast and narrative direction above. Fine cinematic actions in this proposal (three memory shots, a reaching-hand animation and particles dissolving the human body) remain concept detail, not implemented shots.
