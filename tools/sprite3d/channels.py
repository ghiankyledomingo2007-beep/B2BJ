"""Channel poses for the Rainoray puppet: the engine every planted action (attacks, dash, guard, hurt) uses.

A pose is a dict of CHANNELS in the character's own frame (right, forward, up; world units, 1 ~ 36 px):
  root      whole body slides in the cell, feet included (r, f)
  hip       hips move, feet stay planted (r, f, u)          lean / roll   hips pitch forward / tilt
  yaw       torso turn, + brings the RIGHT shoulder forward  coil          extra turn of the chest only
  turn      the WHOLE posed body spun about the vertical through his hips (a spin attack), feet and all
  spine neck look   small pitches and an extra head turn     hair hair_tip hair_sway   hair swing back / sideways
  footL footR   ankle offsets from the rest stance (leg IK)  toeL toeR     heel raise, degrees
  armR armL     (r, f, u, reach): direction FROM THE SHOULDER and reach 0..1 of the full arm (arm IK)
  blade     the direction the sword points

Arms are aimed from the shoulder on purpose. A one-handed cut is a big rotation driven by hips and
shoulder with the arm extended and the blade continuing the arm's line; placing the hand near the chest
and spinning the blade at the wrist reads as a dagger flick. Arm and blade directions are slerped between
keys, so the hand travels an arc around the shoulder and the tip travels a bigger one.
"""
import math

from mathutils import Vector

import locomotion
from puppet import X_AXIS, Y_AXIS, Z_AXIS, body, mix

ELBOW_R, ELBOW_L = (0.5, -0.5, -0.7), (-0.5, -0.5, -0.7)      # elbows bend out, back and down
ARMS = {"armR", "armL"}
DIRECTIONS = {"blade"}
YAW_IN_HIPS = 0.35                                             # the pelvis turns less than the shoulders
HEAD_COUNTER = 0.8                                             # the mask keeps facing the target
PLANTED = True          # feet are placed by IK on the rest floor, so these actions share the idle's floor


def arm(right, forward, up, reach=1.0):
    v = Vector((right, forward, up)).normalized()
    return (v.x, v.y, v.z, reach)


def arm_length(p, side):
    upper, lower = "upperarm" + side, "forearm" + side
    return (p.tail(upper) - p.head(upper)).length + (p.tail(lower) - p.head(lower)).length


def measure_guard(p, view):
    """The idle pose expressed as channels, so every action leaves from and returns to exactly that stance."""
    locomotion.pose(p, 0.0, 0.0, 0.0, view)

    def rel(v):
        return (-v.x, -v.y, v.z)

    def arm_of(side):
        reach = p.head("hand" + side) - p.head("upperarm" + side)
        return (*rel(reach.normalized()), reach.length / arm_length(p, side))

    return {
        "root": (0.0, 0.0), "hip": (0.0, 0.0, locomotion.READY_SINK), "lean": 2.0, "roll": 1.6, "yaw": 0.0, "coil": 0.0,
        "turn": 0.0,
        "spine": 1.0, "neck": -2.0, "look": 0.0, "hair": 2.0, "hair_tip": 0.0, "hair_sway": 0.0,
        "footL": locomotion.STANCE[".L"], "footR": locomotion.STANCE[".R"], "toeL": 0.0, "toeR": 0.0,
        "armR": arm_of(".R"), "elbowR": rel(p.head("forearm.R") - p.head("upperarm.R")),
        "armL": arm_of(".L"), "elbowL": rel(p.head("forearm.L") - p.head("upperarm.L")),
        "blade": rel(p.direction("blade")),
    }


