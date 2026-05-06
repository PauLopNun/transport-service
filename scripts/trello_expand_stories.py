"""
1. Archives the single-story placeholders for TRK-41, 42, 43.
2. Creates expanded stories for each publisher feature.
3. Adds GitHub commit links as attachments to the relevant Trello cards.
4. (Optional) Updates GitHub PR descriptions with Trello links — requires --github-token.

Usage:
  py trello_expand_stories.py
  py trello_expand_stories.py --github-token ghp_xxxx
"""

import argparse, requests, time, sys, io, re
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

KEY   = "a5df122d6183ebfcaa99c219ae595533"
TOKEN = "ATTAe6296912f9210452b6bbe0c2a7a6a42b7032fb6a0b285886e499d3358740654909B2BE80"
BASE  = "https://api.trello.com/1"
BOARD = "WtpHmvMt"
REPO  = "PauLopNun/transport-service"

STORIES_TODO = "69f35a95f9ba42f112d205d2"

def tget(path, **p):
    p.update({"key": KEY, "token": TOKEN})
    return requests.get(f"{BASE}{path}", params=p).json()

def tpost(path, **data):
    data.update({"key": KEY, "token": TOKEN})
    r = requests.post(f"{BASE}{path}", json=data)
    r.raise_for_status(); return r.json()

def tput(path, **data):
    data.update({"key": KEY, "token": TOKEN})
    r = requests.put(f"{BASE}{path}", json=data)
    r.raise_for_status(); return r.json()

labels = tget(f"/boards/{BOARD}/labels")
by_name = {l["name"]: l["id"] for l in labels}
LBL_STORY  = by_name["Story"]
LBL_TRUCKS = by_name["team-trucks"]

members = tget(f"/boards/{BOARD}/members")
PAU_G = next(m["id"] for m in members if m["username"] == "pugz2")


# ── helpers ───────────────────────────────────────────────────────────────────

def create_story(name, desc, feature_id, feature_name, feature_url, epic_url, epic_name):
    parent = (
        f"\n\n---\n"
        f"**Feature:** [{feature_name}]({feature_url})\n"
        f"**Epic:** [{epic_name}]({epic_url})"
    )
    card = tpost("/cards",
        idList=STORIES_TODO,
        name=name, desc=desc + parent,
        idLabels=f"{LBL_STORY},{LBL_TRUCKS}",
        idMembers=PAU_G,
    )
    time.sleep(0.3)
    return card


def add_to_checklist(feature_id, story_name, story_url):
    cls = tget(f"/cards/{feature_id}/checklists")
    cl  = next((c for c in cls if c["name"] == "Stories"), None)
    if not cl:
        cl = tpost(f"/cards/{feature_id}/checklists", name="Stories")
        time.sleep(0.2)
    existing = {i["name"] for i in cl.get("checkItems", [])}
    if not any(story_url in e for e in existing):
        tpost(f"/checklists/{cl['id']}/checkItems",
              name=f"[{story_name}]({story_url})")
        time.sleep(0.2)


def add_commit_attachment(card_id, sha, label):
    url = f"https://github.com/{REPO}/commit/{sha}"
    existing = tget(f"/cards/{card_id}/attachments")
    if any(a.get("url") == url for a in existing):
        return False
    tpost(f"/cards/{card_id}/attachments", url=url, name=label)
    time.sleep(0.2)
    return True


# ── Step 1: Archive old single-story placeholders ────────────────────────────
print("=== Step 1: Archive old placeholder stories ===\n")
OLD = {
    "69fb11967d0e2f82359e9f7d": "TRK-195 (old single story for TRK-41)",
    "69fb119b393aff2348e0fd87": "TRK-196 (old single story for TRK-42)",
    "69fb119fd1d60466412896cb": "TRK-197 (old single story for TRK-43)",
}
for cid, label in OLD.items():
    tput(f"/cards/{cid}", closed=True)
    print(f"  archived: {label}")
    time.sleep(0.2)


# ── Step 2: Expand TRK-41 — Publish truck.position.updated.v1 ────────────────
print("\n=== Step 2: Expand TRK-41 stories ===\n")

F41_ID   = "69f3153d81f91b060444fc96"
F41_URL  = "https://trello.com/c/u7ozO0aj/41"
F41_NAME = "[TRK-41] FEATURE | Publish truck.position.updated.v1"
E11_URL  = "https://trello.com/c/uXnlWPQD/11"
E11_NAME = "[TRK-11] EPIC | Trucks — Messaging"

