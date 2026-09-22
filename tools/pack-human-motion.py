"""Pack reviewed native frames from selection.json; no PixelLab access or credentials."""
import json
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "docs/art-review/human-motion-fix"

def main():
    manifest = json.loads((ART / "selection.json").read_text())
    with tempfile.TemporaryDirectory(prefix="b2bj-human-pack-") as work:
        subprocess.run(["javac", "--release", "17", "-d", work, str(ROOT / "tools/PackFrames.java")], check=True)
        for target, spec in manifest["sheets"].items():
            frames = [str(ART / frame) for frame in spec["frames"]]
            subprocess.run(["java", "-Djava.awt.headless=true", "-cp", work, "PackFrames",
                            "80", "80", str(spec["columns"]), "-1", str(ROOT / target), *frames], check=True)
    shutil.copyfile(ART / manifest["portrait"], ROOT / "assets/ui/blade.png")

if __name__ == "__main__":
    main()
