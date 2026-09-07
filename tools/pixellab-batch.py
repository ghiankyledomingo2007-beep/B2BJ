"""Batch driver over pixellab-call.py: submit a request list, then poll and download results.

Usage:
  python3 tools/pixellab-batch.py submit <batch.json> <review-dir>
  python3 tools/pixellab-batch.py poll <review-dir>

batch.json is a list of {"name", "tool", "args"}. Each request and raw reply are saved in the
review directory next to a jobs.json ledger. Credentials stay inside the helper, which redacts them.
"""
import json
import re
import subprocess
import sys
import urllib.request
from pathlib import Path

HELPER = Path(__file__).with_name("pixellab-call.py")
UUID = re.compile(r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")


def call(tool, args, request_file):
    request_file.write_text(json.dumps(args, indent=1))
    reply = subprocess.run(["python3", str(HELPER), tool, str(request_file)],
                           capture_output=True, text=True, timeout=120)
    try:
        payload = json.loads(reply.stdout)
        text = " ".join(part.get("text", "") for part in payload.get("result", {}).get("content", []))
    except ValueError:
        text = ""
    return reply.returncode, text, reply.stdout


def load_ledger(review):
    ledger = review / "jobs.json"
    return json.loads(ledger.read_text()) if ledger.exists() else []


def save_ledger(review, jobs):
    (review / "jobs.json").write_text(json.dumps(jobs, indent=1))


def submit(batch_file, review):
    review.mkdir(parents=True, exist_ok=True)
    jobs = load_ledger(review)
    for item in json.loads(Path(batch_file).read_text()):
        name, tool, args = item["name"], item["tool"], item["args"]
        code, text, raw = call(tool, args, review / f"{name}-request.json")
        (review / f"{name}-submit.json").write_text(raw)
        job = UUID.search(text)
        jobs.append({"name": name, "tool": tool, "id": job.group(0) if job else None,
                     "status": "submitted" if code == 0 and job else "failed",
                     "decision": "pending", "reply": text[:300]})
        print(name, "|", "ok" if code == 0 else "FAILED", "|", text[:220].replace("\n", " / "))
    save_ledger(review, jobs)


def poll(review):
    jobs = load_ledger(review)
    for job in jobs:
        if job["status"] not in ("submitted", "processing") or not job["id"]:
            continue
        code, text, raw = call("get_image", {"job_id": job["id"]}, review / f"{job['name']}-poll.json")
        (review / f"{job['name']}-status.json").write_text(raw)
        lower = text.lower()
        if "completed" in lower or "download" in lower:
            target = review / f"{job['name']}.png"
            if not target.exists():
                url = f"https://api.pixellab.ai/mcp/images/{job['id']}/download?index=0"
                with urllib.request.urlopen(url, timeout=60) as response:
                    target.write_bytes(response.read())
            job["status"] = "downloaded"
        elif "failed" in lower or code != 0:
            job["status"] = "failed"
        else:
            job["status"] = "processing"
        print(job["name"], "|", job["status"], "|", text[:160].replace("\n", " / "))
    save_ledger(review, jobs)


if __name__ == "__main__":
    mode = sys.argv[1]
    if mode == "submit":
        submit(sys.argv[2], Path(sys.argv[3]))
    elif mode == "poll":
        poll(Path(sys.argv[2]))
    else:
        raise SystemExit("mode must be submit or poll")