def apply(p, ch, view):
    """Pose the puppet from one channel set."""
    p.begin()
    ankles = {s: p.head("foot" + s) for s in (".L", ".R")}
    foot_dir = p.direction("foot.L")
    foot_length = (p.tail("foot.L") - p.head("foot.L")).length
    slide = body(ch["root"][0], ch["root"][1], 0.0)
    p.move("hips", slide + body(*ch["hip"]))
    p.turn("hips", X_AXIS, ch["lean"])
    p.turn("hips", Y_AXIS, ch["roll"])
    p.turn("hips", Z_AXIS, ch["yaw"] * YAW_IN_HIPS)
    p.turn("spine", X_AXIS, ch["spine"])
    p.turn("chest", Z_AXIS, ch["yaw"] * (1 - YAW_IN_HIPS) + ch["coil"])
    p.turn("neck", X_AXIS, ch["neck"])
    p.finish_head(view["face_turn"] + ch["look"] - HEAD_COUNTER * (ch["yaw"] + ch["coil"]))
    p.turn("hair", X_AXIS, ch["hair"])
    p.turn("hair", Y_AXIS, ch["hair_sway"])
    p.turn("hair_tip", X_AXIS, ch["hair_tip"])
    p.turn("hair_tip", Y_AXIS, ch["hair_sway"] * 0.8)           # the tips swing wider than the roots

    for side, out in ((".L", -0.2), (".R", 0.2)):
        key = side[1]
        heel = body(0, 0, foot_length * math.sin(math.radians(max(ch["toe" + key], 0.0))))   # toes stay down
        p.ik("thigh" + side, "shin" + side, ankles[side] + slide + heel + body(*ch["foot" + key]),
             body(out, 1.0, 0.0))
        p.point("foot" + side, foot_dir)
        p.turn("foot" + side, X_AXIS, ch["toe" + key])          # positive lifts the heel

    # A thrust aimed straight along the camera's line of sight is a dot from the front and hides behind
    # him from the back. Those facings may bend anything pointing nearly dead ahead (and only that, so
    # the guard and the diagonal cuts keep the exact pose the idle has): down for the front camera,
    # out past his side for the back one.
    bias = Vector(view.get("ahead_bias", (0.0, 0.0, 0.0)))

    def aimed(direction):
        d = Vector(direction).normalized()
        return d + bias * min(max((d.y - 0.8) / 0.2, 0.0), 1.0)

    for side in (".R", ".L"):
        key = side[1]
        r, f, u, reach = ch["arm" + key]
        along = aimed((r, f, u)) if side == ".R" else Vector((r, f, u))
        target = p.head("upperarm" + side) + body(*along).normalized() * arm_length(p, side) * min(reach, 0.999)
        p.ik("upperarm" + side, "forearm" + side, target, body(*ch["elbow" + key]))
    p.point("blade", body(*aimed(ch["blade"])))
    # Last, and rigid: everything above was posed facing forward, then the finished body is spun as one
    # piece. Every other channel therefore keeps meaning "relative to him", whichever way he has turned.
    p.turn("hips", Z_AXIS, ch["turn"])


# ---------------------------------------------------------------- interpolation

EASE = {
    "lin": lambda t: t,
    "in": lambda t: t * t,                                      # slow start
    "out": lambda t: 1 - (1 - t) * (1 - t),                     # fast start, settles
    "inout": lambda t: t * t * (3 - 2 * t),
}


def swing(a, b, t):
    va, vb = Vector(a), Vector(b)
    if va.length < 1e-6 or vb.length < 1e-6:
        return va.lerp(vb, t)
    return va.normalized().slerp(vb.normalized(), t)


def blend(a, b, t, name):
    if isinstance(a, (int, float)):
        return mix(a, b, t)
    if name in ARMS:                                            # swing the direction, ease the reach
        return (*swing(a[:3], b[:3], t), mix(a[3], b[3], t))
    if name in DIRECTIONS:
        return tuple(swing(a, b, t))
    return tuple(Vector(a).lerp(Vector(b), t))


def at(keys, frame):
    """keys: [(frame, channels, ease into this key)], frames ascending."""
    if frame <= keys[0][0]:
        return keys[0][1]
    for (f0, c0, _), (f1, c1, ease) in zip(keys, keys[1:]):
        if frame <= f1:
            t = EASE[ease]((frame - f0) / (f1 - f0))
            return {k: blend(c0[k], c1[k], t, k) for k in c0}
    return keys[-1][1]


def toward(a, b, t, **override):
    """Channels a fraction of the way from a to b, with overrides: a hand-placed in-between."""
    return {**at([(0, a, "lin"), (1, b, "lin")], t), **override}


def ready(g):
    """Recovery waypoint: sword hand in front, blade forward and a little down. Coming home through it,
    the blade travels round the front of him, never down through the floor."""
    return {**g, "lean": 5, "hip": (0, 0.02, -0.03), "armR": arm(0.15, 0.75, -0.65, 0.8), "elbowR": ELBOW_R,
            "blade": (0.12, 0.9, -0.35), "elbowL": ELBOW_L}


def poser(tracks, action, n, view):
    """-> pose(p, frame) for any frame position, whole or fractional. tracks[action](guard) returns keys,
    or tracks[action](guard, phase) returns channels directly (loops and per-frame tracks)."""
    cache = {}

    def pose(p, frame):
        if "guard" not in cache:
            cache["guard"] = measure_guard(p, view)
        track, g = tracks[action], cache["guard"]
        apply(p, track(g, frame / n) if getattr(track, "loops", False) else at(track(g), frame), view)

    return pose


def posers(tracks, action, n, view):
    """-> one callable per sheet frame."""
    pose = poser(tracks, action, n, view)
    return [lambda p, i=i: pose(p, i) for i in range(n)]
