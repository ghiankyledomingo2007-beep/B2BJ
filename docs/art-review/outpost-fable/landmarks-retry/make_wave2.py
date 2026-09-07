import json
from pathlib import Path

WT = Path("/home/ghiankylledomingo/gamedev-vault/Projects/B2BJ/.claude/worktrees/agent-a5e1019b49145f622")
LM = WT / "docs/art-review/outpost-fable/landmarks"
HERE = WT / "docs/art-review/outpost-fable/landmarks-retry"
PROPS = WT / "assets/props/outpost"
TAIL = " Transparent background, muted dark colors, no green, no grass, no sand, nothing else in the image."

batch = [
    {"name": "watch-platform-r2", "tool": "edit_image_pixen", "args": {
        "image_path": str(LM / "watch-platform-rejected-2.png"),
        "description": "Remove the brown mud island and grass under the platform completely. Add only a very small, very dark, "
                       "near-black wet-mud contact shadow directly under the four post feet and the ladder feet. "
                       "No large grey ground shape, no pale shadow, nothing drawn under the deck. "
                       "Keep the wooden platform, posts and ladder identical." + TAIL,
        "no_background": True, "seed": 7}},
    {"name": "lean-to-fix", "tool": "edit_image_pixen", "args": {
        "image_path": str(HERE / "lean-to-rejected-1.png"),
        "description": "Remove the grass tufts and the brown dirt patch under the shelter completely; replace with a thin dark "
                       "wet-mud contact shadow only under the two front posts. Keep the timber posts, the sagging canvas roof "
                       "and the dark interior identical." + TAIL,
        "no_background": True}},
    {"name": "lean-to-r2", "tool": "create_image_pixflux", "args": {
        "description": "front view of a small ruined open-front lean-to shelter: two upright timber posts at the front, a sagging "
                       "torn brown canvas roof sloping back, dark shadowed interior, seen from a high top-down three-quarter camera "
                       "facing straight on, no side walls. Floating on a fully transparent background with no ground drawn at all: "
                       "no grass, no dirt, no ground patch, no pedestal, only a thin dark contact shadow under the two posts",
        "width": 96, "height": 80, "direction": "south", "view": "high top-down", "no_background": True,
        "outline": "selective outline", "detail": "medium detail", "seed": 7,
        "color_image_path": str(PROPS / "barricade.png")}},
]
Path(__file__).with_name("wave2.json").write_text(json.dumps(batch, indent=1))
print(len(batch), "requests")
