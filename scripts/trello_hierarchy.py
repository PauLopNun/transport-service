"""
Adds Jira-style checklists to Epic and Feature cards:
  - Each Epic card gets a "Features" checklist with links to its feature cards
  - Each Feature card (that has stories) gets a "Stories" checklist with links

Idempotent: skips checklists that already exist.
"""

import os`nimport requests, time, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

KEY   = os.environ["TRELLO_KEY"]
TOKEN = os.environ["TRELLO_TOKEN"]
BASE  = "https://api.trello.com/1"


def get(path, **p):
    p.update({"key": KEY, "token": TOKEN})
    return requests.get(f"{BASE}{path}", params=p).json()


def post(path, **data):
    data.update({"key": KEY, "token": TOKEN})
    r = requests.post(f"{BASE}{path}", json=data)
    r.raise_for_status()
    return r.json()


def card_url(card_id):
    c = get(f"/cards/{card_id}", fields="url")
    return c["url"]


def ensure_checklist(card_id, checklist_name, items):
    """items = list of (label, card_id_or_url)"""
    existing = get(f"/cards/{card_id}/checklists")
    cl = next((c for c in existing if c["name"] == checklist_name), None)

    if cl is None:
        cl = post(f"/cards/{card_id}/checklists", name=checklist_name)
        print(f"    + created checklist '{checklist_name}'")
        time.sleep(0.2)

    existing_names = {item["name"] for item in cl.get("checkItems", [])}

    for label, url in items:
        item_text = f"[{label}]({url})"
        if any(label in n for n in existing_names):
            continue
        post(f"/checklists/{cl['id']}/checkItems", name=item_text)
        time.sleep(0.2)


# ── Fetch live URLs for all relevant cards ────────────────────────────────────

print("Fetching card URLs...")

# Epics
E_DOMAIN   = get("/cards/69f3152ef2fd6d094fd5da32", fields="url,name")
E_USECASES = get("/cards/69f3152e22590b71a9b97f80", fields="url,name")
E_MESSAGING = get("/cards/69f3152e80cd2059b907a5fd", fields="url,name")

# Epic 1 features
F = {
    "truck_agg":   get("/cards/69f31539f49a0bc5ecff9410", fields="url,name"),
    "del_agg":     get("/cards/69f31539a7047a903f32a119", fields="url,name"),
    "dist_calc":   get("/cards/69f3153a003a3a9eb4d2abd8", fields="url,name"),
    "opt_sel":     get("/cards/69f3153acf1a0aee417da71f", fields="url,name"),
    "multi_del":   get("/cards/69f36cc7dec50addfd2c9f07", fields="url,name"),
    # Epic 2
    "register":    get("/cards/69f3153bd2a3150c05a6f988", fields="url,name"),
    "assign":      get("/cards/69f3153b98f2dadefc0b9a3b", fields="url,name"),
    "advance":     get("/cards/69f3153bfc6c5d4596fd130f", fields="url,name"),
    # Epic 3
    "cons_ship":   get("/cards/69f3153ca6b541a2afb27b0a", fields="url,name"),
    "cons_time":   get("/cards/69f3153c7f97f56ba0ce5f44", fields="url,name"),
    "pub_pos":     get("/cards/69f3153d81f91b060444fc96", fields="url,name"),
    "pub_del":     get("/cards/69f3153e81baa8ecc1428b07", fields="url,name"),
    "pub_status":  get("/cards/69f3153ecee1b98ac0052ca6", fields="url,name"),
    "persist":     get("/cards/69fa03ecd3910335fab18843", fields="url,name"),
}

# Stories
S = {
    # Epic 2 — RegisterTruck
    "reg_rest":    get("/cards/69f31545ad117011a062df1e", fields="url,name"),
    # Epic 2 — AssignTruck
    "asgn_avail":  get("/cards/69f3154502d8b926d21489f1", fields="url,name"),
    "asgn_trans":  get("/cards/69f36cc8bbd42948e15acdd7", fields="url,name"),
    # Epic 2 — AdvanceTrucks
    "move_fwd":    get("/cards/69f31546c074c280ba98e29b", fields="url,name"),
    "pub_pos_upd": get("/cards/69f315469a32448e5c879f60", fields="url,name"),
    "confirm_del": get("/cards/69f3154623398b9cf9e579e2", fields="url,name"),
    "keep_trans":  get("/cards/69f36cc8b96514ff7025ddc5", fields="url,name"),
    "ret_avail":   get("/cards/69f36cc8feb4b64f06cf0684", fields="url,name"),
    # Epic 3 — Consumers
    "dispatch_l":  get("/cards/69fb00c149c23f11e7ef3c93", fields="url,name"),
    "time_l":      get("/cards/69fb00c94141fab2d89d3701", fields="url,name"),
    # Epic 3 — Persistence
    "tdd_tests":   get("/cards/69faf2911a411945a5980438", fields="url,name"),
    "base_cfg":    get("/cards/69faf29b052c11dbc42949d7", fields="url,name"),
    "jpa_adapt":   get("/cards/69faf2a79a6b36ebc667940d", fields="url,name"),
    "rmq_cfg":     get("/cards/69faf2ba156e53de578cf572", fields="url,name"),
}

print("Done fetching.\n")


# ── Epic 1: Domain Model ──────────────────────────────────────────────────────
print(f"Epic: {E_DOMAIN['name']}")
ensure_checklist("69f3152ef2fd6d094fd5da32", "Features", [
    (F["truck_agg"]["name"],  F["truck_agg"]["url"]),
    (F["del_agg"]["name"],    F["del_agg"]["url"]),
    (F["dist_calc"]["name"],  F["dist_calc"]["url"]),
    (F["opt_sel"]["name"],    F["opt_sel"]["url"]),
    (F["multi_del"]["name"],  F["multi_del"]["url"]),
])

# ── Epic 2: Use Cases ─────────────────────────────────────────────────────────
print(f"\nEpic: {E_USECASES['name']}")
ensure_checklist("69f3152e22590b71a9b97f80", "Features", [
    (F["register"]["name"], F["register"]["url"]),
    (F["assign"]["name"],   F["assign"]["url"]),
    (F["advance"]["name"],  F["advance"]["url"]),
])

# ── Epic 3: Messaging ─────────────────────────────────────────────────────────
print(f"\nEpic: {E_MESSAGING['name']}")
ensure_checklist("69f3152e80cd2059b907a5fd", "Features", [
    (F["cons_ship"]["name"],  F["cons_ship"]["url"]),
    (F["cons_time"]["name"],  F["cons_time"]["url"]),
    (F["pub_pos"]["name"],    F["pub_pos"]["url"]),
    (F["pub_del"]["name"],    F["pub_del"]["url"]),
    (F["pub_status"]["name"], F["pub_status"]["url"]),
    (F["persist"]["name"],    F["persist"]["url"]),
])

# ── Feature checklists with Stories ──────────────────────────────────────────
print(f"\nFeature: {F['register']['name']}")
ensure_checklist("69f3153bd2a3150c05a6f988", "Stories", [
    (S["reg_rest"]["name"], S["reg_rest"]["url"]),
])

print(f"\nFeature: {F['assign']['name']}")
ensure_checklist("69f3153b98f2dadefc0b9a3b", "Stories", [
    (S["asgn_avail"]["name"], S["asgn_avail"]["url"]),
    (S["asgn_trans"]["name"], S["asgn_trans"]["url"]),
])

print(f"\nFeature: {F['advance']['name']}")
ensure_checklist("69f3153bfc6c5d4596fd130f", "Stories", [
    (S["move_fwd"]["name"],    S["move_fwd"]["url"]),
    (S["pub_pos_upd"]["name"], S["pub_pos_upd"]["url"]),
    (S["confirm_del"]["name"], S["confirm_del"]["url"]),
    (S["keep_trans"]["name"],  S["keep_trans"]["url"]),
    (S["ret_avail"]["name"],   S["ret_avail"]["url"]),
])

print(f"\nFeature: {F['cons_ship']['name']}")
ensure_checklist("69f3153ca6b541a2afb27b0a", "Stories", [
    (S["dispatch_l"]["name"], S["dispatch_l"]["url"]),
])

print(f"\nFeature: {F['cons_time']['name']}")
ensure_checklist("69f3153c7f97f56ba0ce5f44", "Stories", [
    (S["time_l"]["name"], S["time_l"]["url"]),
])

print(f"\nFeature: {F['persist']['name']}")
ensure_checklist("69fa03ecd3910335fab18843", "Stories", [
    (S["tdd_tests"]["name"], S["tdd_tests"]["url"]),
    (S["base_cfg"]["name"],  S["base_cfg"]["url"]),
    (S["jpa_adapt"]["name"], S["jpa_adapt"]["url"]),
    (S["rmq_cfg"]["name"],   S["rmq_cfg"]["url"]),
])

print("\nDone. Open any Epic or Feature card to see the full hierarchy.")
