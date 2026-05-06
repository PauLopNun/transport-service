"""
Adds EPIC | / FEATURE | / STORY | prefix back to card names,
detected from the type label already on each card.
"""

import os`nimport requests, time, re, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

KEY   = os.environ["TRELLO_KEY"]
TOKEN = os.environ["TRELLO_TOKEN"]
BASE  = "https://api.trello.com/1"
BOARD = "WtpHmvMt"

cards = requests.get(f"{BASE}/boards/{BOARD}/cards", params={
    "key": KEY, "token": TOKEN, "filter": "open",
    "fields": "id,name,idShort,labels",
}).json()

trucks = [c for c in cards
          if any(l["name"] == "team-trucks" for l in c.get("labels", []))]

PREFIX_RE = re.compile(r"^(\[TRK-\d+\])\s*(EPIC|FEATURE|STORY)\s*\|\s*", re.IGNORECASE)

for card in sorted(trucks, key=lambda c: c["idShort"]):
    name       = card["name"]
    label_names = {l["name"] for l in card.get("labels", [])}

    # Detect type from label
    if "Epic" in label_names:
        kind = "EPIC"
    elif "Feature" in label_names:
        kind = "FEATURE"
    else:
        kind = "STORY"

    # Already has the right prefix?
    if PREFIX_RE.match(name):
        print(f"  skip: {name}")
        continue

    # Insert prefix after [TRK-N]
    trk_match = re.match(r"^(\[TRK-\d+\])\s*", name)
    if trk_match:
        new_name = f"{trk_match.group(1)} {kind} | {name[trk_match.end():].strip()}"
    else:
        new_name = f"{kind} | {name.strip()}"

    r = requests.put(f"{BASE}/cards/{card['id']}",
                     json={"key": KEY, "token": TOKEN, "name": new_name})
    r.raise_for_status()
    print(f"  {new_name}")
    time.sleep(0.25)

print("\nDone.")
