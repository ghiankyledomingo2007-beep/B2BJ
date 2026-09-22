"""Locomotion for the Rainoray puppet: one blend between a standing low guard and a full sword sprint.

amp 0 = standing and breathing, amp 1 = sprint. Every angle is mix(idle, run, amp), so `start`
ramps amp up and lands exactly on run phase 0, and `stop` ramps it back down.
About +X a positive angle leans the torso forward (-Y) and swings a hanging limb backward,
so "leg forward" is negative.
"""
import math

from mathutils import Vector

from puppet import X_AXIS, Y_AXIS, Z_AXIS, mix

# action -> default frame count. Must match src/BladeAnimation.java.
FRAMES = {"run": 8, "idle": 8, "start": 4, "stop": 4, "turn": 4}


def pose(p, t, amp, breath, view):
    """t = stride phase [0,1), amp = 0 standing .. 1 sprint, breath = idle phase [0,1)."""
    a, b = 2 * math.pi * t, 2 * math.pi * breath
    twist = view["twist"]          # pelvis/shoulder twist reads as a goofy wobble from the front or back
    p.begin()
    p.move("hips", Vector((0, 0, mix(0.022 * math.sin(b), 0.075 * math.cos(2 * a) - 0.035, amp))))
    p.turn("hips", X_AXIS, mix(2, 24, amp))                                  # committed forward lean
    p.turn("hips", Y_AXIS, mix(1.6 * math.cos(b), 7 * math.sin(a) * twist, amp))
    p.turn("hips", Z_AXIS, amp * 15 * math.sin(a) * twist)                   # pelvis follows the leading leg
    p.turn("spine", X_AXIS, mix(1 + 2.6 * math.sin(b), 7, amp))              # breathing when standing
    p.turn("chest", Z_AXIS, amp * -22 * math.sin(a) * twist)                 # shoulders counter the pelvis
    p.turn("neck", X_AXIS, mix(-2, -26, amp))                                # eyes stay on the target
    p.finish_head(view["face_turn"])                                         # show the mask to a side camera
    # the hair is a thin shell: past ~35 degrees it turns edge-on to a side camera and vanishes
    p.turn("hair", X_AXIS, mix(4 + 3.5 * math.sin(b - 0.5), 24 + 6 * math.sin(2 * a + 1.0), amp))
    p.turn("hair_tip", X_AXIS, mix(3 + 6 * math.sin(b - 1.4), 16 + 12 * math.sin(2 * a + 0.2), amp))

    for side, ph in ((".L", 0.0), (".R", math.pi)):
        s = math.sin(a + ph)
        fold = 0.5 * (1 + math.sin(a + ph + 1.9))                            # heel tucks up during recovery
        p.turn("thigh" + side, X_AXIS, amp * (-30 - 68 * s))                 # knee to the chest, leg far behind
        p.turn("shin" + side, X_AXIS, amp * (8 + 122 * fold))
        p.turn("foot" + side, X_AXIS, amp * (-12 + 30 * fold))

    # free arm: hangs when standing, pumps opposite its own leg when running
    s = math.sin(a + math.pi)
    p.turn("upperarm.L", Y_AXIS, 38)                                         # out of the A-pose, beside the ribs
    p.turn("upperarm.L", X_AXIS, amp * (-72 * s + 5))
    p.turn("forearm.L", X_AXIS, mix(-12 + 4 * math.sin(b + 0.7), -95 - 15 * s, amp))

    # sword arm: low guard when standing; locked back with the blade trailing when running
    p.turn("upperarm.R", Y_AXIS, mix(-30, view["arm_in"], amp))
    p.turn("upperarm.R", X_AXIS, amp * (view["arm_back"] + 4 * math.sin(2 * a)))
    p.turn("forearm.R", X_AXIS, mix(-8 + 5 * math.sin(b + 2.0), 10, amp))    # the guard drifts as he breathes
    p.turn("hand.R", Z_AXIS, amp * view["sword_yaw"])                        # blade from forward-out to behind
    # lift early (sqrt) so the tip never dips under the floor while the arm is still swinging back
    p.turn("hand.R", X_AXIS, math.sqrt(amp) * (58 + 5 * math.sin(2 * a + 0.8)))


def frames(action, n):
    """-> list of (stride phase, amplitude, breath phase), one per sheet frame."""
    if action == "run":
        return [(i / n, 1.0, 0.0) for i in range(n)]
    if action == "idle":
        return [(0.0, 0.0, i / n) for i in range(n)]
    if action in ("start", "turn"):                                           # lands exactly on run phase 0
        return [((i - n) / 12.0, 0.25 + 0.75 * (i + 1) / n, 0.0) for i in range(n)]
    if action == "stop":                                                      # strides shorten into the guard
        return [(0.05 * i, 0.8 - 0.7 * i / (n - 1), 0.0) for i in range(n)]
    raise SystemExit("unknown locomotion action " + action)


def poses(action, n, view):
    """-> one callable per frame; calling it poses the puppet."""
    return [lambda p, f=f: pose(p, *f, view) for f in frames(action, n)]
