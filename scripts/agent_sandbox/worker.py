"""Runs INSIDE a disposable no-network container, never directly on a workstation."""
import json
from pathlib import Path
import subprocess
import sys
from html.parser import HTMLParser

payload = json.loads(sys.stdin.buffer.read(300_001))
if sys.argv[1] == "code.python":
    code = payload["code"]
    if not isinstance(code, str) or len(code) > 6000:
        raise ValueError("Invalid code size")
    exec(compile(code, "<salve-task>", "exec"), {"__name__": "__main__"})
elif sys.argv[1] == "browser.render":
    html = payload["html"]
    if not isinstance(html, str) or len(html) > 256_000:
        raise ValueError("Invalid document size")
    Path("/tmp/page.html").write_text(html, encoding="utf-8")
    rendered = subprocess.run(["chromium", "--headless=new", "--no-sandbox", "--disable-gpu", "--disable-dev-shm-usage",
                               "--user-data-dir=/tmp/chromium", "--virtual-time-budget=2000", "--timeout=8000",
                               "--dump-dom", "file:///tmp/page.html"], capture_output=True, timeout=12, check=True)

    class Reader(HTMLParser):
        def __init__(self):
            super().__init__(); self.text = []; self.links = []; self.hidden = 0
        def handle_starttag(self, tag, attrs):
            if tag in ("script", "style", "noscript"): self.hidden += 1
            if tag == "a" and len(self.links) < 12:
                for key, value in attrs:
                    if key == "href" and value and len(value) <= 500: self.links.append(value)
        def handle_endtag(self, tag):
            if tag in ("script", "style", "noscript") and self.hidden: self.hidden -= 1
        def handle_data(self, data):
            if not self.hidden: self.text.append(data)
    parser = Reader(); parser.feed(rendered.stdout[:256_000].decode("utf-8", errors="replace"))
    # Keep the JSON envelope within the bridge output limit even for non-ASCII text.
    print(json.dumps({"text": " ".join(" ".join(parser.text).split())[:3000], "links": parser.links[:6]}, ensure_ascii=False))
else:
    raise ValueError("Unknown isolated executor")
