"""Blade-form sword work for the Rainoray puppet: the four-click combo, the charge, its hold, the cleave and
the two variations (dash slash, full-charge spin).

He fights one-handed, and a cut is thrown by the whole body, not the arm (see channels.py for the language):
  * wind-up   he SINKS and LOADS: hips back and down onto the rear leg, torso coiled away from the cut, the
              stepping foot already off the ground;
  * contact   he DRIVES: hips a long way forward and still low, the step landing, rear leg straight with the
              heel up, torso through and bent into the cut, sword arm at full reach with the blade continuing
              its line (tip a touch behind the hand), off arm thrown back as the counterweight;
  * follow    the arm keeps its length and sweeps on, the tip now leading, torso over-rotating; held longest;
  * recover   through the `ready` waypoint, so the blade comes home round the front, never through the floor.
These sheets have wide cells (build_sheets.WIDE), so none of that travel has to be cancelled to fit a cell;
he ends each move back on his mark because the GAME has carried his mark forward (strikeStep).

Sheet timing comes from B2BJ.bladeAttackFrame(): frames 0-3 are the wind-up, frame 4 is contact, 5-11 the
rest. The game's blue arc (B2BJ.drawBladeEffects) turns clockwise on screen, which is always from the
attacker's left to his right, and mirrored for move 2 and the dash slash, so the cuts travel that way.
"""
import math

from channels import ELBOW_L, ELBOW_R, arm, poser, posers, ready, toward
from reactions import dash_land

# action -> frame count. Must match src/B2BJ.java / src/BladeAnimation.java.
FRAMES = {"slash0": 12, "slash1": 12, "slash2": 12, "slash3": 12, "cleave": 12, "charge": 8, "hold": 8,
          "dashslash": 12, "spincleave": 12}


def draw_cut(g):
    """Move 0. Rising draw cut from the left hip to high on his right. 0.09 s wind-up, 0.30 s total.
    The wind-up is the charge's stance, so a press that turns out to be a click flows straight into it."""
    wind = {**g, "yaw": 42, "coil": 12, "lean": 12, "roll": 6, "hip": (0, -0.10, -0.12), "footR": (0, 0.08, 0.06),
            "footL": (0, -0.08, 0), "armR": arm(-0.67, 0.48, -0.56), "elbowR": ELBOW_R,
            "blade": (-0.4, -0.9, -0.15), "armL": arm(-0.09, 0.12, -0.99, 0.75), "elbowL": ELBOW_L,
            "neck": -6, "hair": -4, "hair_sway": 12}
    # the blade crosses low in front on its way to the hip; the short way round is through his legs
    cross = toward(g, wind, 0.45, blade=(-0.5, 0.7, -0.5))
    contact = {**wind, "yaw": -10, "coil": -4, "lean": 18, "roll": -8, "hip": (0, 0.24, -0.12),
               "footR": (0, 0.36, 0), "footL": (0, -0.12, 0), "toeL": 35, "armR": arm(0.12, 0.97, 0.05),
               "blade": (-0.28, 0.95, -0.05), "armL": arm(-0.45, -0.75, -0.45, 0.95), "neck": -12,
               "hair": 26, "hair_tip": 30, "hair_sway": -16}
    # he rises out of the lunge as the blade climbs: the cut lifts him
    follow = {**contact, "yaw": -50, "coil": -10, "lean": 8, "roll": -4, "hip": (0, 0.22, -0.03), "toeR": 10,
              "armR": arm(0.66, 0.22, 0.62), "blade": (0.6, -0.35, 0.62), "neck": -4, "hair": 10,
              "hair_tip": 30, "hair_sway": -26}
    settle = {**follow, "yaw": -54, "lean": 5, "hip": (0, 0.16, -0.04), "armR": arm(0.66, 0.05, 0.6, 0.95),
              "blade": (0.6, -0.45, 0.55), "hair": 4, "hair_tip": 12, "hair_sway": -10}
    home = toward(settle, ready(g), 0.5, blade=(0.55, 0.5, 0.55))
    # frame 0 keeps the guard's blade: from the front it already hangs at the bottom of the cell
    return [(0, toward(g, wind, 0.25, blade=g["blade"]), "lin"), (1, cross, "lin"), (3, wind, "out"),
            (4, contact, "lin"), (5, follow, "lin"), (7, settle, "out"), (8, home, "lin"), (9, ready(g), "lin"),
            (11, g, "inout")]


