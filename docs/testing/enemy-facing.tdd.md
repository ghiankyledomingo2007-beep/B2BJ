# Enemy direction magnitudes survive animation selection

RED: `EnemyFacingTest` fails at SCOUT pursuit `(1, .15)`: the sprite incorrectly
selects South, despite motion being almost entirely East.

Root cause: four Wisp call sites reduced both axes to `signum` before passing
them to `WispAnimation`. Every non-cardinal heading became `(±1, ±1)`, so the
four-direction atlas selector always chose its vertical tie-break.

Keep the real direction magnitudes through pursuit, target tracking, committed
telegraph and lunge. The existing shared dominant-axis selector does the rest;
no new direction/state system. Stationary/hurt/recovery preserve the last facing.

`EnemyFacingTest` covers five combat roles across eight shallow/steep headings
(40 scenarios), including re-aiming behind the enemy after its direction locks.
Existing exact-pixel guard/knight, Remnant/Spitter and combat tests remain active.
