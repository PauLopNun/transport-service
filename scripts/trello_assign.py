"""
Assigns each Epic, Feature and Story to its owner based on EPICS.md.
Epic 1 (Domain Model)          -> @inay
Epic 2 (Use Cases)             -> @paulopeznunez
Epic 3 (Messaging & Infra)     -> @pugz2
"""

import os`nimport requests, time, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

KEY   = os.environ["TRELLO_KEY"]
TOKEN = os.environ["TRELLO_TOKEN"]
BASE  = "https://api.trello.com/1"
BOARD = "WtpHmvMt"

# ── Resolve usernames to member IDs ──────────────────────────────────────────
members = requests.get(f"{BASE}/boards/{BOARD}/members",
                       params={"key": KEY, "token": TOKEN}).json()
by_username = {m["username"]: m["id"] for m in members}
print("Board members found:", list(by_username.keys()))

IVAN  = by_username.get("inay")
PAU_L = by_username.get("paulopeznunez")
PAU_G = by_username.get("pugz2")

for name, mid in [("inay", IVAN), ("paulopeznunez", PAU_L), ("pugz2", PAU_G)]:
    if not mid:
        print(f"  WARNING: @{name} not found on board — skipping their cards")

# ── Assignment map: card_id -> member_id ─────────────────────────────────────
ASSIGNMENTS = {}

if IVAN:
    for cid in [
        "69f3152ef2fd6d094fd5da32",  # EPIC | Trucks — Domain Model
        "69f31539f49a0bc5ecff9410",  # Implement Truck aggregate
        "69f31539a7047a903f32a119",  # Implement Delivery aggregate
        "69f3153a003a3a9eb4d2abd8",  # DistanceCalculator
        "69f3153acf1a0aee417da71f",  # OptimalTruckSelector
        "69f36cc7dec50addfd2c9f07",  # multi-delivery capacity
    ]:
        ASSIGNMENTS[cid] = IVAN

if PAU_L:
    for cid in [
        "69f3152e22590b71a9b97f80",  # EPIC | Trucks — Use Cases
        "69f3153bd2a3150c05a6f988",  # RegisterTruck use case
        "69f3153b98f2dadefc0b9a3b",  # AssignTruck use case
        "69f3153bfc6c5d4596fd130f",  # AdvanceTrucks use case
        "69f31545ad117011a062df1e",  # STORY | Register a new truck via REST
        "69f3154502d8b926d21489f1",  # STORY | Assign closest available truck
        "69f36cc8bbd42948e15acdd7",  # STORY | Assign additional IN_TRANSIT
        "69f31546c074c280ba98e29b",  # STORY | Move trucks forward on each tick
        "69f315469a32448e5c879f60",  # STORY | Publish position update
        "69f3154623398b9cf9e579e2",  # STORY | Confirm delivery and return
        "69f36cc8b96514ff7025ddc5",  # STORY | Keep truck IN_TRANSIT partial
        "69f36cc8feb4b64f06cf0684",  # STORY | Return truck to AVAILABLE
    ]:
        ASSIGNMENTS[cid] = PAU_L

if PAU_G:
    for cid in [
        "69f3152e80cd2059b907a5fd",  # EPIC | Trucks — Messaging
        "69f3153ca6b541a2afb27b0a",  # Consume shipment.requested.v1
        "69f3153c7f97f56ba0ce5f44",  # Consume simulation.time.tick
        "69f3153d81f91b060444fc96",  # Publish truck.position.updated.v1
        "69f3153e81baa8ecc1428b07",  # Publish delivery.completed.v1
        "69f3153ecee1b98ac0052ca6",  # Publish truck.status.changed.v1
        "69fa03ecd3910335fab18843",  # Persistence & DB
        "69fb00c149c23f11e7ef3c93",  # STORY | DispatchRequestedListener
        "69fb00c94141fab2d89d3701",  # STORY | TimeAdvancedListener
        "69faf2bc540767a451bdfbba",  # STORY | RabbitMQ Config
        "69faf2911a411945a5980438",  # STORY | TDD — Persistence tests
        "69faf29b052c11dbc42949d7",  # STORY | Base configuration
        "69faf2a79a6b36ebc667940d",  # STORY | JPA Adapter — Trucks
    ]:
        ASSIGNMENTS[cid] = PAU_G

# ── Apply assignments ─────────────────────────────────────────────────────────
print(f"\nAssigning {len(ASSIGNMENTS)} cards...")
ok = skip = err = 0
for card_id, member_id in ASSIGNMENTS.items():
    card = requests.get(f"{BASE}/cards/{card_id}",
                        params={"key": KEY, "token": TOKEN, "fields": "name,idMembers"}).json()
    if member_id in card.get("idMembers", []):
        print(f"  skip (already assigned): {card['name']}")
        skip += 1
        continue
    r = requests.post(f"{BASE}/cards/{card_id}/idMembers",
                      json={"key": KEY, "token": TOKEN, "value": member_id})
    if r.status_code == 200:
        print(f"  assigned: {card['name']}")
        ok += 1
    else:
        print(f"  ERROR {r.status_code}: {card['name']} — {r.text}")
        err += 1
    time.sleep(0.25)

print(f"\nDone. Assigned: {ok}  Skipped: {skip}  Errors: {err}")
