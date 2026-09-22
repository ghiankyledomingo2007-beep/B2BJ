"""Everything the blade form does besides cutting: dash, the crescent cast, the riposte guard and being hurt.

Same pose language as attacks.py (channels.py). Timing from the game:
  dash   8 frames over Player.DASH_DURATION 0.16 s, once
  cast   8 frames over BLADE_CAST_DURATION 0.35 s; the crescent leaves at t=0, so the sprite opens already
         swinging and spends its frames on the follow-through. Feet stay planted (AnimationPlaybackTest).
  guard  8 frames over RIPOSTE_DURATION 0.3 s
  hurt   8 frames over 0.32 s. B2BJ.step() freezes a dead blade form at 0.20/0.32 = frame 5, so frame 5 is
         the collapsed pose: down on one knee, propped on the sword.
CombatArtTest needs all 8 frames of every row to be different pictures.
"""
import math

from channels import ELBOW_L, ELBOW_R, arm, poser, posers, ready, toward

FRAMES = {"dash": 8, "cast": 8, "guard": 8, "hurt": 8, "counter": 8}


def dash_drop(g):
    return {**g, "lean": 26, "neck": -18, "hip": (0, 0.04, -0.12), "footR": (0, 0.10, 0.02),
            "footL": (0, -0.12, 0), "toeL": 25, "armR": arm(0.4, -0.6, -0.7, 0.9), "elbowR": ELBOW_R,
            "blade": (0.2, -0.96, -0.18), "armL": arm(-0.35, 0.7, -0.6, 0.8), "elbowL": ELBOW_L,
            "hair": 14, "hair_tip": 18}


def dash_land(g):
    """How a dash ends: low, weight forward, blade still trailing. The dash slash starts from here."""
    return {**dash_drop(g), "lean": 20, "hip": (0, 0.06, -0.10), "footR": (0, 0.16, 0), "footL": (0, -0.14, 0),
            "hair": 16, "hair_tip": 26}


def dash(g):
    """A low burst: he drops, flies with the blade trailing flat behind the hip, and lands ready."""
    drop = dash_drop(g)
    fly = {**drop, "lean": 42, "neck": -30, "hip": (0, 0.10, -0.10), "footR": (0, 0.24, 0.12),
           "footL": (0, -0.32, 0.10), "toeL": 40, "toeR": -10, "armR": arm(0.35, -0.88, -0.30),
           "blade": (0.2, -0.96, -0.18), "armL": arm(-0.3, 0.85, -0.42, 0.9), "hair": 30, "hair_tip": 30}
    glide = {**fly, "lean": 40, "footR": (0, 0.26, 0.10), "footL": (0, -0.30, 0.12), "hair_tip": 34}
    return [(0, toward(g, drop, 0.6), "lin"), (2, fly, "out"), (5, glide, "lin"), (7, dash_land(g), "inout")]


def cast(g):
    """Ichor Crescent: a flat backhand flick at shoulder height that throws the arc. Feet planted."""
    chamber = {**g, "yaw": 34, "coil": 10, "lean": 8, "roll": 5, "hip": (0, -0.08, -0.10), "footR": (0, 0.06, 0),
               "footL": (0, -0.06, 0), "armR": arm(-0.7, 0.6, 0.12, 0.9), "elbowR": ELBOW_R,
               "blade": (-0.85, -0.25, 0.2), "armL": arm(-0.3, -0.3, -0.9, 0.85), "elbowL": ELBOW_L,
               "hair_sway": 10}
    # feet stay where they are (the game plants him for a cast); the throw comes from the hips and shoulders
    release = {**chamber, "yaw": -12, "coil": -4, "lean": 16, "roll": -6, "hip": (0, 0.16, -0.10),
               "toeL": 30, "armR": arm(0.1, 0.99, 0.05), "blade": (-0.2, 0.98, 0.05),
               "armL": arm(-0.5, -0.7, -0.4, 0.9), "neck": -10, "hair": 22, "hair_tip": 26, "hair_sway": -14}
    follow = {**release, "yaw": -48, "coil": -10, "lean": 12, "hip": (0, 0.14, -0.07),
              "armR": arm(0.70, 0.45, -0.25), "blade": (0.62, -0.55, -0.3), "hair": 10, "hair_tip": 28,
              "hair_sway": -24}
    settle = {**follow, "yaw": -50, "lean": 7, "hip": (0, 0.08, -0.05), "armR": arm(0.7, 0.05, -0.35, 0.95),
              "blade": (0.55, -0.65, -0.45), "hair": 4, "hair_tip": 8, "hair_sway": -6}
    home = toward(settle, ready(g), 0.5, blade=(0.75, 0.2, -0.5))
    return [(0, toward(g, chamber, 0.85), "lin"), (1, release, "lin"), (2, follow, "lin"), (4, settle, "out"),
            (5, home, "lin"), (6, ready(g), "lin"), (7, g, "inout")]


def parry_stance(g):
    return {**g, "yaw": 14, "lean": 5, "neck": -4, "hip": (0, -0.03, -0.08), "footL": (0, 0.10, 0),
            "footR": (0, -0.10, 0), "armR": arm(-0.25, 0.85, 0.30, 0.66), "elbowR": (0.6, -0.2, -0.75),
            "blade": (-0.55, 0.15, 0.82), "armL": arm(0.35, 0.85, 0.15, 0.6), "elbowL": (-0.6, -0.2, -0.75)}


