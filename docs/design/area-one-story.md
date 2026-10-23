# Area One — The Last Watch

Rainoray wakes in the Ruined Outpost as a slime. A medic is keeping a lamp lit for the people who never returned. At the eastern gate, their protector is still obeying an evacuation order it can no longer understand.

## Story

The last thing Rainoray remembers is counting blankets.

There were wounded coming in, carts going out, and always another shortage to settle before he could sleep. He was the quartermaster. If the numbers did not add up, he stayed until they did. His heart stopped before the work did.

Now rain passes through a blue body that has neither hands nor pockets. The outpost is familiar in pieces: the ditch beside the supply road, the expedition marks, a lamp burning where the captain's tent used to stand. He goes toward the lamp because someone must have lit it.

Mara, the field medic, almost mistakes him for another scavenger. His name stops her. She remembers the signature on the final delivery. His crates arrived; he did not. She has no explanation for what he has become. She can tell him what happened here.

The expedition sent its wounded east when the demon rift opened. The Outpost Warden was ordered to hold the road until the last stretcher passed. Then the rift struck the gatehouse. The Warden began treating Ichor as an enemy mark. The road it had been built to protect became a place no survivor could cross.

Mara tried to follow the evacuation into the forest. Roots had closed the trail. She returned to the camp, where the wounded would know to look for her, and kept the lamp burning.

Rainoray can hurry straight to the gate. If he searches the outpost, he finds the people still present in small decisions: one bed left made for a returning patient; weapon crates abandoned so blankets could fit on the carts; a dispatch with names marked missing instead of dead. His own name has been crossed out and written back in. Mara admits that she thought she saw him fall. She could not be certain enough to leave the mark there.

At the gatehouse, Rainoray tries to reason with the Warden. It is still waiting for the last stretcher. When he gives his name, it finds his death in the evacuation records and refuses to recognize the person standing in front of it. There is no captain left to correct it. Rainoray has to break the thing that is keeping everyone safe on paper.

Beyond the broken chain, stretcher tracks disappear into the trees. Only Mara's footprints return. Rainoray cannot know whether the wounded are still alive. He chooses to follow them anyway.

If he returns to camp first, Mara offers him a dry scarf. He is not sure he can wear it in this body. She says she will keep it dry. For once, something can wait for him without being another unfinished task.

## In-game delivery

Dialogue shows one spoken line at a time, with the current speaker's portrait. Lines appear immediately; there is no forced typing delay. The compact 160-pixel-high panel uses a bold display-font speaker name, a small colored rule and larger 3× pixel body text, wrapping to at most two rows. The ornate frame, small next arrow and clickable Skip label remain. Enter, E or left click advances, X skips, and Escape opens the pause menu; shortcuts stay in Controls instead of a large dialogue footer. Combat, movement, projectiles, cooldowns and Ichor drain stop while the panel is open.

The Warden's first encounter starts only from an unobstructed approach within 520 world pixels.
A 1.1-second camera pan frames the Warden and gate; dialogue starts after a short settling beat.
The Warden breathes and ambient flames continue moving while combat stays frozen. Finishing or
skipping the conversation returns the camera over 0.6 seconds before combat resumes. Pause
freezes the entire sequence. After victory, the East Gate opens over 1.4 seconds when approached;
cleared gates loaded from a save stay open. [Visual review](../art-review/outpost-polish/README.md).

| Moment | Trigger | Purpose |
| --- | --- | --- |
| The Last Watch | Start a new journey | Establish Rainoray's memory, changed body and the lamp |
| The Lamp at Camp | E at Captain Camp | Meet Mara and learn why the east road closed |
| The Lamp at Camp, late arrival | Meet Mara for the first time after defeating the Warden | Acknowledge the open gate instead of sending the player to an already defeated boss |
| Mara | Talk again before the Warden falls | Short route reminder |
| The Missing Names | E at Signal Tower | Discover the evacuation order and Rainoray's name |
| One Empty Bed | E at Field Infirmary | Show Mara's hope through something she left behind |
| The Last Delivery | E at Supply Reserve | Give Rainoray's former work a human consequence |
| Still Missing | Return to Mara after examining the tower | Resolve the crossed-out name |
| The Last Order | First approach to the Warden | Make the failed order explicit before combat |
| The Road Opens | E at the defeated Warden's exit | Find the tracks and choose to follow them; E again enters the forest |
| A Place to Return To | Talk to Mara after victory | Leave a small personal reason to come back |

Optional discoveries never gate the boss or the forest. Each discovery awards its existing shard once; re-reading does not award another. The opening plays on a new journey. The Warden introduction and gate departure play once, including when skipped. Camp conversations and written discoveries can be revisited.

Exact shipped lines live in [OutpostDialogue.java](../../src/OutpostDialogue.java). They are authored for the current GDD's former **quartermaster**, rather than the archived modern-office-worker draft. Mara is the existing camp medic, not a newly introduced faction or quest system. The story ends this chapter at the Corrupted Forest; later regions are not required to resolve the Outpost conflict.

## Art and persistence

- Mara's [native 64 × 64 portrait](../../assets/characters/npcs/mara_portrait.png) is imported from the left half of the preserved source sheet; her [80 × 80 standing sprite](../../assets/characters/npcs/mara_idle.png) matches the player's world scale. [Art and generation records](../art-review/area-one-story/polish-art.md).
- Rainoray's human portrait comes from the current masked sprite. His [slime portrait](../../assets/characters/slime/rainoray_portrait.png) matches the current star-marked body. The Warden portrait uses its existing art.
- Story completion uses the existing version-one save's `unlocks` set. Existing health upgrades, shards, checkpoints and cleared bosses are preserved. Continue does not force the new opening onto an older save.
- Test/admin sessions retain their existing protection against overwriting normal saves.
- Portraits are static. No voice acting or animated facial expressions are included.
