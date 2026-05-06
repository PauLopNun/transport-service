import requests, time, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

KEY   = "a5df122d6183ebfcaa99c219ae595533"
TOKEN = "ATTAe6296912f9210452b6bbe0c2a7a6a42b7032fb6a0b285886e499d3358740654909B2BE80"
BASE  = "https://api.trello.com/1"

members = requests.get(f"{BASE}/boards/WtpHmvMt/members",
                       params={"key": KEY, "token": TOKEN}).json()
PAU_G = next(m["id"] for m in members if m["username"] == "pugz2")

remaining = [
    ("69faf2bc540767a451bdfbba", "RabbitMQ Config"),
    ("69faf2911a411945a5980438", "TDD Persistence tests"),
    ("69faf29b052c11dbc42949d7", "Base configuration"),
    ("69faf2a79a6b36ebc667940d", "JPA Adapter Trucks"),
]

for cid, name in remaining:
    time.sleep(0.5)
    resp = requests.get(f"{BASE}/cards/{cid}",
                        params={"key": KEY, "token": TOKEN, "fields": "name,idMembers"})
    if resp.status_code != 200:
        print(f"  ERROR getting {name}: {resp.status_code}")
        continue
    card = resp.json()
    if PAU_G in card.get("idMembers", []):
        print(f"  skip (already assigned): {name}")
        continue
    r = requests.post(f"{BASE}/cards/{cid}/idMembers",
                      json={"key": KEY, "token": TOKEN, "value": PAU_G})
    status = "ok" if r.status_code == 200 else f"ERR {r.status_code}"
    print(f"  {status}: {name}")

print("Done.")
