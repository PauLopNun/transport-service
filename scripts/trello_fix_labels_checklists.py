"""
Fix 1: Each card keeps exactly ONE type label (Epic/Feature/Story) + team-trucks.
Fix 2: Update checklist item text on Epic and Feature cards to match current card names.
"""

import os`nimport requests, time, re, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

KEY   = os.environ["TRELLO_KEY"]
TOKEN = os.environ["TRELLO_TOKEN"]
BASE  = "https://api.trello.com/1"
BOARD = "WtpHmvMt"

def get(path, **p):
    p.update({"key": KEY, "token": TOKEN})
    return requests.get(f"{BASE}{path}", params=p).json()

def put(path, **data):
    data.update({"key": KEY, "token": TOKEN})
    r = requests.put(f"{BASE}{path}", json=data)
    r.raise_for_status()
    return r.json()

# ── Fetch labels ──────────────────────────────────────────────────────────────
all_labels = get(f"/boards/{BOARD}/labels")
by_name    = {l["name"]: l["id"] for l in all_labels}
LBL_EPIC    = by_name["Epic"]
LBL_FEATURE = by_name["Feature"]
LBL_STORY   = by_name["Story"]
LBL_TRUCKS  = by_name["team-trucks"]
TYPE_LABELS = {LBL_EPIC, LBL_FEATURE, LBL_STORY}

# ── Fetch all team-trucks cards ───────────────────────────────────────────────
cards = get(f"/boards/{BOARD}/cards", filter="open",
            fields="id,name,idShort,labels,shortUrl")
trucks = [c for c in cards
          if any(l["name"] == "team-trucks" for l in c.get("labels", []))]

# Build id → current name map (for checklist updates)
id_to_name = {c["id"]: c["name"] for c in trucks}
# Also index by shortLink (last part of shortUrl)
shortlink_to = {}
for c in trucks:
    sl = c.get("shortUrl", "").split("/")[-1]
    shortlink_to[sl] = c

print(f"=== FIX 1: Labels ({len(trucks)} cards) ===\n")

for card in sorted(trucks, key=lambda c: c["idShort"]):
    name = card["name"]
    current_label_ids = [l["id"] for l in card.get("labels", [])]

    # Detect correct type from name
    upper = name.upper()
    if "EPIC |" in upper:
        correct_type = LBL_EPIC
    elif "FEATURE |" in upper:
        correct_type = LBL_FEATURE
    else:
        correct_type = LBL_STORY

    # Build desired set: correct type + team-trucks only
    desired = {correct_type, LBL_TRUCKS}
    current_set = set(current_label_ids)

    if current_set == desired:
        print(f"  ok:    {name}")
        continue

    put(f"/cards/{card['id']}", idLabels=",".join(desired))
    removed = [l["name"] for l in card["labels"] if l["id"] in (current_set - desired)]
    added   = []
    if correct_type not in current_set:
        added.append(next(l["name"] for l in all_labels if l["id"] == correct_type))
    print(f"  fixed: {name}")
    if removed: print(f"         removed labels: {removed}")
    if added:   print(f"         added labels:   {added}")
    time.sleep(0.25)

# ── FIX 2: Update checklist item text on parent cards ─────────────────────────
print(f"\n=== FIX 2: Checklist item names ===\n")

# Cards that have checklists pointing to children
parent_ids = [c["id"] for c in trucks
              if any(l["name"] in ("Epic", "Feature") for l in c.get("labels", []))]

URL_RE = re.compile(r"\(https://trello\.com/c/([^/]+)/")

for parent_id in parent_ids:
    checklists = get(f"/cards/{parent_id}/checklists")
    for cl in checklists:
        if cl["name"] not in ("Features", "Stories"):
            continue
        for item in cl.get("checkItems", []):
            old_text = item["name"]
            m = URL_RE.search(old_text)
            if not m:
                continue
            shortlink = m.group(1)
            child = shortlink_to.get(shortlink)
            if not child:
                continue
            new_text = f"[{child['name']}]({child['shortUrl']})"
            if old_text == new_text:
                continue
            put(f"/cards/{parent_id}/checkItem/{item['id']}", name=new_text)
            print(f"  updated: {old_text[:60]}")
            print(f"       ->  {new_text[:60]}")
            time.sleep(0.2)

print("\nDone.")
