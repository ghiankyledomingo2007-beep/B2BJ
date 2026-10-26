# Beelzebub — 2026-10-26

Press **R in human/Blade form** to draw nearby fallen enemies into a dark vortex. Available in both campaign regions; no unlock or save migration required.

The [official Tensura glossary](https://www.ten-sura.com/keyword) identifies Beelzebuth as the evolved Gluttony skill. We use the user's requested English spelling **Beelzebub**. This corpse-harvest mechanic is a B2BJ adaptation, not a reproduction of every canonical ability.

| Rule | Value |
| --- | --- |
| Reach | 420 world pixels around Rainoray; walls block the pull |
| Channel | 1.15 seconds, stationary |
| Cooldown | 10 seconds from cast start |
| Upfront Ichor cost | None; normal Blade drain continues |
| Each corpse | 5 Ichor; ordinary enemies also restore 10 HP |
| Boss summons | Ichor only, no healing |

Only corpses present and reachable at cast start join the pull. Live enemies, NPCs and the boss are not consumed. Rewards arrive together when the cast completes and respect normal resource caps. Human absorption grants no temporary slime traits. Slime's E absorption remains available as before.

Moving, dodging, attacking, using another skill, taking damage, dying, reverting, entering dialogue or leaving the region cancels the cast. Bodies remain where they have been pulled, with their original 20-second lifetime. Cancellation retains the cooldown; an empty cast consumes no cooldown. Pause freezes progress and cooldown.

Presentation reuses the human cast pose, existing charge/cast/absorption sounds and the existing consume icon tinted violet. Native-pixel currents draw inward; corpses shrink and dissolve. The R slot shows cooldown and the active prompt counts remains. Reduced Effects removes moving motes and rotating spirals. No new dependencies, generated audio or runtime art files.

Implementation: `RuinedOutpostGame` owns targets, interruption and rewards; `B2BJ` binds R, animation, audio and HUD; `BeelzebubFx` draws the vortex; `CampaignMenu` lists the control. `BeelzebubTest` covers mechanics and presentation wiring. Range, channel and cooldown remain initial playtest values.
