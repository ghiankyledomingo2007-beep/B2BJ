"""Check region routing and preservation without opening an editor or changing real art."""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile


def main():
    repo = Path(__file__).resolve().parents[2]
    cases = {
        "assets/tilesets/outpost/outpost_tileset.png": "outpost",
        "assets/tilesets/forest/forest_tileset.png": "forest",
        "assets/props/forest/set_oak.png": "forest",
        "assets/characters/forest/rift_imp_walk.png": "forest",
        "assets/characters/guardian/briarheart_motion.png": "forest",
        "assets/effects/briar_spike.png": "forest",
        "assets/effects/warden_spike.png": "outpost",
        "assets/effects/fire_camp.png": "shared",
    }
    with tempfile.TemporaryDirectory(prefix="b2bj-source-export-") as folder:
        root = Path(folder)
        script = root / "tools/art/export-sources.sh"
        script.parent.mkdir(parents=True)
        shutil.copy2(repo / "tools/art/export-sources.sh", script)
        # Populate all real input names so every shell glob has a match.
        for source in (repo / "assets").rglob("*.png"):
            fixture = root / source.relative_to(repo)
            fixture.parent.mkdir(parents=True, exist_ok=True)
            fixture.touch()
        palette = root / "assets/source/shared/outpost_night.gpl"
        palette.parent.mkdir(parents=True)
        palette.write_text("test palette")
        kept = root / "assets/source/outpost/outpost_tileset.pixie-dust"
        kept.parent.mkdir(parents=True)
        kept.write_text("teammate layers")
        editor = root / "editor"
        editor.write_text("#!/usr/bin/env python3\nfrom pathlib import Path\nimport sys\n"
                          "assert Path(sys.argv[4]).is_file()\n"
                          "Path(sys.argv[6]).write_text(sys.argv[2])\n")
        editor.chmod(0o700)
        env = dict(os.environ, PIXIE_DUST=str(editor))
        for _ in range(2):
            subprocess.run(["bash", str(script)], cwd=root, env=env, check=True, capture_output=True)
            assert kept.read_text() == "teammate layers"
            for source, area in cases.items():
                name = Path(source).stem + ".pixie-dust"
                target = root / "assets/source" / area / name
                assert target.is_file(), target
                if target != kept:
                    assert target.read_text() == source
                assert len(list((root / "assets/source").glob("*/" + name))) == 1
    print("ExportSourcesTest passed: region routing, existing layers and repeat exports")


if __name__ == "__main__":
    main()
