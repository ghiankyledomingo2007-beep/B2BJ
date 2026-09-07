"""Write wave1.json / wave2.json request lists for tools/pixellab-batch.py (8 + 7 jobs)."""
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
STONE = str(HERE / "palette-stone.png")
TIMBER = str(HERE / "palette-timber.png")
STYLE = (" Top-down video game sprite, rainy abandoned medieval military outpost, muted dark palette: "
         "dark navy slate, grey-green mud, weathered brown timber, tarnished iron. Subtle dark contact "
         "shadow only, transparent background, no grass, no glow, no gold, no bright colours.")
CAM = " seen from a high top-down three-quarter camera."
ABOVE = " seen from directly above, one single object."

TARGETS = [
    # name, w, h, description, palette, detail
    ("gate-pier", 64, 112, "A ruined stone gate pier: one tall square masonry column of dark slate blocks, "
     "top broken off and crumbling, one rusted iron hinge bracket on its side," + CAM, STONE, "medium detail"),
    ("watch-platform", 128, 96, "A collapsed timber watch platform: a low raised plank deck on four wooden posts, "
     "one side fallen and sagging to the ground, a broken ladder leaning against it," + CAM, TIMBER, "medium detail"),
    ("tower-footing", 128, 128, "A collapsed round stone signal tower footing: a ring of dark slate masonry two "
     "courses high, broken open on the south side, loose rubble inside the ring," + CAM, STONE, "medium detail"),
    ("wall-end", 48, 64, "The broken end fragment of a low dark slate stone wall, crumbling stepped blocks tapering "
     "down to loose rubble," + CAM, STONE, "low detail"),
    ("lean-to", 96, 80, "A small ruined lean-to shelter: two upright weathered timber posts holding one sagging torn "
     "brown canvas roof that slopes down to the back, open front," + CAM, TIMBER, "medium detail"),
    ("weapon-rack", 64, 64, "A damaged wooden spear rack: two upright posts and a crossbar holding three dull "
     "iron-tipped spears, one post cracked and leaning," + CAM + " Not isometric.", TIMBER, "low detail"),
    ("bedroll", 64, 48, "A single worn dark blue soldier's bedroll unrolled flat on the ground with a folded grey "
     "wool blanket on top," + ABOVE, None, "low detail"),
    ("stretcher", 96, 48, "One empty canvas field stretcher lying flat on mud: two long wooden carrying poles with "
     "torn grey-brown canvas stretched between them," + ABOVE, TIMBER, "low detail"),
    # wave 2
    ("medical-chest", 48, 48, "A small battered closed wooden field medical chest with dull iron corner straps and "
     "a dull iron clasp," + CAM, TIMBER, "low detail"),
    ("bandage", 32, 32, "A small bundle of rolled off-white linen bandages tied with a cord, lying on the "
     "ground," + ABOVE, None, "low detail"),
    ("signal-mast", 96, 64, "A fallen wooden signal mast lying on the ground with a snapped crossbar and a torn "
     "faded navy pennant," + ABOVE, None, "low detail"),
    ("reeds", 64, 48, "A low clump of wet dark grey-green reeds and sedge growing from mud, dull muted colours,"
     + ABOVE, None, "low detail"),
    ("sack", 48, 32, "One torn grey burlap grain sack lying on the ground, split open with dull grain spilling "
     "out," + ABOVE, None, "low detail"),
    ("wheel", 32, 32, "A broken wooden cart wheel lying flat on the ground, missing spokes," + ABOVE, TIMBER,
     "low detail"),
    ("canvas-debris", 64, 48, "Torn brown canvas tangled with two broken roof timbers lying flat on the ground, "
     "one loose pile," + ABOVE, TIMBER, "low detail"),
]


def request(name, w, h, desc, palette, detail):
    style = STYLE.replace("no grass, ", "") if name == "reeds" else STYLE
    args = {"description": desc + style, "width": w, "height": h, "view": "high top-down",
            "no_background": True, "detail": detail, "outline": "selective outline"}
    if palette:
        args["color_image_path"] = palette
    return {"name": name, "tool": "create_image_pixflux", "args": args}


jobs = [request(*t) for t in TARGETS]
(HERE / "wave1.json").write_text(json.dumps(jobs[:8], indent=1))
(HERE / "wave2.json").write_text(json.dumps(jobs[8:], indent=1))
print("wave1", [j["name"] for j in jobs[:8]])
print("wave2", [j["name"] for j in jobs[8:]])
