import requests, time, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

KEY   = "a5df122d6183ebfcaa99c219ae595533"
TOKEN = "ATTAe6296912f9210452b6bbe0c2a7a6a42b7032fb6a0b285886e499d3358740654909B2BE80"
BASE  = "https://api.trello.com/1"
STORIES_TODO = "69f35a95f9ba42f112d205d2"

LABEL_STORY   = None  # will be looked up
LABEL_TRUCKS  = None
BOARD = "WtpHmvMt"

# Look up label IDs
labels = requests.get(f"{BASE}/boards/{BOARD}/labels", params={"key": KEY, "token": TOKEN}).json()
for l in labels:
    if l["name"] == "Story":
        LABEL_STORY = l["id"]
    if l["name"] == "team-trucks":
        LABEL_TRUCKS = l["id"]

print(f"Story label: {LABEL_STORY}, team-trucks label: {LABEL_TRUCKS}")

RESTORE = [
    {
        "id":   "69fb00c149c23f11e7ef3c93",
        "name": "STORY | (interno) DispatchRequestedListener",
        "desc": (
            "Implementar DispatchRequestedListener. Deserializa shipment.requested.v1 "
            "(shipmentId, originId, destinationId, items[], requestedAt) y llama al use case AssignTruck.\n"
            "Integration test DispatchRequestedListenerIT con Testcontainers.\n\n"
            "---\n"
            "**Feature:** [FEATURE | Consume shipment.requested.v1 - trucks](https://trello.com/c/IUaXe8AT/39)\n"
            "**Epic:** [EPIC | Trucks - Messaging](https://trello.com/c/uXnlWPQD/11)"
        ),
    },
    {
        "id":   "69fb00c94141fab2d89d3701",
        "name": "STORY | (interno) TimeAdvancedListener",
        "desc": (
            "Implementar TimeAdvancedListener. Guarda lastDay en memoria.\n"
            "Calcula daysAdvanced = currentDay - lastDay y llama a AdvanceTrucks.\n"
            "Integration test TimeAdvancedListenerIT con Testcontainers.\n\n"
            "---\n"
            "**Feature:** [FEATURE | Consume simulation.time.tick - trucks](https://trello.com/c/eGeIWXVy/40)\n"
            "**Epic:** [EPIC | Trucks - Messaging](https://trello.com/c/uXnlWPQD/11)"
        ),
    },
]

for s in RESTORE:
    payload = {
        "key": KEY, "token": TOKEN,
        "closed": False,
        "idList": STORIES_TODO,
        "desc": s["desc"],
        "name": s["name"],
    }
    if LABEL_TRUCKS:
        payload["idLabels"] = LABEL_TRUCKS
    r = requests.put(f"{BASE}/cards/{s['id']}", json=payload)
    r.raise_for_status()
    print(f"restored: {s['name']}")
    time.sleep(0.3)

print("Done.")