stories_41 = [
    ("STORY | Create TruckPositionUpdatedMessage DTO",
     "The JSON object that gets serialized and sent to RabbitMQ. "
     "Payload: `{ truckId: UUID, location: { x: int, y: int } }`. No timestamp, no business data."),
    ("STORY | Implement RabbitMQTruckEventPublisher — publishPositionUpdated",
     "RabbitMQ publisher that sends the truck position updated event. "
     "Builds TruckPositionUpdatedMessage from the Truck aggregate and publishes to the "
     "truck.position.updated.v1 routing key."),
    ("STORY | Connect publisher to AdvanceTrucks — Publish position on each tick for IN_TRANSIT trucks",
     "Call publishPositionUpdated for every IN_TRANSIT truck that has not yet arrived on each time tick. "
     "Unit test verifies the publisher is called the correct number of times."),
    ("STORY | Integration test with Testcontainers RabbitMQ — truck.position.updated.v1",
     "End-to-end test verifying messages actually reach the RabbitMQ exchange. "
     "Publish a time tick, assert truck.position.updated.v1 is received with correct payload "
     "for each IN_TRANSIT truck."),
]

for name, desc in stories_41:
    card = create_story(name, desc, F41_ID, F41_NAME, F41_URL, E11_URL, E11_NAME)
    add_to_checklist(F41_ID, card["name"], card["url"])
    print(f"  created: {card['name']}")


# ── Step 3: Expand TRK-42 — Publish truck.status.changed.v1 ──────────────────
print("\n=== Step 3: Expand TRK-42 stories ===\n")

F42_ID   = "69f3153ecee1b98ac0052ca6"
F42_URL  = "https://trello.com/c/XcUfLqbn/42"
F42_NAME = "[TRK-42] FEATURE | Publish truck.status.changed.v1"

stories_42 = [
    ("STORY | Create TruckStatusChangedMessage DTO",
     "The JSON object that gets serialized and sent to RabbitMQ. "
     "Payload: `{ truckId, oldStatus (null on registration), newStatus, position: {x,y}, "
     "currentLoad, capacity, timestamp: int, reason: string }`."),
    ("STORY | Implement RabbitMQTruckEventPublisher — truck status changed event",
     "RabbitMQ publisher that sends the truck status changed event. "
     "Implement publishStatusChanged(Truck, oldStatus, reason) — builds the DTO and publishes "
     "to the truck.status.changed.v1 routing key."),
    ("STORY | Connect publisher to RegisterTruck — reason TRUCK_REGISTERED",
     "Publish event with reason TRUCK_REGISTERED when a truck is registered. "
     "oldStatus=null. Unit test verifies publisher is called with correct arguments."),
    ("STORY | Connect publisher to AssignTruck — reason DISPATCHED and LOAD_UPDATED",
     "Publish event with reason DISPATCHED and LOAD_UPDATED when a truck is assigned. "
     "DISPATCHED for AVAILABLE→IN_TRANSIT, LOAD_UPDATED for IN_TRANSIT fallback. "
     "Unit tests cover both paths."),
    ("STORY | Connect publisher to AdvanceTrucks — reason RETURNED_TO_BASE",
     "Publish event with reason RETURNED_TO_BASE when a truck returns to base. "
     "Called after all deliveries completed and truck transitions back to AVAILABLE. "
     "Unit test verifies the call."),
    ("STORY | Integration test with Testcontainers RabbitMQ — truck.status.changed.v1",
     "End-to-end test verifying messages actually reach the RabbitMQ exchange. "
     "Trigger register, assign and advance flows — assert truck.status.changed.v1 is received "
     "for each reason (TRUCK_REGISTERED, DISPATCHED, LOAD_UPDATED, RETURNED_TO_BASE) "
     "with correct payload."),
]

for name, desc in stories_42:
    card = create_story(name, desc, F42_ID, F42_NAME, F42_URL, E11_URL, E11_NAME)
    add_to_checklist(F42_ID, card["name"], card["url"])
    print(f"  created: {card['name']}")


# ── Step 4: Expand TRK-43 — Publish delivery.completed.v1 ────────────────────
print("\n=== Step 4: Expand TRK-43 stories ===\n")

