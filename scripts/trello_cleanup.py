"""
Cleanup script — archives the 27 duplicate cards and 3 lists created by trello_setup.py,
then adds parent links (Epic/Feature) to the original team-trucks cards.

Usage:
  py trello_cleanup.py --key YOUR_KEY --token YOUR_TOKEN
"""

import argparse
import time
import requests

BASE_URL = "https://api.trello.com/1"

# ── Cards created by trello_setup.py (archive all of these) ──────────────────
NEW_CARD_IDS = [
    # Epics
    "69fb00825c9ea1977106354c",  # [Epic] Trucks — Domain Model
    "69fb0091a4db24fdf4b3588b",  # [Epic] Trucks — Use Cases
    "69fb00b3e49ee252b279d450",  # [Epic] Trucks — Messaging & Infrastructure
    # Features
    "69fb00853b62087c38d0630c",  # [Feature] Implement Truck aggregate
    "69fb00874c607de38fc653f4",  # [Feature] Implement Delivery aggregate
    "69fb008a7c0567ebad70cf43",  # [Feature] Implement DistanceCalculator domain service
    "69fb008cc88c36ded4c5cb69",  # [Feature] Implement OptimalTruckSelector domain service
    "69fb008e7a0d6015c52cf33b",  # [Feature] Implement multi-delivery capacity validation
    "69fb0094ff2d6a7e2d0b9f3f",  # [Feature] Implement RegisterTruck use case
    "69fb009a7738b2aef426c795",  # [Feature] Implement AssignTruck use case
    "69fb00a3c296478c6e72ec14",  # [Feature] Implement AdvanceTrucks use case
    "69fb00b8d21243ed5c359863",  # [Feature] Consume shipment.requested.v1
    "69fb00c46c4b26e9cec0c600",  # [Feature] Consume time.advanced.v1
    "69fb00cb8bc36e2a575e4ab2",  # [Feature] Publish truck.position.updated.v1
    "69fb00ce26acad4f8a24682d",  # [Feature] Publish delivery.completed.v1
    "69fb00d04ac37beeb570865f",  # [Feature] Publish truck.status.changed.v1
    "69fb00d3cb684136b6bdaeed",  # [Feature] Persistence & DB
    # Stories
    "69fb00983f62c31babe3349e",  # [Story] Register a new truck via REST
    "69fb009e9bc10b56758f27f9",  # [Story] Assign the closest available truck
    "69fb00a13fd13398bf30721e",  # [Story] Assign additional shipment to IN_TRANSIT
    "69fb00a78811aaa55f102237",  # [Story] Move trucks forward on each time tick
    "69fb00a96c4b26e9cec07b65",  # [Story] Publish position update for moving trucks
    "69fb00ab0e47ba7bf445774f",  # [Story] Confirm delivery and return truck
    "69fb00aef2e68ff1cfb03262",  # [Story] Keep truck IN_TRANSIT after partial delivery
    "69fb00b16b3ac351754d2743",  # [Story] Return truck to AVAILABLE
    "69fb00c149c23f11e7ef3c93",  # [Story] (interno) DispatchRequestedListener
    "69fb00c94141fab2d89d3701",  # [Story] (interno) TimeAdvancedListener
]

# ── Lists created by trello_setup.py (archive all of these) ──────────────────
NEW_LIST_IDS = [
    "69fb007d968970ee812dc384",  # Epics
    "69fb007d37c4e4cdf652021d",  # Features
    "69fb007e4af7ed1eb79ca09a",  # Stories / Backlog
]

# ── Original team-trucks Epics ────────────────────────────────────────────────
EPIC_DOMAIN  = {"url": "https://trello.com/c/PXUrajWp/9-epic-trucks-domain-model",  "name": "EPIC | Trucks — Domain Model"}
EPIC_USECASES = {"url": "https://trello.com/c/tSygOYAx/10-epic-trucks-use-cases",   "name": "EPIC | Trucks — Use Cases"}
EPIC_MESSAGING = {"url": "https://trello.com/c/uXnlWPQD/11-epic-trucks-messaging",  "name": "EPIC | Trucks — Messaging"}

