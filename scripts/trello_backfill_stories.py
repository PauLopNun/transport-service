"""
Backfills missing stories derived from existing code for features with only 1 story.
Creates stories, assigns commits as attachments, updates GitHub PRs.
"""

import os`nimport requests, time, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

KEY   = os.environ["TRELLO_KEY"]
TOKEN = os.environ["TRELLO_TOKEN"]
GH    = os.environ.get("GITHUB_TOKEN", "")
BASE  = "https://api.trello.com/1"
REPO  = "PauLopNun/transport-service"

REVISION     = "69f358c8abf71b63789dafe4"
STORIES_TODO = "69f35a95f9ba42f112d205d2"

labels  = requests.get(f"{BASE}/boards/WtpHmvMt/labels", params={"key":KEY,"token":TOKEN}).json()
by_name = {l["name"]: l["id"] for l in labels}
LBL_STORY  = by_name["Story"]
LBL_TRUCKS = by_name["team-trucks"]

members = requests.get(f"{BASE}/boards/WtpHmvMt/members", params={"key":KEY,"token":TOKEN}).json()
by_user = {m["username"]: m["id"] for m in members}
IVAN  = by_user["inay"]
PAU_L = by_user["paulopeznunez"]
PAU_G = by_user["pugz2"]


def create_card(list_id, name, desc, member):
    r = requests.post(f"{BASE}/cards", json={
        "key":KEY, "token":TOKEN,
        "idList": list_id, "name": name, "desc": desc,
        "idLabels": f"{LBL_STORY},{LBL_TRUCKS}",
        "idMembers": member,
    })
    r.raise_for_status()
    time.sleep(0.3)
    return r.json()


def add_checklist_item(feature_id, name, url):
    cls = requests.get(f"{BASE}/cards/{feature_id}/checklists", params={"key":KEY,"token":TOKEN}).json()
    cl  = next((c for c in cls if c["name"] == "Stories"), None)
    if not cl:
        cl = requests.post(f"{BASE}/cards/{feature_id}/checklists",
                           json={"key":KEY,"token":TOKEN,"name":"Stories"}).json()
        time.sleep(0.2)
    requests.post(f"{BASE}/checklists/{cl['id']}/checkItems",
                  json={"key":KEY,"token":TOKEN,"name":f"[{name}]({url})"})
    time.sleep(0.2)


def attach_commit(card_id, sha, label):
    url = f"https://github.com/{REPO}/commit/{sha}"
    existing = requests.get(f"{BASE}/cards/{card_id}/attachments",
                            params={"key":KEY,"token":TOKEN}).json()
    if any(a.get("url") == url for a in existing):
        return False
    requests.post(f"{BASE}/cards/{card_id}/attachments",
                  json={"key":KEY,"token":TOKEN,"url":url,"name":label})
    time.sleep(0.2)
    return True


def update_pr(pr_num, trk_refs):
    headers = {"Authorization": f"Bearer {GH}", "Accept": "application/vnd.github+json"}
    pr = requests.get(f"https://api.github.com/repos/{REPO}/pulls/{pr_num}",
                      headers=headers).json()
    body = pr.get("body") or ""
    refs = " · ".join(trk_refs)
    section = f"\n\n---\n**Trello:** {refs}"
    # Remove old trello section if present, re-append updated one
    if "**Trello:**" in body:
        body = body[:body.index("\n\n---\n**Trello:**")]
    requests.patch(f"https://api.github.com/repos/{REPO}/pulls/{pr_num}",
                   headers=headers, json={"body": body.rstrip() + section})
    time.sleep(0.3)


# ─────────────────────────────────────────────────────────────────────────────
# STORIES TO CREATE
# Each entry: (feature_card_id, list_id, member, parent_name, parent_url,
#              epic_name, epic_url, story_name, story_desc, commits[(sha,label)])
# ─────────────────────────────────────────────────────────────────────────────

EPIC_DOMAIN    = ("[TRK-9] EPIC | Trucks — Domain Model",   "https://trello.com/c/PXUrajWp/9")
EPIC_USECASES  = ("[TRK-10] EPIC | Trucks — Use Cases",     "https://trello.com/c/tSygOYAx/10")
EPIC_MESSAGING = ("[TRK-11] EPIC | Trucks — Messaging",     "https://trello.com/c/uXnlWPQD/11")

NEW_STORIES = [
    # ── TRK-32: Implement Truck aggregate ────────────────────────────────────
    dict(
        feature_id   = "69f31539f49a0bc5ecff9410",
        feature_name = "[TRK-32] FEATURE | Implement Truck aggregate",
        feature_url  = "https://trello.com/c/Wjrnh7Oh/32",
        epic          = EPIC_DOMAIN,
        list_id      = REVISION,
        member       = IVAN,
        name         = "STORY | Unit tests for Truck aggregate — remainingCapacity, canAccept and speed default",
        desc         = "TruckTest covers: remainingCapacity() = capacity - currentLoad, "
                       "canAccept() boundary conditions (true/false/exact fit/zero items), "
                       "speed defaults to 1, speed can be overridden via builder.",
        commits      = [("834f8c0", "test: truck has speed field defaulting to 1")],
    ),
    dict(
        feature_id   = "69f31539f49a0bc5ecff9410",
        feature_name = "[TRK-32] FEATURE | Implement Truck aggregate",
        feature_url  = "https://trello.com/c/Wjrnh7Oh/32",
        epic          = EPIC_DOMAIN,
        list_id      = REVISION,
        member       = IVAN,
        name         = "STORY | Add speed field to Truck aggregate (default 1)",
        desc         = "Add speed field to Truck with @Builder.Default = 1. "
                       "Speed controls how many grid steps the truck advances per simulation day.",
        commits      = [("0c35c07", "feat: add speed to Truck aggregate, default 1")],
    ),
    # ── TRK-33: Implement Delivery aggregate ─────────────────────────────────
    dict(
        feature_id   = "69f31539a7047a903f32a119",
        feature_name = "[TRK-33] FEATURE | Implement Delivery aggregate",
        feature_url  = "https://trello.com/c/fMDL0MYp/33",
        epic          = EPIC_DOMAIN,
        list_id      = REVISION,
        member       = IVAN,
        name         = "STORY | Add origin field, isArrived and complete methods to Delivery aggregate",
        desc         = "Add origin (Location) to Delivery. "
                       "isArrived(truckLocation) returns true when truckLocation.equals(destination). "
                       "complete(int completedAt) returns new immutable Delivery with completedAt set, "
                       "preserving all other fields.",
        commits      = [
            ("613216a", "feat: add origin, isArrived and complete to Delivery aggregate"),
            ("0215bda", "feat: wire origin through Delivery creation"),
            ("31f867d", "feat: persist delivery origin in deliveries table"),
        ],
    ),
    dict(
        feature_id   = "69f31539a7047a903f32a119",
        feature_name = "[TRK-33] FEATURE | Implement Delivery aggregate",
        feature_url  = "https://trello.com/c/fMDL0MYp/33",
        epic          = EPIC_DOMAIN,
        list_id      = REVISION,
        member       = IVAN,
        name         = "STORY | Unit tests for Delivery aggregate — isCompleted, isArrived, complete and field preservation",
        desc         = "DeliveryTest covers: isCompleted false when completedAt null, "
                       "isCompleted true when set, isArrived true at destination, "
                       "isArrived false elsewhere, complete() sets completedAt, "
                       "complete() preserves origin/destination/shipmentId.",
        commits      = [
            ("0c5184f", "test: delivery has origin field and domain methods isArrived and complete"),
            ("fd68899", "test: delivery origin persists and restores correctly"),
        ],
    ),
    # ── TRK-35: DistanceCalculator ────────────────────────────────────────────
    dict(
        feature_id   = "69f3153a003a3a9eb4d2abd8",
        feature_name = "[TRK-35] FEATURE | Implement DistanceCalculator domain service",
        feature_url  = "https://trello.com/c/EzRRIDVi/35",
        epic          = EPIC_DOMAIN,
        list_id      = REVISION,
        member       = IVAN,
        name         = "STORY | Unit tests for DistanceCalculator — Manhattan distance and isOnRoute edge cases",
        desc         = "DistanceCalculatorTest covers: calculate() returns correct Manhattan distance, "
                       "isOnRoute() detects points on horizontal leg, vertical leg, corners, "
                       "and rejects points off the L-path.",
        commits      = [("4c5f266", "test: isOnRoute detects points on Manhattan path")],
    ),
    # ── TRK-34: OptimalTruckSelector ─────────────────────────────────────────
    dict(
        feature_id   = "69f3153acf1a0aee417da71f",
        feature_name = "[TRK-34] FEATURE | Implement OptimalTruckSelector domain service",
        feature_url  = "https://trello.com/c/K48MgcTf/34",
        epic          = EPIC_DOMAIN,
        list_id      = REVISION,
        member       = IVAN,
        name         = "STORY | Unit tests for OptimalTruckSelector — closest truck, capacity filter and NoTruckAvailableException",
        desc         = "OptimalTruckSelectorTest covers: selects closest AVAILABLE truck with enough capacity, "
                       "filters out trucks without capacity, throws NoTruckAvailableException when no truck available.",
        commits      = [("7250156", "test: OptimalTruckSelector selects closest available truck with capacity")],
    ),
    # ── TRK-36: RegisterTruck use case ────────────────────────────────────────
    dict(
        feature_id   = "69f3153bd2a3150c05a6f988",
        feature_name = "[TRK-36] FEATURE | Implement RegisterTruck use case",
        feature_url  = "https://trello.com/c/XiixEwUN/36",
        epic          = EPIC_USECASES,
        list_id      = REVISION,
        member       = PAU_L,
        name         = "STORY | Implement GET /trucks — GetTrucks use case and TruckController endpoint",
        desc         = "GetTrucks use case retrieves all trucks from TruckRepository and maps them to TruckResponse. "
                       "TruckController exposes GET /trucks returning the full fleet with current location and status.",
        commits      = [
            ("ab7e25f", "feat: implement GetTrucks use case"),
            ("b42440d", "test: GetTrucks returns mapped truck responses from repository"),
        ],
    ),
    dict(
        feature_id   = "69f3153bd2a3150c05a6f988",
        feature_name = "[TRK-36] FEATURE | Implement RegisterTruck use case",
        feature_url  = "https://trello.com/c/XiixEwUN/36",
        epic          = EPIC_USECASES,
        list_id      = REVISION,
        member       = PAU_L,
        name         = "STORY | Define TruckEventPublisher and DeliveryEventPublisher output port interfaces",
        desc         = "Define the two output port interfaces (hexagonal architecture): "
                       "TruckEventPublisher.publish(TruckRegisteredEvent) and publish(TruckStatusChangedEvent), "
                       "DeliveryEventPublisher.publish(DeliveryCompletedEvent). "
                       "Implementations are pending Epic 3 (Pau Greus).",
        commits      = [("c90025b", "feat: define TruckEventPublisher and DeliveryEventPublisher output ports")],
    ),
    # ── TRK-39: Consume shipment.requested.v1 ────────────────────────────────
    dict(
        feature_id   = "69f3153ca6b541a2afb27b0a",
        feature_name = "[TRK-39] FEATURE | Consume shipment.requested.v1 — trucks",
        feature_url  = "https://trello.com/c/IUaXe8AT/39",
        epic          = EPIC_MESSAGING,
        list_id      = STORIES_TODO,
        member       = PAU_G,
        name         = "STORY | Implement Location resolver — map originId/destinationId strings to coordinates",
        desc         = "DispatchRequestedListener receives originId and destinationId as string IDs "
                       "(e.g. 'warehouse-north-01'). Implement a resolver that maps them to Location "
                       "coordinates before calling AssignTruck. "
                       "Agree coordinate mapping with team-warehouses.",
        commits      = [],
    ),
    dict(
        feature_id   = "69f3153ca6b541a2afb27b0a",
        feature_name = "[TRK-39] FEATURE | Consume shipment.requested.v1 — trucks",
        feature_url  = "https://trello.com/c/IUaXe8AT/39",
        epic          = EPIC_MESSAGING,
        list_id      = STORIES_TODO,
        member       = PAU_G,
        name         = "STORY | Integration test DispatchRequestedListenerIT with Testcontainers",
        desc         = "DispatchRequestedListenerIT with Testcontainers (RabbitMQ + PostgreSQL): "
                       "publish shipment.requested.v1, assert a Delivery is persisted and "
                       "truck.status.changed.v1 with reason DISPATCHED is published.",
        commits      = [],
    ),
    # ── TRK-40: Consume time.advanced.v1 ─────────────────────────────────────
    dict(
        feature_id   = "69f3153c7f97f56ba0ce5f44",
        feature_name = "[TRK-40] FEATURE | Consume time.advanced.v1 — trucks",
        feature_url  = "https://trello.com/c/eGeIWXVy/40",
        epic          = EPIC_MESSAGING,
        list_id      = STORIES_TODO,
        member       = PAU_G,
        name         = "STORY | Integration test TimeAdvancedListenerIT with Testcontainers",
        desc         = "TimeAdvancedListenerIT with Testcontainers (RabbitMQ + PostgreSQL): "
                       "persist an IN_TRANSIT truck, publish time.advanced.v1, assert truck "
                       "advances toward destination and truck.position.updated.v1 is published.",
        commits      = [],
    ),
]


# ── Run ───────────────────────────────────────────────────────────────────────
created_cards = []

for s in NEW_STORIES:
    epic_name, epic_url = s["epic"]
    parent_block = (
        f"\n\n---\n"
        f"**Feature:** [{s['feature_name']}]({s['feature_url']})\n"
        f"**Epic:** [{epic_name}]({epic_url})"
    )
    card = create_card(s["list_id"], s["name"], s["desc"] + parent_block, s["member"])
    print(f"  created: {card['name'][:70]}")
    print(f"           {card['url']}")

    add_checklist_item(s["feature_id"], card["name"], card["url"])

    for sha, label in s["commits"]:
        attach_commit(card["id"], sha, label)

    created_cards.append(card)

print(f"\n  Created {len(created_cards)} stories\n")


# ── Add TRK-N IDs ─────────────────────────────────────────────────────────────
import re
PREFIX_RE = re.compile(r"^\[TRK-\d+\]")
EMOJI_RE  = re.compile(r"[^\x00-\x7FÀ-ɏ–—‘’“”]+")

print("Adding TRK-N IDs to new stories...")
for card in created_cards:
    if PREFIX_RE.match(card["name"]):
        continue
    fresh = requests.get(f"{BASE}/cards/{card['id']}",
                         params={"key":KEY,"token":TOKEN,"fields":"idShort,name"}).json()
    new_name = f"[TRK-{fresh['idShort']}] {fresh['name']}"
    requests.put(f"{BASE}/cards/{card['id']}",
                 json={"key":KEY,"token":TOKEN,"name":new_name})
    print(f"  [TRK-{fresh['idShort']}] {fresh['name'][:60]}")
    time.sleep(0.25)


# ── Update GitHub PRs ─────────────────────────────────────────────────────────
print("\nUpdating GitHub PRs...")
headers = {"Authorization": f"Bearer {GH}", "Accept": "application/vnd.github+json"}

# PR #10 core domain model → TRK-32, TRK-33, TRK-34, TRK-35
# PR #11 trucks use cases  → TRK-36
PR_UPDATES = {
    10: [
        "[TRK-32](https://trello.com/c/Wjrnh7Oh/32)",
        "[TRK-33](https://trello.com/c/fMDL0MYp/33)",
        "[TRK-34](https://trello.com/c/K48MgcTf/34)",
        "[TRK-35](https://trello.com/c/EzRRIDVi/35)",
        "[TRK-66](https://trello.com/c/M4R6qASA/66)",
    ],
    11: [
        "[TRK-36](https://trello.com/c/XiixEwUN/36)",
        "[TRK-37](https://trello.com/c/VwuSN09r/37)",
        "[TRK-38](https://trello.com/c/sSo5Aw15/38)",
    ],
}

for pr_num, refs in PR_UPDATES.items():
    pr = requests.get(f"https://api.github.com/repos/{REPO}/pulls/{pr_num}",
                      headers=headers).json()
    body = pr.get("body") or ""
    if "**Trello:**" in body:
        body = body[:body.index("\n\n---\n**Trello:**")]
    section = "\n\n---\n**Trello:** " + " · ".join(refs)
    r = requests.patch(f"https://api.github.com/repos/{REPO}/pulls/{pr_num}",
                       headers=headers, json={"body": body.rstrip() + section})
    status = "updated" if r.status_code == 200 else f"ERR {r.status_code}"
    print(f"  {status}: PR #{pr_num}")
    time.sleep(0.3)

print("\nAll done.")