F43_ID   = "69f3153e81baa8ecc1428b07"
F43_URL  = "https://trello.com/c/hXocJhOX/43"
F43_NAME = "[TRK-43] FEATURE | Publish delivery.completed.v1"

stories_43 = [
    ("STORY | Create DeliveryCompletedMessage DTO",
     "The JSON object that gets serialized and sent to RabbitMQ. "
     "Payload: `{ shipmentId: UUID, truckId: UUID, items: [{ materialType, quantity }], "
     "location: { x, y }, completedAt: int }`. "
     "items are Value Objects — no ID, serialized as plain objects."),
    ("STORY | Implement RabbitMQDeliveryEventPublisher — delivery completed event",
     "RabbitMQ publisher that sends the delivery completed event. "
     "Implement publishDeliveryCompleted(Delivery, Location) — builds the DTO from the "
     "Delivery aggregate and publishes to the delivery.completed.v1 routing key."),
    ("STORY | Connect publisher to AdvanceTrucks — Publish when truck arrives at destination",
     "When isArrived() returns true, call publishDeliveryCompleted after delivery.complete(). "
     "Unit test verifies publisher is called with correct shipmentId, truckId, items and location."),
    ("STORY | Integration test with Testcontainers RabbitMQ — delivery.completed.v1",
     "End-to-end test verifying messages actually reach the RabbitMQ exchange. "
     "Advance truck to destination — assert delivery.completed.v1 is received with correct "
     "payload including items[] serialized as Value Objects (no ID)."),
]

for name, desc in stories_43:
    card = create_story(name, desc, F43_ID, F43_NAME, F43_URL, E11_URL, E11_NAME)
    add_to_checklist(F43_ID, card["name"], card["url"])
    print(f"  created: {card['name']}")


# ── Step 5: Add GitHub commit links to Trello cards ───────────────────────────
print("\n=== Step 5: Attach GitHub commits to Trello cards ===\n")

