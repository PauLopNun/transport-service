"""
Creates missing stories for the 8 features that have no stories,
adds them to each feature's checklist, and assigns them to their owner.
"""

import os`nimport requests, time, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

KEY   = os.environ["TRELLO_KEY"]
TOKEN = os.environ["TRELLO_TOKEN"]
BASE  = "https://api.trello.com/1"
BOARD = "WtpHmvMt"

REVISION     = "69f358c8abf71b63789dafe4"
STORIES_TODO = "69f35a95f9ba42f112d205d2"

members = requests.get(f"{BASE}/boards/{BOARD}/members",
                       params={"key": KEY, "token": TOKEN}).json()
by_user = {m["username"]: m["id"] for m in members}
IVAN  = by_user["inay"]
PAU_G = by_user["pugz2"]

labels = requests.get(f"{BASE}/boards/{BOARD}/labels",
                       params={"key": KEY, "token": TOKEN}).json()
LBL_STORY  = next(l["id"] for l in labels if l["name"] == "Story")
LBL_TRUCKS = next(l["id"] for l in labels if l["name"] == "team-trucks")


def create_card(list_id, name, desc, member_id):
    r = requests.post(f"{BASE}/cards", json={
        "key": KEY, "token": TOKEN,
        "idList": list_id,
        "name": name,
        "desc": desc,
        "idLabels": f"{LBL_STORY},{LBL_TRUCKS}",
        "idMembers": member_id,
    })
    r.raise_for_status()
    time.sleep(0.3)
    return r.json()


def add_to_checklist(feature_id, story_name, story_url):
    cls = requests.get(f"{BASE}/cards/{feature_id}/checklists",
                       params={"key": KEY, "token": TOKEN}).json()
    cl = next((c for c in cls if c["name"] == "Stories"), None)
    if cl is None:
        cl = requests.post(f"{BASE}/cards/{feature_id}/checklists",
                           json={"key": KEY, "token": TOKEN, "name": "Stories"}).json()
        time.sleep(0.2)
    requests.post(f"{BASE}/checklists/{cl['id']}/checkItems",
                  json={"key": KEY, "token": TOKEN, "name": f"[{story_name}]({story_url})"})
    time.sleep(0.2)


# ── Stories to create ─────────────────────────────────────────────────────────
# (feature_id, feature_url, feature_name, epic_url, epic_name, list_id, member, stories[])

MISSING = [
    # ── Epic 1 — Domain Model (done → Revision) ───────────────────────────────
    {
        "feature_id":   "69f31539f49a0bc5ecff9410",
        "feature_url":  "https://trello.com/c/Wjrnh7Oh/32",
        "feature_name": "TRK-32 FEATURE | Implement Truck aggregate",
        "epic_url":     "https://trello.com/c/PXUrajWp/9",
        "epic_name":    "TRK-9 EPIC | Trucks — Domain Model",
        "list_id":      REVISION,
        "member":       IVAN,
        "stories": [
            {
                "name": "STORY | Implement Truck aggregate root, value objects and TruckRepository port",
                "desc": (
                    "As team-trucks I want the Truck aggregate with TruckId, Location (record), "
                    "TruckStatus (AVAILABLE/IN_TRANSIT/DELIVERED), capacity, currentLoad, "
                    "deliveryIds, remainingCapacity() and canAccept(int) so that the domain "
                    "model is complete. Include TruckRepository interface."
                ),
            },
        ],
    },
    {
        "feature_id":   "69f31539a7047a903f32a119",
        "feature_url":  "https://trello.com/c/fMDL0MYp/33",
        "feature_name": "TRK-33 FEATURE | Implement Delivery aggregate",
        "epic_url":     "https://trello.com/c/PXUrajWp/9",
        "epic_name":    "TRK-9 EPIC | Trucks — Domain Model",
        "list_id":      REVISION,
        "member":       IVAN,
        "stories": [
            {
                "name": "STORY | Implement Delivery aggregate, DeliveryItem value object and DeliveryRepository port",
                "desc": (
                    "As team-trucks I want the Delivery aggregate with DeliveryId, DeliveryItem "
                    "(record — materialType + quantity, no ID), shipmentId, truckId, destination "
                    "(Location), items, isArrived(Location) and complete(int currentDay) so that "
                    "delivery lifecycle is fully modelled. Include DeliveryRepository interface."
                ),
            },
        ],
    },
    {
        "feature_id":   "69f3153a003a3a9eb4d2abd8",
        "feature_url":  "https://trello.com/c/EzRRIDVi/35",
        "feature_name": "TRK-35 FEATURE | Implement DistanceCalculator domain service",
        "epic_url":     "https://trello.com/c/PXUrajWp/9",
        "epic_name":    "TRK-9 EPIC | Trucks — Domain Model",
        "list_id":      REVISION,
        "member":       IVAN,
        "stories": [
            {
                "name": "STORY | Implement DistanceCalculator — Manhattan distance and isOnRoute",
                "desc": (
                    "As team-trucks I want a domain service that calculates Manhattan distance "
                    "between two Locations and checks if a point lies on the L-shaped Manhattan "
                    "path (X-leg first, then Y-leg) via isOnRoute(point, from, to) so that "
                    "OptimalTruckSelector and IN_TRANSIT fallback work correctly."
                ),
            },
        ],
    },
    {
        "feature_id":   "69f3153acf1a0aee417da71f",
        "feature_url":  "https://trello.com/c/K48MgcTf/34",
        "feature_name": "TRK-34 FEATURE | Implement OptimalTruckSelector domain service",
        "epic_url":     "https://trello.com/c/PXUrajWp/9",
        "epic_name":    "TRK-9 EPIC | Trucks — Domain Model",
        "list_id":      REVISION,
        "member":       IVAN,
        "stories": [
            {
                "name": "STORY | Implement OptimalTruckSelector — closest AVAILABLE truck and IN_TRANSIT fallback",
                "desc": (
                    "As team-trucks I want a domain service that returns the closest AVAILABLE "
                    "truck with remainingCapacity >= required items, with fallback to an "
                    "IN_TRANSIT truck that passes through the origin and has capacity, so that "
                    "every shipment request is assigned to the optimal truck."
                ),
            },
        ],
    },
    {
        "feature_id":   "69f36cc7dec50addfd2c9f07",
        "feature_url":  "https://trello.com/c/M4R6qASA/66",
        "feature_name": "TRK-66 FEATURE | Implement multi-delivery capacity validation",
        "epic_url":     "https://trello.com/c/PXUrajWp/9",
        "epic_name":    "TRK-9 EPIC | Trucks — Domain Model",
        "list_id":      REVISION,
        "member":       IVAN,
        "stories": [
            {
                "name": "STORY | Add deliveryIds list and currentLoad to Truck aggregate",
                "desc": (
                    "As team-trucks I want Truck to hold multiple deliveries via a deliveryIds "
                    "list and track currentLoad so that capacity is enforced across concurrent "
                    "shipments and remainingCapacity() reflects the real available space."
                ),
            },
            {
                "name": "STORY | Implement domain events: TruckRegisteredEvent, TruckStatusChangedEvent, TruckPositionUpdatedEvent, DeliveryCompletedEvent",
                "desc": (
                    "As team-trucks I want all four domain events implemented — "
                    "TruckRegisteredEvent, TruckStatusChangedEvent (with oldStatus, newStatus, "
                    "position, currentLoad, capacity, timestamp, reason), "
                    "TruckPositionUpdatedEvent and DeliveryCompletedEvent — so that the "
                    "application layer can publish the full set of messaging contracts."
                ),
            },
        ],
    },
    # ── Epic 3 — Messaging (pending → STORIES - To Do) ────────────────────────
    {
        "feature_id":   "69f3153d81f91b060444fc96",
        "feature_url":  "https://trello.com/c/u7ozO0aj/41",
        "feature_name": "TRK-41 FEATURE | Publish truck.position.updated.v1",
        "epic_url":     "https://trello.com/c/uXnlWPQD/11",
        "epic_name":    "TRK-11 EPIC | Trucks — Messaging",
        "list_id":      STORIES_TODO,
        "member":       PAU_G,
        "stories": [
            {
                "name": "STORY | Implement RabbitMQ publisher for truck.position.updated.v1",
                "desc": (
                    "As team-trucks I want to publish truck.position.updated.v1 "
                    "{truckId, location} via RabbitMQ on every time tick for each IN_TRANSIT "
                    "truck that has not yet arrived, so that the Map UI updates positions "
                    "in real time. Implement TruckEventPublisher port and its RabbitMQ adapter."
                ),
            },
        ],
    },
    {
        "feature_id":   "69f3153ecee1b98ac0052ca6",
        "feature_url":  "https://trello.com/c/XcUfLqbn/42",
        "feature_name": "TRK-42 FEATURE | Publish truck.status.changed.v1",
        "epic_url":     "https://trello.com/c/uXnlWPQD/11",
        "epic_name":    "TRK-11 EPIC | Trucks — Messaging",
        "list_id":      STORIES_TODO,
        "member":       PAU_G,
        "stories": [
            {
                "name": "STORY | Implement RabbitMQ publisher for truck.status.changed.v1",
                "desc": (
                    "As team-trucks I want to publish truck.status.changed.v1 with fields "
                    "truckId, oldStatus (null on registration), newStatus, position, "
                    "currentLoad, capacity, timestamp and reason "
                    "(TRUCK_REGISTERED / DISPATCHED / LOAD_UPDATED / RETURNED_TO_BASE) so that "
                    "Reporting tracks the full truck lifecycle."
                ),
            },
        ],
    },
    {
        "feature_id":   "69f3153e81baa8ecc1428b07",
        "feature_url":  "https://trello.com/c/hXocJhOX/43",
        "feature_name": "TRK-43 FEATURE | Publish delivery.completed.v1",
        "epic_url":     "https://trello.com/c/uXnlWPQD/11",
        "epic_name":    "TRK-11 EPIC | Trucks — Messaging",
        "list_id":      STORIES_TODO,
        "member":       PAU_G,
        "stories": [
            {
                "name": "STORY | Implement RabbitMQ publisher for delivery.completed.v1",
                "desc": (
                    "As team-trucks I want to publish delivery.completed.v1 "
                    "{shipmentId, truckId, items[] (Value Objects, no ID), location, completedAt} "
                    "when a truck arrives at its destination so that Warehouses, Reporting and "
                    "Map UI are notified of completed deliveries. Implement DeliveryEventPublisher "
                    "port and its RabbitMQ adapter."
                ),
            },
        ],
    },
]

# ── Create cards and wire checklists ──────────────────────────────────────────
total = 0
for feat in MISSING:
    print(f"\n{feat['feature_name']}")
    for s in feat["stories"]:
        parent_block = (
            f"\n\n---\n"
            f"**Feature:** [{feat['feature_name']}]({feat['feature_url']})\n"
            f"**Epic:** [{feat['epic_name']}]({feat['epic_url']})"
        )
        card = create_card(feat["list_id"], s["name"], s["desc"] + parent_block, feat["member"])
        print(f"  created: {card['name']} -> {card['url']}")
        add_to_checklist(feat["feature_id"], card["name"], card["url"])
        total += 1

print(f"\nDone. Created {total} stories.")