def thrust(g):
    """Move 1. Fencing lunge. 0.10 s wind-up, 0.34 s total; the game carries his mark 30 px forward."""
    wind = {**g, "yaw": -44, "coil": -8, "lean": -4, "roll": -4, "hip": (0, -0.14, -0.12), "footL": (0, 0.04, 0),
            "footR": (0, -0.06, 0.05), "armR": arm(0.45, -0.55, -0.45, 0.62), "elbowR": (0.4, -0.9, -0.2),
            "blade": (0.0, 1.0, 0.04), "armL": arm(-0.15, 0.9, 0.1, 0.85), "elbowL": ELBOW_L,
            "hair": -6, "hair_sway": -8}
    # arm, blade and the line from the back heel are one straight line; the off arm is thrown straight back
    contact = {**wind, "yaw": 42, "coil": 8, "lean": 22, "roll": 0, "neck": -16, "hip": (0, 0.30, -0.15),
               "footR": (0, 0.44, 0), "footL": (0, -0.10, 0), "toeL": 40, "armR": arm(0.0, 1.0, 0.03),
               "elbowR": ELBOW_R, "blade": (0.0, 1.0, 0.0), "armL": arm(-0.25, -0.9, -0.35, 0.95),
               "hair": 30, "hair_tip": 32, "hair_sway": 0}
    reach = {**contact, "lean": 25, "hip": (0, 0.33, -0.16), "hair": 20, "hair_tip": 34}
    settle = {**reach, "lean": 20, "hip": (0, 0.28, -0.13), "blade": (0.0, 1.0, -0.06), "hair": 6, "hair_tip": 12}
    return [(0, toward(g, wind, 0.25), "lin"), (3, wind, "out"), (4, contact, "lin"), (5, reach, "lin"),
            (7, settle, "out"), (9, ready(g), "inout"), (11, g, "inout")]


def diagonal(g):
    """Move 2. Descending forehand diagonal, high on his right to low on his left (the arc mirrors for
    this move in the game). 0.14 s wind-up, 0.40 s total."""
    # he rises onto his toes to load it, blade laid back over the right shoulder
    wind = {**g, "yaw": -48, "coil": -10, "lean": -8, "roll": -6, "hip": (0, -0.10, 0.03), "toeL": 20, "toeR": 25,
            "footL": (0, 0.08, 0.07), "footR": (0, -0.08, 0), "armR": arm(0.5, -0.3, 0.82, 0.92), "elbowR": ELBOW_R,
            "blade": (0.3, -0.85, 0.42), "armL": arm(-0.25, 0.85, 0.2, 0.8), "elbowL": ELBOW_L,
            "neck": 4, "hair": -8, "hair_sway": -12}
    # the arm climbs through the front on its way up: straight out to the side is a weak, flat silhouette
    climb = toward(g, wind, 0.5, armR=arm(0.5, 0.45, 0.35, 0.8), blade=(0.5, 0.2, 0.84))
    # and he comes DOWN with it: the whole body drops into the cut
    contact = {**wind, "yaw": 8, "coil": 4, "lean": 20, "roll": 8, "hip": (0, 0.22, -0.14), "toeL": 0,
               "footL": (0, 0.36, 0), "footR": (0, -0.12, 0), "toeR": 35, "armR": arm(-0.05, 0.97, 0.1),
               "blade": (0.2, 0.93, 0.3), "armL": arm(-0.5, -0.7, -0.4, 0.95), "neck": -12,
               "hair": 28, "hair_tip": 30, "hair_sway": 16}
    # ends across the body in front of the left hip; kept forward so a side camera still sees the blade
    follow = {**contact, "yaw": 52, "coil": 10, "lean": 26, "roll": 10, "hip": (0, 0.24, -0.20),
              "armR": arm(-0.62, 0.30, -0.72), "blade": (-0.45, 0.35, -0.82), "hair": 14, "hair_tip": 32,
              "hair_sway": 26}
    settle = {**follow, "yaw": 56, "lean": 20, "hip": (0, 0.18, -0.16), "armR": arm(-0.6, 0.2, -0.78, 0.95),
              "blade": (-0.5, 0.2, -0.84), "hair": 4, "hair_tip": 10, "hair_sway": 8}
    home = toward(settle, ready(g), 0.5, blade=(-0.2, 0.75, -0.63))
    return [(0, toward(g, wind, 0.25, blade=g["blade"]), "lin"), (1, climb, "lin"), (3, wind, "out"),
            (4, contact, "lin"), (5, follow, "lin"), (7, settle, "out"), (8, home, "lin"), (9, ready(g), "lin"),
            (11, g, "inout")]