# ── Original feature cards: id → {name, url, epic} ───────────────────────────
FEATURE_LINKS = {
    "69f31539f49a0bc5ecff9410": {
        "name": "FEATURE | Implement Truck aggregate",
        "url":  "https://trello.com/c/Wjrnh7Oh/32",
        "epic": EPIC_DOMAIN,
    },
    "69f31539a7047a903f32a119": {
        "name": "FEATURE | Implement Delivery aggregate",
        "url":  "https://trello.com/c/fMDL0MYp/33",
        "epic": EPIC_DOMAIN,
    },
    "69f3153a003a3a9eb4d2abd8": {
        "name": "FEATURE | Implement DistanceCalculator domain service",
        "url":  "https://trello.com/c/EzRRIDVi/35",
        "epic": EPIC_DOMAIN,
    },
    "69f3153acf1a0aee417da71f": {
        "name": "FEATURE | Implement OptimalTruckSelector domain service",
        "url":  "https://trello.com/c/K48MgcTf/34",
        "epic": EPIC_DOMAIN,
    },
    "69f36cc7dec50addfd2c9f07": {
        "name": "FEATURE | Implement multi-delivery capacity validation",
        "url":  "https://trello.com/c/M4R6qASA/66",
        "epic": EPIC_DOMAIN,
    },
    "69f3153bd2a3150c05a6f988": {
        "name": "FEATURE | Implement RegisterTruck use case",
        "url":  "https://trello.com/c/XiixEwUN/36",
        "epic": EPIC_USECASES,
    },
    "69f3153b98f2dadefc0b9a3b": {
        "name": "FEATURE | Implement AssignTruck use case",
        "url":  "https://trello.com/c/VwuSN09r/37",
        "epic": EPIC_USECASES,
    },
    "69f3153bfc6c5d4596fd130f": {
        "name": "FEATURE | Implement AdvanceTrucks use case",
        "url":  "https://trello.com/c/sSo5Aw15/38",
        "epic": EPIC_USECASES,
    },
    "69f3153ca6b541a2afb27b0a": {
        "name": "FEATURE | Consume shipment.requested.v1 — trucks",
        "url":  "https://trello.com/c/IUaXe8AT/39",
        "epic": EPIC_MESSAGING,
    },
    "69f3153c7f97f56ba0ce5f44": {
        "name": "FEATURE | Consume simulation.time.tick — trucks",
        "url":  "https://trello.com/c/eGeIWXVy/40",
        "epic": EPIC_MESSAGING,
    },
    "69f3153d81f91b060444fc96": {
        "name": "FEATURE | Publish truck.position.updated.v1",
        "url":  "https://trello.com/c/u7ozO0aj/41",
        "epic": EPIC_MESSAGING,
    },
    "69f3153e81baa8ecc1428b07": {
        "name": "FEATURE | Publish delivery.completed.v1",
        "url":  "https://trello.com/c/hXocJhOX/43",
        "epic": EPIC_MESSAGING,
    },
    "69f3153ecee1b98ac0052ca6": {
        "name": "FEATURE | Publish truck.status.changed.v1",
        "url":  "https://trello.com/c/XcUfLqbn/42",
        "epic": EPIC_MESSAGING,
    },
    "69fa03ecd3910335fab18843": {
        "name": "FEATURE | Persistence & DB",
        "url":  "https://trello.com/c/oypCU2tr/134",
        "epic": EPIC_MESSAGING,
    },
}

