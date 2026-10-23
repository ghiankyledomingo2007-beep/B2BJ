"""Verify the editable animation exports the installed runtime pixels without loss."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

from PIL import Image

root = Path(__file__).resolve().parents[2]
editor = os.environ.get("PIXIE_DUST", str(Path.home() / ".local/bin/pixie-dust"))
project = root / "assets/source/characters/rainoray/Rainoray-Human.pixie-dust"
script = root / "tools/art/rainoray-project.lua"
original = hashlib.sha256(project.read_bytes()).hexdigest()


def export(out):
    return subprocess.run([editor, "--batch", "--script-param", "mode=export",
                           "--script-param", f"root={root}", "--script-param", f"out={out}",
                           "--script", str(script)], capture_output=True, text=True)


with tempfile.TemporaryDirectory(prefix="rainoray-timeline-") as temporary:
    temporary = Path(temporary)
    out = temporary / "export"
    result = export(out)
    assert result.returncode == 0, result.stdout + result.stderr
    files = sorted(out.glob("*.png"))
    assert len(files) == 18, "14 body sheets and four effects sheets"
    for file in files:
        folder = "effects" if file.stem.endswith("_trail") else "characters/blade"
        actual = Image.open(file).convert("RGBA")
        expected = Image.open(root / "assets" / folder / file.name).convert("RGBA")
        assert actual.size == expected.size and actual.tobytes() == expected.tobytes(), file.name

    hashes = {f.name: hashlib.sha256(f.read_bytes()).hexdigest() for f in files}
    refused = export(out)
    assert "Refusing to overwrite" in refused.stdout + refused.stderr
    assert hashes == {f.name: hashlib.sha256(f.read_bytes()).hexdigest() for f in files}

    metadata = temporary / "frames.json"
    subprocess.run([editor, "--batch", "--list-tags", "--list-layers", str(project),
                    "--format", "json-array", "--data", str(metadata)], check=True, capture_output=True)
    data = json.loads(metadata.read_text())
    assert len(data["frames"]) == 672 and len(data["meta"]["frameTags"]) == 76
    tags = {tag["name"]: tag for tag in data["meta"]["frameTags"]}
    for name, total in {"run_south": 500, "dash_west": 120, "draw_cut_east": 300,
                        "spin_cleave_north": 700, "cast_south": 350}.items():
        tag = tags[name]
        durations = [f["duration"] for f in data["frames"][tag["from"]:tag["to"]+1]]
        assert sum(durations) == total, (name, durations)
        if name == "draw_cut_east":
            assert sum(durations[:4]) == 90, "contact starts after the windup"

assert hashlib.sha256(project.read_bytes()).hexdigest() == original, "export must preserve the native project"
print("PASS: 672 frames, 76 clips, game timings, 18 lossless sheets and overwrite protection")
