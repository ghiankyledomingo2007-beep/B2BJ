"""Write retryA.json (8) / retryB.json (6): one revised attempt per rejected first-pass target.
Standing pieces: pixflux + forced palette + direction south + straight-on wording.
Ground pieces: pixen (the route that produced the accepted splinters/debris props) + "nothing else" wording.
"""
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
STONE = str(HERE / "palette-stone.png")
TIMBER = str(HERE / "palette-timber.png")
STYLE = (" Top-down video game sprite, rainy abandoned medieval military outpost, muted dark palette: dark navy "
         "slate, grey-green mud, weathered brown timber, tarnished iron. Subtle dark contact shadow only, "
         "transparent background, nothing else in the image, no ground patch, no grass, no plants, no glow, "
         "no gold, no bright colours.")
FRONT = (" Front face square to the camera, seen from the front and slightly above, straight-on, not rotated, "
         "not isometric.")
FLAT = " Lying flat on the ground, seen from directly above, one single isolated object."

STANDING = [
    ("gate-pier", 64, 112, "A ruined stone gate pier: one tall square column of dark slate masonry blocks, top "
     "broken off and crumbling, one rusted iron hinge bracket on the front." + FRONT, STONE, "medium detail"),
    ("wall-end", 48, 64, "A short broken stub of a low stone wall: three courses of dark slate blocks whose right "
     "end has crumbled into a rough taper of loose fallen blocks." + FRONT, STONE, "low detail"),
    ("watch-platform", 128, 96, "A collapsed timber watch platform: a low rectangular plank deck on wooden posts, "
     "front edge straight across, the right half fallen and broken down to the ground, a broken ladder against "
     "the front." + FRONT, TIMBER, "medium detail"),
    ("lean-to", 96, 80, "A small ruined lean-to: only two upright weathered timber posts holding one sagging torn "
     "brown canvas roof that slopes down to the back, open front, no plank walls, no floor." + FRONT, TIMBER,
     "medium detail"),
    ("weapon-rack", 64, 64, "A damaged wooden spear rack: two upright posts joined by one crossbar, three dull "
     "iron-tipped spears leaning upright in it, one post cracked." + FRONT, TIMBER, "low detail"),
    ("medical-chest", 48, 48, "A small battered closed wooden field medical chest with dull iron corner straps "
     "and a dull iron clasp on the front." + FRONT, TIMBER, "low detail"),
    ("reeds", 64, 48, "A low clump of wet dark grey-green reeds and sedge growing from dark mud, dull muted "
     "colours, no bright green." + FLAT, STONE, "low detail"),
]

GROUND = [
    ("bedroll", 64, 48, "A single worn dark blue soldier's bedroll unrolled flat with a folded grey wool blanket "
     "on top." + FLAT),
    ("stretcher", 96, 48, "One empty canvas field stretcher: two long wooden carrying poles with torn grey-brown "
     "canvas stretched between them." + FLAT),
    ("bandage", 32, 32, "A bundle of three rolled off-white linen bandage rolls tied together with a cord, "
     "filling the frame." + FLAT),
    ("signal-mast", 96, 64, "A fallen wooden signal mast with a snapped crossbar and a torn faded navy pennant "
     "still tied to it." + FLAT),
    ("sack", 48, 32, "One torn grey burlap grain sack split open with dull grey-brown grain spilling out."
     + FLAT),
    ("wheel", 32, 32, "A broken wooden cart wheel with missing spokes, only the wheel." + FLAT),
    ("canvas-debris", 64, 48, "Torn brown canvas tangled with two broken roof timbers, one loose flat pile."
     + FLAT),
]


def standing(name, w, h, desc, palette, detail):
    style = STYLE.replace("no grass, no plants, ", "") if name == "reeds" else STYLE
    args = {"description": desc + style, "width": w, "height": h, "view": "high top-down",
            "no_background": True, "detail": detail, "outline": "selective outline",
            "color_image_path": palette}
    if name != "reeds":
        args["direction"] = "south"
    return {"name": name + "-r1", "tool": "create_image_pixflux", "args": args}


def ground(name, w, h, desc):
    args = {"description": desc + STYLE, "width": w, "height": h, "view": "high top-down",
            "no_background": True, "detail": "low detail", "outline": "selective outline"}
    return {"name": name + "-r1", "tool": "create_image_pixen", "args": args}


jobs = [standing(*t) for t in STANDING] + [ground(*t) for t in GROUND]
(HERE / "retryA.json").write_text(json.dumps(jobs[:8], indent=1))
(HERE / "retryB.json").write_text(json.dumps(jobs[8:], indent=1))
print("retryA", [j["name"] for j in jobs[:8]])
print("retryB", [j["name"] for j in jobs[8:]])