def parry(g, phase):
    """Riposte: he snaps the blade up across his body, braces, and eases off as the window closes."""
    stance = parry_stance(g)
    frame = round(phase * FRAMES["guard"])
    if frame == 0:
        return toward(g, stance, 0.55)
    if frame >= 6:
        return toward(stance, g, 0.4 if frame == 6 else 0.8)
    # braced: the snap overshoots on frame 1, then the blade shivers against the load
    shiver = 2 * math.pi * frame / 5
    return {**stance, "hip": (0, -0.03, -0.08 - (0.03 if frame == 1 else 0.008 * math.sin(shiver))),
            "blade": (-0.55 + 0.07 * math.sin(shiver + 0.5), 0.15, 0.82), "lean": 5 + 1.5 * math.cos(shiver)}


parry.loops = True          # posed per frame from its phase rather than from keys


def hurt(g):
    """Knocked back, buckles to a knee propped on the sword (frame 5, also the dead body), starts to rise."""
    hit = {**g, "lean": -16, "spine": -8, "neck": 14, "hip": (0, -0.08, -0.02), "footR": (0, -0.10, 0.03),
           "armR": arm(0.75, 0.25, -0.6, 0.95), "elbowR": ELBOW_R, "blade": (0.5, 0.6, 0.62),
           "armL": arm(-0.8, 0.3, -0.2, 0.95), "elbowL": ELBOW_L, "hair": -10, "hair_tip": -14}
    reel = {**hit, "lean": -22, "hip": (0, -0.14, -0.05), "footR": (0, -0.18, 0), "footL": (0, -0.04, 0)}
    buckle = {**g, "lean": 14, "spine": 10, "neck": 16, "hip": (0, -0.12, -0.18), "footR": (0, -0.16, 0),
              "footL": (0, 0.06, 0), "armR": arm(0.45, 0.55, -0.7, 0.8), "elbowR": ELBOW_R,
              "blade": (0.3, 0.9, -0.3), "armL": arm(-0.4, 0.3, -0.86, 0.9), "elbowL": ELBOW_L,
              "hair": 16, "hair_tip": 20}
    knee = {**buckle, "lean": 24, "neck": 22, "hip": (0, -0.08, -0.26), "toeR": 40, "blade": (0.3, 0.94, -0.16)}      # the sword lies along the ground: he is too low to stand it up
    return [(0, toward(g, hit, 0.7), "lin"), (1, reel, "out"), (3, buckle, "inout"), (5, knee, "out"),
            (7, toward(knee, g, 0.6), "inout")]


def counter(g):
    """The answer to a caught hit (RIPOSTE_COUNTER). It begins in the parry: the blade whips back over his
    left shoulder and comes down through the attacker to low on his right, with a step in. 8 frames over
    RuinedOutpostGame.COUNTER_DURATION; the game has already dealt the damage, this is what it looks like."""
    stance = parry_stance(g)
    # the parry gave ground; the whip-back takes a little more and rises, then all of it comes forward
    whip = {**stance, "yaw": 38, "coil": 10, "lean": -4, "roll": 6, "hip": (0, -0.10, -0.02),
            "footR": (0, 0.04, 0.06), "armR": arm(-0.55, 0.55, 0.62, 0.8), "elbowR": ELBOW_R,
            "blade": (-0.45, -0.6, 0.66), "armL": arm(-0.3, -0.2, -0.93, 0.85), "elbowL": ELBOW_L,
            "hair": -8, "hair_sway": 12}
    contact = {**whip, "yaw": -12, "coil": -4, "lean": 20, "roll": -8, "hip": (0, 0.26, -0.14),
               "footR": (0, 0.38, 0), "footL": (0, -0.12, 0), "toeL": 38, "armR": arm(0.1, 0.97, -0.05),
               "blade": (-0.25, 0.95, 0.12), "armL": arm(-0.5, -0.7, -0.4, 0.95), "neck": -14,
               "hair": 28, "hair_tip": 30, "hair_sway": -16}
    follow = {**contact, "yaw": -54, "coil": -10, "lean": 22, "roll": -10, "hip": (0, 0.26, -0.18),
              "armR": arm(0.66, 0.2, -0.7), "blade": (0.6, -0.3, -0.72), "hair": 12, "hair_tip": 32,
              "hair_sway": -26}
    settle = {**follow, "yaw": -56, "lean": 14, "roll": -4, "hip": (0, 0.18, -0.12),
              "armR": arm(0.62, 0.05, -0.76, 0.95), "blade": (0.5, -0.5, -0.7), "hair": 4, "hair_tip": 10,
              "hair_sway": -6}
    home = toward(settle, ready(g), 0.5, blade=(0.7, 0.3, -0.6))
    return [(0, whip, "lin"), (1, contact, "lin"), (2, follow, "lin"), (4, settle, "out"), (5, home, "lin"),
            (6, ready(g), "lin"), (7, g, "inout")]


TRACKS = {"dash": dash, "cast": cast, "guard": parry, "hurt": hurt, "counter": counter}


# the crescent leaves at t=0 and the counter answers at once, so both trail from the first frame
TRAILS = {"cast": (0, 4), "counter": (0, 4)}


def poses(action, n, view):
    return posers(TRACKS, action, n, view)


def motion(action, n, view):
    return poser(TRACKS, action, n, view)
