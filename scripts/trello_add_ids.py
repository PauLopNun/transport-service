"""
Prefixes every open team-trucks card with [TRK-{idShort}] and
updates CLAUDE.md with the branch naming convention.
"""

import os`nimport requests, time, sys, io, re
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

KEY   = os.environ["TRELLO_KEY"]
TOKEN = os.environ["TRELLO_TOKEN"]
BASE  = "https://api.trello.com/1"
BOARD = "WtpHmvMt"

cards = requests.get(f"{BASE}/boards/{BOARD}/cards", params={
    "key": KEY, "token": TOKEN,
    "filter": "open",
    "fields": "id,name,idShort,labels",
}).json()

trucks = [c for c in cards if any(l["name"] == "team-trucks" for l in c.get("labels", []))]
print(f"Found {len(trucks)} open team-trucks cards\n")

PREFIX_RE = re.compile(r"^\[TRK-\d+\]")

ok = skip = 0
for card in sorted(trucks, key=lambda c: c["idShort"]):
    name = card["name"]
    short = card["idShort"]
    if PREFIX_RE.match(name):
        print(f"  skip (already has ID): {name}")
        skip += 1
        continue
    new_name = f"[TRK-{short}] {name}"
    r = requests.put(f"{BASE}/cards/{card['id']}", json={
        "key": KEY, "token": TOKEN, "name": new_name
    })
    r.raise_for_status()
    print(f"  [TRK-{short}] {name}")
    ok += 1
    time.sleep(0.25)

print(f"\nDone. Updated: {ok}  Skipped: {skip}")
