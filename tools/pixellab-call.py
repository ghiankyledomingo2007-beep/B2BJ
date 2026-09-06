"""Scoped PixelLab fallback when the editor's MCP connection holds an old token.

Reads temporary PIXELLAB_AUTH_HEADER or owner's private config; never prints either.
Usage: python3 tools/pixellab-call.py get_balance [arguments.json]
Local first_frame_path / last_frame_path arguments are encoded without resampling.
"""
import base64
import json
import os
from pathlib import Path
import sys
import tomllib
import urllib.error
import urllib.request


def main(name=None, args=None):
    name = name or sys.argv[1]
    if name not in {"get_balance", "get_image", "list_jobs", "animate_image", "create_image_pixflux", "edit_image",
                    "create_character", "animate_character", "get_character"}:
        raise ValueError("Tool not allowed")
    args = dict(args) if args is not None else (json.loads(Path(sys.argv[2]).read_text()) if len(sys.argv) > 2 else {})
    for key in ("first_frame", "last_frame", "init_image", "color_image", "reference_image"):
        if key + "_path" in args:
            args[key + "_base64"] = base64.b64encode(Path(args.pop(key + "_path")).read_bytes()).decode()
    if "image_paths" in args:
        args["images_base64"] = [base64.b64encode(Path(path).read_bytes()).decode()
                                 for path in args.pop("image_paths")]
    auth = os.environ.get("PIXELLAB_AUTH_HEADER")
    if not auth:
        config = tomllib.loads((Path.home() / ".codex/config.toml").read_text())
        auth = config["mcp_servers"]["pixellab"]["env"]["AUTH_HEADER"]
    if not auth.startswith("Bearer ") or any(c in auth for c in "\r\n"):
        raise ValueError("Invalid authorization header")
    headers = {"Authorization": auth, "Content-Type": "application/json",
               "Accept": "application/json, text/event-stream"}

    def send(payload):
        request = urllib.request.Request("https://api.pixellab.ai/mcp",
            data=json.dumps(payload).encode(), headers=headers, method="POST")
        with urllib.request.urlopen(request, timeout=35) as response:
            session = response.headers.get("Mcp-Session-Id")
            if session:
                headers["Mcp-Session-Id"] = session
            raw = response.read().decode()
        if not raw.strip():
            return {}
        if raw.lstrip().startswith("{"):
            return json.loads(raw)
        for line in raw.splitlines():
            if line.startswith("data:"):
                return json.loads(line[5:].strip())
        raise ValueError("Unrecognized response")

    init = send({"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {
        "protocolVersion": "2025-03-26", "capabilities": {},
        "clientInfo": {"name": "b2bj-asset-review", "version": "1"}}})
    headers["MCP-Protocol-Version"] = init["result"]["protocolVersion"]
    send({"jsonrpc": "2.0", "method": "notifications/initialized"})
    result = send({"jsonrpc": "2.0", "id": 2, "method": "tools/call",
                   "params": {"name": name, "arguments": args}})
    # Images are reviewed through their download URLs, not dumped into terminal logs.
    if "result" in result and "content" in result["result"]:
        result["result"]["content"] = [c for c in result["result"]["content"] if c.get("type") == "text"]
    safe = json.dumps(result).replace(auth, "[REDACTED]").replace(auth.removeprefix("Bearer "), "[REDACTED]")
    print(safe)
    if "error" in result or result.get("result", {}).get("isError"):
        raise SystemExit(1)
    return json.loads(safe)


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        raise SystemExit("PixelLab request failed: " + type(error).__name__) from None
