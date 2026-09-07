"""Print the (redacted) PixelLab balance for the owner's backup allocation. Never prints the token."""
import os
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
env = dict(os.environ)
if "B2BJ_PIXELLAB_BACKUP_AUTH" not in env:
    raise SystemExit("B2BJ_PIXELLAB_BACKUP_AUTH is not present in the environment")
env["PIXELLAB_AUTH_HEADER"] = env["B2BJ_PIXELLAB_BACKUP_AUTH"]
reply = subprocess.run(["python3", str(ROOT / "tools/pixellab-call.py"), "get_balance"],
                       capture_output=True, text=True, timeout=60, env=env)
print(reply.stdout.strip())
if reply.returncode:
    print("helper exit", reply.returncode)
