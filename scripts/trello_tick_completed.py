"""
Marks checklist items as complete when the linked card is in Revision or Done.
Applies to Epic->Features and Feature->Stories checklists.
"""

import os`nimport requests, time, re, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

KEY   = os.environ["TRELLO_KEY"]
TOKEN = os.environ["TRELLO_TOKEN"]
BASE  = "https://api.trello.com/1"
BOARD = "WtpHmvMt"

DONE_LISTS = {
    "69f358c8abf71b63789dafe4",  # Revision
    "69f3150f7f2afba50da43e4c",  # Done
}

URL_RE = re.compile(r"\(https://trello\.com/c/([^/)\s]+)")


def get(path, **p):
    p.update({"key": KEY, "token": TOKEN})
    return requests.get(f"{BASE}{path}", params=p).json()


def put(path, **data):
    data.update({"key": KEY, "token": TOKEN})
    r = requests.put(f"{BASE}{path}", json=data)
    r.raise_for_status()
    return r.json()


cards  = get(f"/boards/{BOARD}/cards", filter="open", fields="id,name,idList,labels,shortLink")
trucks = [c for c in cards if any(l["name"] == "team-trucks" for l in c.get("labels", []))]
sl_map = {c["shortLink"]: c for c in trucks}

parents = [c for c in trucks if any(l["name"] in ("Epic", "Feature") for l in c.get("labels", []))]
print(f"Checking {len(parents)} Epic/Feature cards...\n")

total_ticked = 0

for parent in sorted(parents, key=lambda c: c["name"]):
    checklists = get(f"/cards/{parent['id']}/checklists")
    for cl in checklists:
        if cl["name"] not in ("Features", "Stories"):
            continue

        items     = cl["checkItems"]
        ticked    = sum(1 for i in items if i["state"] == "complete")
        newly     = 0

        for item in items:
            if item["state"] == "complete":
                continue
            m = URL_RE.search(item["name"])
            if not m:
                continue
            child = sl_map.get(m.group(1))
            if child and child["idList"] in DONE_LISTS:
                put(f"/cards/{parent['id']}/checkItem/{item['id']}", state="complete")
                ticked += 1
                newly  += 1
                total_ticked += 1
                time.sleep(0.2)

        total = len(items)
        mark  = f"  {ticked}/{total}"
        arrow = f"  (+{newly} ticked)" if newly else ""
        print(f"  {parent['name'][:60]}  {cl['name']}: {mark}{arrow}")

print(f"\nTotal newly ticked: {total_ticked}")