COMMIT_MAP = [
    # card_id, [(sha, label), ...]
    ("69faf2ba156e53de578cf572", [  # TRK-150 RabbitMQ Config
        ("6c8f114", "feat: RabbitMQConfig"),
    ]),
    ("69faf2a79a6b36ebc667940d", [  # TRK-148 JPA Adapter
        ("c7b7493", "feat: TruckEntity + TruckJpaRepository + TruckRepositoryAdapter"),
    ]),
    ("69faf29b052c11dbc42949d7", [  # TRK-147 Base config
        ("de346a9", "feat: application.yml + LiquiBase changesets"),
    ]),
    ("69faf2911a411945a5980438", [  # TRK-146 TDD tests
        ("51d23aa", "test: add truckRepositoryAdapterIT and RabbitMQConfigTest (RED)"),
        ("a581e60", "refactor: remove RabbitMQConfigTest"),
        ("b3e1cad", "fix: set ddl-auto to none for Liquibase compatibility"),
    ]),
    ("69f3153b98f2dadefc0b9a3b", [  # TRK-37 AssignTruck feature
        ("970b98d", "feat: implement AssignTruck use case"),
        ("5dbd7bc", "test: AssignTruck assigns closest available truck"),
    ]),
    ("69f3154502d8b926d21489f1", [  # TRK-57 Assign closest truck
        ("970b98d", "feat: implement AssignTruck use case"),
        ("5dbd7bc", "test: AssignTruck assigns closest available truck"),
    ]),
    ("69f36cc8bbd42948e15acdd7", [  # TRK-67 Assign IN_TRANSIT fallback
        ("1eaa867", "feat: assign shipment to IN_TRANSIT truck"),
        ("6f82707", "test: AssignTruck falls back to IN_TRANSIT truck"),
    ]),
    ("69fb1193cf0c69a7a7c605be", [  # TRK-194 Domain events
        ("bbf2399", "feat: complete domain model and align events to contracts"),
        ("b32fd3c", "feat: event timestamps are int simulation day"),
        ("99b21ea", "test: TruckRegisteredEvent timestamp must be int"),
        ("559d1ed", "feat: TruckStatusChangedEvent timestamp is int"),
        ("c6cd81d", "test: TruckStatusChangedEvent timestamp must be int"),
        ("f4b27a4", "feat: align TruckPositionUpdatedEvent to contract"),
        ("f91fe8e", "test: verify TruckPositionUpdatedEvent payload"),
    ]),
    ("69fb11817f07b88016d14ae9", [  # TRK-190 Delivery aggregate
        ("613216a", "feat: add origin, isArrived and complete to Delivery aggregate"),
        ("0c5184f", "test: delivery has origin field and domain methods"),
        ("0215bda", "feat: wire origin through Delivery creation"),
        ("31f867d", "feat: persist delivery origin in deliveries table"),
        ("fd68899", "test: delivery origin persists and restores correctly"),
    ]),
    ("69fb11866b5725b495bac620", [  # TRK-191 DistanceCalculator
        ("60b89dc", "feat: add isOnRoute to DistanceCalculator"),
        ("4c5f266", "test: isOnRoute detects points on Manhattan path"),
    ]),
    ("69fb118a09c9b520bd6ababb", [  # TRK-192 OptimalTruckSelector
        ("12cbcf0", "feat: implement OptimalTruckSelector and NoTruckAvailableException"),
        ("7250156", "test: OptimalTruckSelector selects closest available truck"),
    ]),
    ("69fb117cfc3224eb7c3f2bbe", [  # TRK-189 Truck aggregate
        ("0c35c07", "feat: add speed to Truck aggregate, default 1"),
        ("834f8c0", "test: truck has speed field defaulting to 1"),
    ]),
    ("69fb118e7ea03d6cf34f75cc", [  # TRK-193 deliveryIds / currentLoad
        ("4939d15", "feat: add missing contract fields to DeliveryCompletedEvent"),
        ("bbf2399", "feat: complete domain model and align events to contracts"),
    ]),
    ("69f3153bd2a3150c05a6f988", [  # TRK-36 RegisterTruck feature
        ("7acec1e", "feat: implement RegisterTruck use case"),
        ("a60a95b", "feat: add CreateTruckRequest and TruckResponse DTOs"),
        ("c90025b", "feat: define TruckEventPublisher and DeliveryEventPublisher ports"),
    ]),
    ("69f31545ad117011a062df1e", [  # TRK-56 Register truck via REST
        ("7acec1e", "feat: implement RegisterTruck use case"),
        ("d9bd730", "test: RegisterTruck saves truck and publishes registration events"),
        ("5b476d4", "feat: implement TruckController with POST and GET /trucks"),
        ("78aeea8", "test: TruckController POST and GET endpoints"),
        ("ab7e25f", "feat: implement GetTrucks use case"),
        ("b42440d", "test: GetTrucks returns mapped truck responses"),
    ]),
    ("69f3153bfc6c5d4596fd130f", [  # TRK-38 AdvanceTrucks feature
        ("c11c3bd", "feat: advance trucks use case and contract fixes"),
        ("7f9dd56", "feat: advance trucks, delivery persistence and contract fixes"),
        ("03e325c", "feat: advance trucks, delivery persistence and contract fixes"),
    ]),
    ("69f31546c074c280ba98e29b", [  # TRK-58 Move trucks forward
        ("c11c3bd", "feat: advance trucks use case and contract fixes"),
    ]),
    ("69f3154623398b9cf9e579e2", [  # TRK-60 Confirm delivery
        ("c11c3bd", "feat: advance trucks use case and contract fixes"),
        ("7f9dd56", "feat: advance trucks, delivery persistence and contract fixes"),
    ]),
    ("69f36cc8b96514ff7025ddc5", [  # TRK-68 Keep truck IN_TRANSIT
        ("c11c3bd", "feat: advance trucks use case and contract fixes"),
    ]),
    ("69f36cc8feb4b64f06cf0684", [  # TRK-69 Return truck AVAILABLE
        ("c11c3bd", "feat: advance trucks use case and contract fixes"),
    ]),
]

total_attached = 0
for card_id, commits in COMMIT_MAP:
    for sha, label in commits:
        added = add_commit_attachment(card_id, sha, label)
        if added:
            print(f"  attached {sha[:7]} to {card_id[:8]}... ({label[:50]})")
            total_attached += 1

print(f"\n  Total commits attached: {total_attached}")


