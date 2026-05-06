"""
1. Removes all emojis and "EPIC |" / "FEATURE |" / "STORY |" prefixes from names.
2. Ensures every card has its type label (Epic / Feature / Story) + team-trucks.
3. Fixes Epic label color to black so it doesn't clash with team-trucks purple.
"""

import os`nimport requests, time, re, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

KEY   = os.environ["TRELLO_KEY"]
TOKEN = os.environ["TRELLO_TOKEN"]
BASE  = "https://api.trello.com/1"
BOARD = "WtpHmvMt"

# ── Labels ────────────────────────────────────────────────────────────────────
all_labels = requests.get(f"{BASE}/boards/{BOARD}/labels",
                          params={"key": KEY, "token": TOKEN}).json()
by_name = {l["name"]: l for l in all_labels}

LBL_TRUCKS  = by_name["team-trucks"]["id"]
LBL_EPIC    = by_name["Epic"]["id"]
LBL_FEATURE = by_name["Feature"]["id"]
LBL_STORY   = by_name["Story"]["id"]

# Fix Epic color to black (distinct from team-trucks purple)
requests.put(f"{BASE}/labels/{LBL_EPIC}",
             json={"key": KEY, "token": TOKEN, "color": "black"})
print("Epic label color -> black")

# Fix Story color to sky (light blue)
requests.put(f"{BASE}/labels/{LBL_STORY}",
             json={"key": KEY, "token": TOKEN, "color": "sky"})
print("Story label color -> sky")
time.sleep(0.3)

# ── Cards ─────────────────────────────────────────────────────────────────────
cards = requests.get(f"{BASE}/boards/{BOARD}/cards", params={
    "key": KEY, "token": TOKEN, "filter": "open",
    "fields": "id,name,idShort,labels",
}).json()

trucks = [c for c in cards
          if any(l["name"] == "team-trucks" for l in c.get("labels", []))]
print(f"\nProcessing {len(trucks)} team-trucks cards...\n")

# Regex to strip emojis and type prefixes
EMOJI_RE = re.compile(
    "["
    "\U0001F300-\U0001F9FF"   # misc symbols, emoticons
    "\U00002600-\U000027BF"   # misc symbols
    "\U0001F004-\U0001F0CF"
    "]+",
    flags=re.UNICODE,
)
PREFIX_RE = re.compile(r"^\s*(EPIC|FEATURE|STORY)\s*\|\s*", re.IGNORECASE)
JUNK_RE   = re.compile(r"^\s*[▎▮■]\s*")  # ▎ ▮ ■ leftovers


def clean_name(raw):
    # Remove emojis only — keep "EPIC | " / "FEATURE | " / "STORY | " text
    s = EMOJI_RE.sub("", raw)
    s = JUNK_RE.sub("", s)
    # Collapse multiple spaces that emojis may leave behind
    s = re.sub(r"  +", " ", s)
    return s.strip()


def card_type(raw_name):
    """Detect type from the original name before cleaning."""
    upper = raw_name.upper()
    if "EPIC" in upper and "|" in upper:
        return "epic"
    if "FEATURE" in upper and "|" in upper:
        return "feature"
    return "story"


TYPE_LABEL = {"epic": LBL_EPIC, "feature": LBL_FEATURE, "story": LBL_STORY}

for card in sorted(trucks, key=lambda c: c["idShort"]):
    raw  = card["name"]
    kind = card_type(raw)
    new_name = clean_name(raw)

    current_label_ids = {l["id"] for l in card.get("labels", [])}
    needed = {LBL_TRUCKS, TYPE_LABEL[kind]}
    missing_labels = needed - current_label_ids

    name_changed  = new_name != raw
    label_changed = bool(missing_labels)

    if not name_changed and not label_changed:
        print(f"  skip: {new_name}")
        continue

    payload = {"key": KEY, "token": TOKEN}
    if name_changed:
        payload["name"] = new_name
    if label_changed:
        payload["idLabels"] = ",".join(current_label_ids | needed)

    r = requests.put(f"{BASE}/cards/{card['id']}", json=payload)
    r.raise_for_status()
    tag = f"[{kind}]"
    print(f"  {tag:10} {new_name}")
    time.sleep(0.25)

print("\nDone.")