# ── Original story cards: id → {name, feature_id} ────────────────────────────
STORY_LINKS = {
    "69f31545ad117011a062df1e": {
        "name":       "STORY | Register a new truck via REST",
        "feature_id": "69f3153bd2a3150c05a6f988",
    },
    "69f3154502d8b926d21489f1": {
        "name":       "STORY | Assign the closest available truck to a shipment request",
        "feature_id": "69f3153b98f2dadefc0b9a3b",
    },
    "69f36cc8bbd42948e15acdd7": {
        "name":       "STORY | Assign additional shipment to IN_TRANSIT truck passing by origin",
        "feature_id": "69f3153b98f2dadefc0b9a3b",
    },
    "69f31546c074c280ba98e29b": {
        "name":       "STORY | Move trucks forward on each time tick",
        "feature_id": "69f3153bfc6c5d4596fd130f",
    },
    "69f315469a32448e5c879f60": {
        "name":       "STORY | Publish position update for moving trucks",
        "feature_id": "69f3153bfc6c5d4596fd130f",
    },
    "69f3154623398b9cf9e579e2": {
        "name":       "STORY | Confirm delivery and return truck to available",
        "feature_id": "69f3153bfc6c5d4596fd130f",
    },
    "69f36cc8b96514ff7025ddc5": {
        "name":       "STORY | Keep truck IN_TRANSIT after partial delivery until all deliveries completed",
        "feature_id": "69f3153bfc6c5d4596fd130f",
    },
    "69f36cc8feb4b64f06cf0684": {
        "name":       "STORY | Return truck to AVAILABLE only when all deliveries are completed",
        "feature_id": "69f3153bfc6c5d4596fd130f",
    },
}


def api_put(path, key, token, **data):
    data.update({"key": key, "token": token})
    r = requests.put(f"{BASE_URL}{path}", json=data)
    r.raise_for_status()
    return r.json()


def api_get(path, key, token, **params):
    params.update({"key": key, "token": token})
    r = requests.get(f"{BASE_URL}{path}", params=params)
    r.raise_for_status()
    return r.json()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--key",   required=True)
    parser.add_argument("--token", required=True)
    args = parser.parse_args()
    key, token = args.key, args.token

    # ── 1. Archive duplicate cards ────────────────────────────────────────────
    print(f"Archiving {len(NEW_CARD_IDS)} duplicate cards...")
    for card_id in NEW_CARD_IDS:
        api_put(f"/cards/{card_id}", key, token, closed=True)
        print(f"  archived {card_id}")
        time.sleep(0.2)

    # ── 2. Archive new empty lists ────────────────────────────────────────────
    print(f"\nArchiving {len(NEW_LIST_IDS)} new lists...")
    for list_id in NEW_LIST_IDS:
        api_put(f"/lists/{list_id}/closed", key, token, value=True)
        print(f"  archived list {list_id}")
        time.sleep(0.2)

    # ── 3. Add Epic link to original feature cards ────────────────────────────
    print("\nLinking features to their epics...")
    for card_id, meta in FEATURE_LINKS.items():
        card = api_get(f"/cards/{card_id}", key, token, fields="desc")
        current_desc = card.get("desc", "") or ""
        epic_link = f"\n\n---\n**Epic:** [{meta['epic']['name']}]({meta['epic']['url']})"
        if meta["epic"]["url"] in current_desc:
            print(f"  skip (already linked): {meta['name']}")
            continue
        # Strip old plain-text "Epic: ..." line if present, then append proper link
        lines = [l for l in current_desc.splitlines() if not l.strip().startswith("Epic:")]
        new_desc = "\n".join(lines).rstrip() + epic_link
        api_put(f"/cards/{card_id}", key, token, desc=new_desc)
        print(f"  linked: {meta['name']}")
        time.sleep(0.3)

    # ── 4. Add Feature + Epic link to original story cards ───────────────────
    print("\nLinking stories to their features and epics...")
    for card_id, meta in STORY_LINKS.items():
        feat = FEATURE_LINKS[meta["feature_id"]]
        card = api_get(f"/cards/{card_id}", key, token, fields="desc")
        current_desc = card.get("desc", "") or ""
        if feat["url"] in current_desc:
            print(f"  skip (already linked): {meta['name']}")
            continue
        parent_block = (
            f"\n\n---\n"
            f"**Feature:** [{feat['name']}]({feat['url']})\n"
            f"**Epic:** [{feat['epic']['name']}]({feat['epic']['url']})"
        )
        new_desc = current_desc.rstrip() + parent_block
        api_put(f"/cards/{card_id}", key, token, desc=new_desc)
        print(f"  linked: {meta['name']}")
        time.sleep(0.3)

    print("\nDone. Board is clean.")


if __name__ == "__main__":
    main()