def overhead(g):
    """Move 3. One-handed overhead finisher: he LEAPS, blade chambered behind his head, and comes down
    with it into a deep crouch. 0.20 s wind-up, 0.54 s total."""
    crouch_in = {**g, "lean": 14, "hip": (0, -0.06, -0.16), "armR": arm(0.45, 0.6, -0.1, 0.8), "elbowR": ELBOW_R,
                 "blade": (0.35, 0.3, 0.88), "armL": arm(-0.3, 0.6, -0.5, 0.8), "elbowL": ELBOW_L, "neck": -8}
    # airborne: both feet tucked up off the floor, back arched, blade hanging down his back
    leap = {**crouch_in, "yaw": -16, "lean": -14, "hip": (0, 0.04, 0.16), "footR": (0, 0.10, 0.20),
            "footL": (0, -0.12, 0.26), "toeL": 30, "toeR": 20, "armR": arm(0.30, -0.25, 0.92, 0.9),
            "blade": (0.1, -0.85, -0.5), "armL": arm(-0.2, 0.9, 0.45, 0.9), "neck": 6,
            "hair": -12, "hair_tip": -16}
    rising = toward(crouch_in, leap, 0.55, armR=arm(0.45, 0.3, 0.7, 0.85), blade=(0.4, -0.5, 0.76))
    # still dropping as the blade comes over the top
    contact = {**leap, "yaw": 10, "lean": 20, "hip": (0, 0.20, 0.02), "footR": (0, 0.34, 0.05),
               "footL": (0, -0.10, 0.10), "toeL": 35, "toeR": -10, "armR": arm(0.1, 0.95, 0.30),
               "blade": (0.05, 0.92, 0.38), "armL": arm(-0.5, -0.6, -0.2, 0.9), "neck": -12,
               "hair": -20, "hair_tip": -26}
    # he lands ON the blow: as low as he goes, hair still in the air above him
    impact = {**contact, "yaw": 16, "lean": 30, "spine": 10, "hip": (0, 0.26, -0.24), "footR": (0, 0.40, 0),
              "footL": (0, -0.12, 0), "toeL": 40, "toeR": 0, "armR": arm(0.08, 0.62, -0.78),
              "blade": (0.0, 0.75, -0.66), "armL": arm(-0.55, -0.65, 0.25, 0.9), "neck": -18,
              "hair": -30, "hair_tip": -36}
    sunk = {**impact, "lean": 27, "hip": (0, 0.24, -0.21), "blade": (0.0, 0.77, -0.64), "hair": 10, "hair_tip": 14}
    return [(0, toward(g, crouch_in, 0.4), "lin"), (1, crouch_in, "out"), (2, rising, "lin"), (3, leap, "lin"),
            (4, contact, "lin"), (5, impact, "lin"), (8, sunk, "out"), (10, ready(g), "inout"), (11, g, "inout")]


def drawn(g):
    """The charged stance: low and wide, turned, sword hand across the body on the grip at his left hip
    as if sheathed, off hand steadying the scabbard. The release is one big draw cut."""
    return {**g, "yaw": 44, "coil": 10, "lean": 14, "hip": (0, -0.08, -0.18), "footR": (0, 0.18, 0),
            "footL": (0, -0.18, 0), "armR": arm(-0.67, 0.48, -0.56), "elbowR": ELBOW_R,
            "blade": (-0.4, -0.9, -0.12), "armL": arm(-0.09, 0.12, -0.99, 0.75), "elbowL": ELBOW_L,
            "neck": -8, "hair": 4, "hair_tip": 6}


