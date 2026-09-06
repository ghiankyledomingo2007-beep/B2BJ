# Slime water-combat pass — 2026-09-06

Owner-requested design change: remove the unnatural close-up jelly punch and replace
it with cyan curved water projectiles, plus a balanced secondary skill. This supersedes
the prior punch pass and deliberately changes the original GCD's Blob attack.
Blade melee, character scale, Ichor economy and the Ruined Outpost route stay intact.

## Runtime tuning

| Move | Damage | Windup | Recovery / cooldown | Initial speed | Maximum travel | Hit radius |
| --- | --- | --- | --- | --- | --- | --- |
| LMB Water Slash | 1 | 0.08 s | 0.48 s shared casting recovery | 620 px/s | 340 px | 16 px |
| RMB Tide Wave | 2 | 0.18 s | 0.48 s shared recovery + 4.5 s skill cooldown | 440 px/s | 280 px | 28 px |

Shots lose speed exponentially, expire within 0.8 seconds, fade near maximum range,
and hit one target. Basic water does not interrupt enemy attacks; Tide Wave staggers,
giving its cooldown a defensive purpose without a basic-attack stun lock.
Swept collision steps are at most four pixels. Walls, earth
banks, world edges and locked breaches absorb water. The Warden stays invulnerable
outside recovery. Enemy momentum uses existing collision movement and drag; this is
gameplay physics, not a fluid simulator. Dash/transform cancel pending casts. Pause
freezes simulation; room changes/death clear projectiles. Heavy cooldown cannot be
reset by crossing a room. Knockback impulses: 140 / 420 px/s, drag 10/s.

## Paid-job ledger

Started with 12 of the current 40-generation token allowance remaining. Each job below
costs one generation. No Pro edit, purchase, credential change or limit increase.

| Purpose | Anchor job | Animation job |
| --- | --- | --- |
| Water Slash | b7ad32bc-51fe-4e27-b7e8-68abe96b318f (`c.png`) | 00b61dc2-e1a8-4316-a0c9-5a000341ba50 |
| Tide Wave | 8f64e662-57df-459f-a539-e5b2bf2942a7 (`b.png`) | f656e989-b316-409d-a8a0-51f61493d90b |
| Water impact | 73da140a-14b3-4f25-9899-2ad56b862c2f (`a.png`) | 5db3d664-c342-49a1-91e8-9845c800afe7 |
| Cast droplets | a3a0814e-d56c-407d-b383-e6961fa44e4b | f78642d6-a4d1-450f-bdd2-8e22c1ec835e |
| Trailing wake | 7b7adf27-3fce-43c4-9189-89b8014bacda | a5912e61-7b7d-4eba-be61-27fea5a87594 |
| Heavy impact | 83586617-0cb7-4285-b3ca-451dddb76fad | 092ee587-37be-4361-b6e8-0aeff3da0725 |

Prompts and read-only status arguments are retained here. Provider animation output
includes the unchanged input at frame 0 plus eight generated frames. Preserve every
download; runtime sheets use reviewed native frames, not automatic acceptance.

## Review / verification

First three effects: accepted. Native 32px basic slash / splash and 48px heavy wave;
all first-pass frames have transparent margins and distinct pixels. Crescent opening
stays opposite travel; renderer corrects the anchor's diagonal pose before rotation.
Rotation happens on a native pixel buffer, then renders at integer 2x. No sword/hilt,
no connected fist, no Slime rescaling. Water participates in world depth sorting.

Last three effects: accepted after contact-sheet review. Cast ring contracts, sparse
wake breaks into droplets, and the heavy crest collapses into a puddle. All six
animations contain nine distinct raw frames; runtime uses frames 0–7 (eight poses),
omitting the near-loop endpoint. All 60 raw PNG results are retained (six anchors
and 54 animation frames). Six runtime sheets contain 48 reviewed poses.

Final provider read: **0 remaining / 40 used / 40 total**, purchased balance **$0.00**.
This pass used all 12 remaining generations; generation paused without retries,
credential changes or purchases. Final build passes all 19 Java test programs plus
the packaged-asset check from outside the project. Added rear-projectile/fence
occlusion regression passes; final left/right-skill scene previews are saved here.

The route fixture now approaches at ranged spacing and waits for casting recovery,
rather than repeatedly teleporting into contact and clicking before cooldown ends.
It is still not an unassisted playthrough or proof of final balance. Boss pacing,
legacy Blade side attacks, environmental art and music remain outside this pass.
