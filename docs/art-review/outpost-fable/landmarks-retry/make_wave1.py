import json
from pathlib import Path

WT = Path("/home/ghiankylledomingo/gamedev-vault/Projects/B2BJ/.claude/worktrees/agent-a5e1019b49145f622")
LM = WT / "docs/art-review/outpost-fable/landmarks"
PROPS = WT / "assets/props/outpost"
TAIL = " Transparent background, muted dark colors, no green, no grass, no sand, nothing else in the image."


def pixen(name, src, text):
    return {"name": name, "tool": "edit_image_pixen",
            "args": {"image_path": str(src), "description": text + TAIL, "no_background": True}}


batch = [
    pixen("bedroll", LM / "bedroll-rejected-2.png",
          "Unroll the bedroll so it lies flat on the ground as a worn dark-blue soldier's bedroll with a folded grey blanket on top. "
          "Remove the brown mud patch under it; replace with a thin dark wet-mud contact shadow only."),
    pixen("stretcher", LM / "stretcher-rejected-2.png",
          "Remove the green moss tufts between and around the poles; replace with a thin dark wet-mud contact shadow only. "
          "Keep the two wooden poles and the canvas wraps identical."),
    pixen("watch-platform", LM / "watch-platform-rejected-2.png",
          "Remove the brown mud island and grass under the platform; replace with a thin dark wet-mud contact shadow only under the posts and ladder. "
          "Keep the wooden platform, posts and ladder identical."),
    pixen("weapon-rack", LM / "weapon-rack-rejected-2.png",
          "Remove the grass and dirt pedestal under the rack; replace with a thin dark wet-mud contact shadow only. "
          "Keep the wooden rack and the spears identical."),
    pixen("gate-pier-mud", PROPS / "gate-pier.png",
          "Remove the small grass tufts at the base of the stone column; replace with a thin dark wet-mud contact shadow only. "
          "Keep the slate masonry column identical."),
    pixen("wall-end-mud", PROPS / "wall-end.png",
          "Remove the small grass tufts at the base of the stone wall stub; replace with a thin dark wet-mud contact shadow only. "
          "Keep the slate masonry identical."),
    {"name": "lean-to", "tool": "create_image_pixflux", "args": {
        "description": "front view of a small ruined open-front lean-to shelter: two upright timber posts at the front, a sagging torn brown canvas roof sloping back, dark shadowed interior, seen from a high top-down three-quarter camera facing straight on, no side walls, no grass, no ground patch, subtle dark contact shadow, transparent background",
        "width": 96, "height": 80, "direction": "south", "view": "high top-down", "no_background": True,
        "outline": "selective outline", "detail": "medium detail",
        "color_image_path": str(PROPS / "barricade.png")}},
]
Path(__file__).with_name("wave1.json").write_text(json.dumps(batch, indent=1))
print(len(batch), "requests")