# ── Step 6: GitHub PR descriptions (needs token) ─────────────────────────────
def update_github_prs(gh_token):
    print("\n=== Step 6: Update GitHub PR descriptions ===\n")
    headers = {
        "Authorization": f"Bearer {gh_token}",
        "Accept": "application/vnd.github+json",
    }
    # Fetch all PRs
    prs = requests.get(
        f"https://api.github.com/repos/{REPO}/pulls",
        headers=headers,
        params={"state": "all", "per_page": 100},
    ).json()

    if not isinstance(prs, list):
        print(f"  ERROR fetching PRs: {prs}")
        return

    print(f"  Found {len(prs)} PRs")

    # Map branch name patterns to Trello card URLs
    BRANCH_TRELLO = {
        "trk-56": "https://trello.com/c/mxeIA2VO/56",
        "trk-57": "https://trello.com/c/KbSTkoWQ/57",
        "trk-58": "https://trello.com/c/vnsTv3R6/58",
        "trk-59": "https://trello.com/c/9Y0aS3Ra/59",
        "trk-60": "https://trello.com/c/hBodkbHg/60",
        "trk-67": "https://trello.com/c/9MQLfyGM/67",
        "trk-68": "https://trello.com/c/0G1of0s7/68",
        "trk-69": "https://trello.com/c/4mHjR6qd/69",
        "trk-36": "https://trello.com/c/XiixEwUN/36",
        "trk-37": "https://trello.com/c/VwuSN09r/37",
        "trk-38": "https://trello.com/c/sSo5Aw15/38",
        "trk-134": "https://trello.com/c/oypCU2tr/134",
        "trk-146": "https://trello.com/c/eoF2GEyc/146",
        "trk-147": "https://trello.com/c/RftWWx9S/147",
        "trk-148": "https://trello.com/c/VerADze7/148",
        "trk-150": "https://trello.com/c/wuaTfw3S/150",
    }

    # Keyword fallback for branches without TRK-N
    KEYWORD_TRELLO = {
        "assign":      [("TRK-37", "https://trello.com/c/VwuSN09r/37"),
                        ("TRK-57", "https://trello.com/c/KbSTkoWQ/57")],
        "register":    [("TRK-36", "https://trello.com/c/XiixEwUN/36"),
                        ("TRK-56", "https://trello.com/c/mxeIA2VO/56")],
        "advance":     [("TRK-38", "https://trello.com/c/sSo5Aw15/38")],
        "domain":      [("TRK-9",  "https://trello.com/c/PXUrajWp/9")],
        "persistence": [("TRK-134","https://trello.com/c/oypCU2tr/134")],
        "rabbitmq":    [("TRK-150","https://trello.com/c/wuaTfw3S/150")],
    }

    for pr in prs:
        branch = pr["head"]["ref"].lower()
        body   = pr.get("body") or ""
        trello_refs = []

        # Match by TRK-N in branch
        for key, url in BRANCH_TRELLO.items():
            if key in branch:
                trk = key.upper().replace("-", "-")
                trello_refs.append(f"[{trk}]({url})")

        # Keyword fallback
        if not trello_refs:
            for kw, cards in KEYWORD_TRELLO.items():
                if kw in branch or kw in pr["title"].lower():
                    for trk, url in cards:
                        trello_refs.append(f"[{trk}]({url})")

        if not trello_refs:
            print(f"  skip (no match): #{pr['number']} {pr['title'][:50]}")
            continue

        trello_section = "\n\n---\n**Trello:** " + " · ".join(trello_refs)

        if "trello.com" in body:
            print(f"  skip (already linked): #{pr['number']} {pr['title'][:50]}")
            continue

        new_body = body.rstrip() + trello_section
        r = requests.patch(
            f"https://api.github.com/repos/{REPO}/pulls/{pr['number']}",
            headers=headers,
            json={"body": new_body},
        )
        if r.status_code == 200:
            print(f"  updated: #{pr['number']} {pr['title'][:50]}")
        else:
            print(f"  ERROR {r.status_code}: #{pr['number']}")
        time.sleep(0.3)

    # Also add Trello board link to repo description area via GitHub API
    # (Just informational — repo topics can't reference external tools)
    print("\n  GitHub PRs updated with Trello links.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--github-token", help="GitHub PAT for updating PR descriptions")
    args = parser.parse_args()

    if args.github_token:
        update_github_prs(args.github_token)
    else:
        print("\n  Tip: run with --github-token ghp_xxx to also update GitHub PR descriptions")

print("\nAll done.")