def charge(g):
    """0.35 s, plays once: sink from the guard into the drawn stance, blade crossing low in front."""
    cross = toward(g, drawn(g), 0.5, blade=(-0.55, 0.6, -0.55))
    return [(0, toward(g, drawn(g), 0.12, blade=g["blade"]), "lin"), (3, cross, "lin"), (7, drawn(g), "out")]


def hold(g, phase):
    """Loops while the button is held: the drawn stance trembling with stored power."""
    b = 2 * math.pi * phase
    return {**drawn(g), "hip": (0, -0.08, -0.18 + 0.014 * math.sin(b)), "lean": 14 + 1.5 * math.sin(b + 0.6),
            "blade": (-0.4 + 0.05 * math.sin(2 * b + 0.4), -0.9, -0.12 + 0.06 * math.cos(b)),
            "hair": 6 + 5 * math.sin(b), "hair_tip": 9 + 9 * math.sin(b - 1.1),
            "hair_sway": 5 * math.sin(b + 2.2)}


hold.loops = True


def release(g):
    """Wind-up and contact shared by the cleave and the full-charge spin: coil tighter, then explode."""
    coil = {**drawn(g), "yaw": 58, "coil": 14, "lean": 16, "roll": 6, "hip": (0, -0.12, -0.20),
            "footR": (0, 0.12, 0.06), "blade": (-0.3, -0.94, -0.1), "hair_sway": 14}
    contact = {**coil, "yaw": -14, "coil": -4, "lean": 20, "roll": -8, "hip": (0, 0.28, -0.14),
               "footR": (0, 0.42, 0), "footL": (0, -0.12, 0), "toeL": 40, "armR": arm(0.12, 0.98, -0.08),
               "blade": (-0.25, 0.96, 0.0), "armL": arm(-0.5, -0.75, -0.3, 0.95), "neck": -14,
               "hair": 28, "hair_tip": 32, "hair_sway": -18}
    return coil, contact


def cleave(g):
    """Move 4. Released charge: a flat full-body draw cut that wraps round behind him.
    0.18 s wind-up, 0.58 s total."""
    coil, contact = release(g)
    follow = {**contact, "yaw": -66, "coil": -12, "lean": 14, "roll": -4, "hip": (0, 0.26, -0.10),
              "armR": arm(0.80, -0.30, -0.15), "blade": (0.55, -0.8, -0.12), "hair": 12, "hair_tip": 32,
              "hair_sway": -28}
    wrap = {**follow, "yaw": -80, "hip": (0, 0.22, -0.08), "armR": arm(0.62, -0.72, -0.2),
            "blade": (0.15, -0.97, -0.15), "hair": 8, "hair_tip": 20, "hair_sway": -14}
    rest = {**wrap, "yaw": -72, "lean": 9, "hip": (0, 0.16, -0.06), "hair": 4, "hair_tip": 8, "hair_sway": -4}
    home = toward(rest, ready(g), 0.5, blade=(0.75, 0.2, -0.5))
    return [(0, drawn(g), "lin"), (3, coil, "out"), (4, contact, "lin"), (5, follow, "lin"), (6, wrap, "lin"),
            (8, rest, "out"), (9, home, "lin"), (10, ready(g), "lin"), (11, g, "inout")]


