"""Offline check: no live credential reads, requests, or generation charges."""
import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("pixellab_call", Path(__file__).with_name("pixellab-call.py"))
client = importlib.util.module_from_spec(spec)
spec.loader.exec_module(client)


class PixelLabCallTest(unittest.TestCase):
    def test_temporary_auth_does_not_read_or_replace_private_config(self):
        replies = [b'{"result":{"protocolVersion":"2025-03-26"}}', b'',
                   b'{"result":{"content":[{"type":"text","text":"fake-override"}]}}']
        def respond(request, timeout):
            self.assertEqual(request.headers["Authorization"], "Bearer fake-override")
            response = io.BytesIO(replies.pop(0))
            response.headers = {}
            return response
        output = io.StringIO()
        with patch.dict(os.environ, {"PIXELLAB_AUTH_HEADER": "Bearer fake-override"}), \
             patch.object(client.sys, "argv", ["pixellab-call.py", "get_balance"]), \
             patch.object(client.Path, "read_text", side_effect=AssertionError("private config read")), \
             patch.object(client.urllib.request, "urlopen", side_effect=respond), \
             contextlib.redirect_stdout(output):
            client.main()
        self.assertNotIn("fake-override", output.getvalue())

    def test_private_auth_and_image_encoding_never_leak_to_output(self):
        requests = []
        replies = [
            {"result": {"protocolVersion": "2025-03-26"}},
            None,
            {"result": {"content": [
                {"type": "text", "text": "Bearer fake-test-secret / fake-test-secret"},
                {"type": "image", "data": "large-image-data"}], "isError": False}},
        ]

        def respond(request, timeout):
            requests.append(request)
            reply = replies.pop(0)
            response = io.BytesIO(b"" if reply is None else ("data: " + json.dumps(reply) + "\n\n").encode())
            response.headers = {"Mcp-Session-Id": "test-session"}
            return response

        config = '[mcp_servers.pixellab.env]\nAUTH_HEADER="Bearer fake-test-secret"'
        output = io.StringIO()
        with patch.object(client.sys, "argv", ["pixellab-call.py", "animate_image", "args.json"]), \
             patch.object(client.Path, "read_text", side_effect=[json.dumps({"first_frame_path": "frame.png", "init_image_path": "frame.png", "color_image_path": "frame.png", "image_paths": ["frame.png"]}), config]), \
             patch.object(client.Path, "read_bytes", return_value=b"test-png"), \
             patch.object(client.urllib.request, "urlopen", side_effect=respond), \
             contextlib.redirect_stdout(output):
            client.main()
        self.assertEqual(len(requests), 3)
        self.assertEqual(requests[2].headers["Authorization"], "Bearer fake-test-secret")
        payload = json.loads(requests[2].data)
        self.assertEqual(payload["params"]["arguments"], {
            "first_frame_base64": "dGVzdC1wbmc=", "init_image_base64": "dGVzdC1wbmc=", "color_image_base64": "dGVzdC1wbmc=",
            "images_base64": ["dGVzdC1wbmc="]})
        self.assertNotIn("fake-test-secret", output.getvalue())
        self.assertNotIn("large-image-data", output.getvalue())
        with patch.object(client.sys, "argv", ["pixellab-call.py", "delete_image"]), \
             patch.object(client.urllib.request, "urlopen") as network:
            with self.assertRaises(ValueError):
                client.main()
            network.assert_not_called()


if __name__ == "__main__":
    unittest.main()