def dash_slash(g):
    """Move 5 (RuinedOutpostGame.DASH_SLASH). A click straight out of a dash: he never stands up. From the
    dash's landing, blade still trailing behind his right hip, one low wide sweep across the front to his
    left while he slides on. Right to left, so the game mirrors its arc as it does for the diagonal.
    0.08 s wind-up, 0.36 s total, 40 px slide."""
    land = dash_land(g)
    wind = {**land, "yaw": -50, "coil": -12, "lean": 28, "roll": -6, "hip": (0, 0.0, -0.18),
            "armR": arm(0.5, -0.8, -0.33), "blade": (0.25, -0.96, -0.1), "armL": arm(-0.2, 0.9, -0.2, 0.85),
            "hair": 24, "hair_sway": -12}
    contact = {**wind, "yaw": 12, "coil": 4, "lean": 26, "roll": 8, "hip": (0, 0.28, -0.20), "footL": (0, 0.42, 0),
               "footR": (0, -0.14, 0), "toeL": 0, "toeR": 45, "armR": arm(0.05, 0.98, -0.18),
               "blade": (0.28, 0.94, -0.2), "armL": arm(-0.45, -0.8, -0.2, 0.95), "neck": -20,
               "hair": 32, "hair_tip": 34, "hair_sway": 18}
    follow = {**contact, "yaw": 58, "coil": 12, "roll": 10, "hip": (0, 0.30, -0.18),
              "armR": arm(-0.66, 0.45, -0.28), "blade": (-0.62, 0.55, -0.4), "hair": 16, "hair_tip": 32,
              "hair_sway": 28}
    settle = {**follow, "yaw": 60, "lean": 18, "roll": 4, "hip": (0, 0.20, -0.13),
              "armR": arm(-0.64, 0.35, -0.4, 0.95), "blade": (-0.6, 0.4, -0.55), "hair": 6, "hair_tip": 12,
              "hair_sway": 8}
    home = toward(settle, ready(g), 0.5, blade=(-0.2, 0.8, -0.55))
    return [(0, toward(land, wind, 0.3), "lin"), (3, wind, "out"), (4, contact, "lin"), (5, follow, "lin"),
            (7, settle, "out"), (8, home, "lin"), (9, ready(g), "lin"), (11, g, "inout")]


def spin_cleave(g):
    """Move 6 (RuinedOutpostGame.SPIN_CLEAVE). A FULL charge does not stop at the cleave: the cut carries
    him round, arm out and blade low, one whole turn, and he finishes as the cleave does.
    0.18 s wind-up, 0.70 s total. `turn` spins the finished pose, so the arm stays 'out to his front-right'
    while the world goes round; it ends at -360, which looks like 0 and never unwinds."""
    coil, contact = release(g)
    # feet come together under him to pivot, and he stays low: a spin stood upright is a pirouette
    whirl = {**contact, "yaw": -34, "coil": -8, "lean": 12, "roll": -6, "hip": (0, 0.10, -0.14),
             "footR": (0, 0.12, 0), "footL": (0, -0.10, 0), "toeL": 25, "toeR": 25,
             "armR": arm(0.45, 0.85, -0.22, 0.95), "blade": (0.42, 0.82, -0.38), "armL": arm(-0.6, -0.5, -0.5, 0.9),
             "hair": 14, "hair_tip": 26, "hair_sway": -30}
    quarters = [(5 + i, {**whirl, "turn": -90 * (i + 1)}, "lin") for i in range(4)]
    follow = {**contact, "turn": -360, "yaw": -66, "coil": -12, "lean": 14, "hip": (0, 0.22, -0.10),
              "armR": arm(0.80, -0.30, -0.15), "blade": (0.55, -0.8, -0.12), "hair": 12, "hair_tip": 30,
              "hair_sway": -26}
    return [(0, drawn(g), "lin"), (3, coil, "out"), (4, contact, "lin"), *quarters, (9, follow, "lin"),
            (10, {**ready(g), "turn": -360}, "inout"), (11, {**g, "turn": -360}, "inout")]


TRACKS = {"slash0": draw_cut, "slash1": thrust, "slash2": diagonal, "slash3": overhead, "cleave": cleave,
          "charge": charge, "hold": hold, "dashslash": dash_slash, "spincleave": spin_cleave}

# swings that leave a blade trail -> (first, last) sheet frame of the strike. Frame 3 is the end of the
# wind-up and frame 4 is contact, so the cut itself is the travel from 3 onward.
# A third value narrows the ribbon to the last part of the blade: a thrust sweeps almost no area, so a
# full-width ribbon is a flag at his hip; near the tip it is the streak a lunge should leave.
# The window closes before the recovery: coming home is quick too, and must not leave a swoosh.
TRAILS = {"slash0": (3, 7), "slash1": (3, 6, 0.75), "slash2": (3, 7), "slash3": (3, 7), "cleave": (3, 7),
          "dashslash": (3, 7), "spincleave": (3, 9)}


def poses(action, n, view):
    return posers(TRACKS, action, n, view)


def motion(action, n, view):
    """pose(p, frame) at any fractional frame: what trail.py samples between the sheet frames."""
    return poser(TRACKS, action, n, view)
