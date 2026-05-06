"""
Trello board setup for Supply Chain Workshop — team-trucks
Creates Epics, Features and Stories from EPICS.md structure,
links children to parents via checklists + description breadcrumbs.

Usage:
  pip install requests
  python trello_setup.py --key YOUR_API_KEY --token YOUR_TOKEN

Board: https://trello.com/b/WtpHmvMt/supply-chain-simulator-workshop
"""

import argparse
import sys
import time
import requests

BOARD_ID = "WtpHmvMt"

BASE_URL = "https://api.trello.com/1"

# Full structure from EPICS.md
STRUCTURE = [
    {
        "epic": "Trucks — Domain Model",
        "owner": "Iván",
        "label_color": "purple",
        "features": [
            {
                "name": "Implement Truck aggregate",
                "desc": (
                    "Implementar `Truck.java` con sus value objects: `TruckId`, `Location`, "
                    "`TruckStatus` (AVAILABLE / IN_TRANSIT / DELIVERED).\n"
                    "El agregado incluye `capacity`, `currentLoad`, lista de `deliveryIds` "
                    "y los métodos `remainingCapacity()` y `canAccept(int items)`.\n"
                    "Definir también el port `TruckRepository` (interfaz)."
                ),
                "stories": [],
            },
            {
                "name": "Implement Delivery aggregate",
                "desc": (
                    "Implementar `Delivery.java` con `DeliveryId` y `DeliveryItem` "
                    "(materialType + quantity).\n"
                    "`DeliveryItem` es un Value Object — sin ID, igualdad por atributos.\n"
                    "Incluye shipmentId, truckId, destination (Location), items, "
                    "assignedAt, completedAt.\n"
                    "Definir también el port `DeliveryRepository` (interfaz)."
                ),
                "stories": [],
            },
            {
                "name": "Implement DistanceCalculator domain service",
                "desc": (
                    "Calcula la distancia Manhattan entre dos `Location`. "
                    "Entrada: dos `Location`. Salida: entero."
                ),
                "stories": [],
            },
            {
                "name": "Implement OptimalTruckSelector domain service",
                "desc": (
                    "Dado un origen y un número de items requeridos, devuelve el camión "
                    "`AVAILABLE` más cercano con `remainingCapacity >= items`.\n"
                    "Si no hay camión disponible lanza una excepción de dominio."
                ),
                "stories": [],
            },
            {
                "name": "Implement multi-delivery capacity validation",
                "desc": (
                    "Un camión puede llevar varias entregas mientras el total de `DeliveryItems` "
                    "no supere su `capacity`.\n"
                    "Implica reemplazar `currentDeliveryId` (singular) por `deliveryIds` (lista) "
                    "y añadir `currentLoad`.\n"
                    "Crear `TruckStatusChangedEvent` con campos: truckId, oldStatus, newStatus, "
                    "position, currentLoad, capacity, timestamp, reason.\n\n"
                    "**Eventos de dominio:** `TruckRegisteredEvent`, `TruckStatusChangedEvent`, "
                    "`TruckPositionUpdatedEvent`, `DeliveryCompletedEvent`."
                ),
                "stories": [],
            },
        ],
    },
    {
        "epic": "Trucks — Use Cases",
        "owner": "Pau López",
        "label_color": "blue",
        "features": [
            {
                "name": "Implement RegisterTruck use case",
                "desc": (
                    "Capa de aplicación. Exponer POST /trucks y GET /trucks.\n"
                    "Incluye output ports: `TruckEventPublisher`, `DeliveryEventPublisher` "
                    "(interfaces).\n"
                    "DTOs: `CreateTruckRequest`, `TruckResponse`."
                ),
                "stories": [
                    {
                        "name": "Register a new truck via REST",
                        "desc": (
                            "Como team-trucks quiero que `POST /trucks` cree un camión y publique "
                            "`truck.status.changed.v1` con `reason: TRUCK_REGISTERED` y "
                            "`oldStatus: null`.\n"
                            "También exponer `GET /trucks` para que Map (UI) pueda obtener el "
                            "estado inicial de la flota."
                        ),
                    }
                ],
            },
            {
                "name": "Implement AssignTruck use case",
                "desc": (
                    "Asignar camión disponible más cercano a un shipment request.\n"
                    "Fallback: camión IN_TRANSIT que pase por el origen y tenga capacidad "
                    "(publica LOAD_UPDATED)."
                ),
                "stories": [
                    {
                        "name": "Assign the closest available truck to a shipment request",
                        "desc": (
                            "Como team-trucks quiero encontrar el camión disponible más cercano "
                            "al origen del shipment y asignarlo para minimizar el tiempo de entrega."
                        ),
                    },
                    {
                        "name": "Assign additional shipment to IN_TRANSIT truck passing by origin",
                        "desc": (
                            "Como team-trucks quiero asignar un nuevo shipment a un camión ya en "
                            "tránsito si pasa por el origen y tiene capacidad, publicando "
                            "`truck.status.changed.v1` con `reason: LOAD_UPDATED`."
                        ),
                    },
                ],
            },
            {
                "name": "Implement AdvanceTrucks use case",
                "desc": (
                    "⚠️ BLOQUEADO — pendiente de acordar contrato `time.advanced.v1` con Rubén.\n\n"
                    "Mover todos los camiones IN_TRANSIT los días del tick recibido. "
                    "Detectar llegadas, completar entregas, retornar a AVAILABLE."
                ),
                "stories": [
                    {
                        "name": "Move trucks forward on each time tick",
                        "desc": (
                            "Como team-trucks quiero avanzar todas las entregas IN_TRANSIT "
                            "los días correspondientes al tick recibido."
                        ),
                    },
                    {
                        "name": "Publish position update for moving trucks",
                        "desc": (
                            "Como team-trucks quiero publicar `truck.position.updated.v1` en "
                            "cada tick para cada camión en tránsito para que el mapa actualice "
                            "el icono."
                        ),
                    },
                    {
                        "name": "Confirm delivery and return truck to available",
                        "desc": (
                            "Como team-trucks quiero publicar `delivery.completed.v1` y "
                            "`truck.status.changed.v1 reason:DELIVERED` cuando un camión llega "
                            "para notificar a warehouses y reporting."
                        ),
                    },
                    {
                        "name": "Keep truck IN_TRANSIT after partial delivery until all deliveries completed",
                        "desc": (
                            "Como team-trucks quiero que el camión siga `IN_TRANSIT` al completar "
                            "una entrega si aún lleva otras, publicando `delivery.completed.v1` "
                            "y `truck.status.changed.v1 reason:LOAD_UPDATED`."
                        ),
                    },
                    {
                        "name": "Return truck to AVAILABLE only when all deliveries are completed",
                        "desc": (
                            "Como team-trucks quiero que el camión pase a `AVAILABLE` solo cuando "
                            "no le queden entregas, publicando "
                            "`truck.status.changed.v1 reason:RETURNED_TO_BASE`."
                        ),
                    },
                ],
            },
        ],
    },
    {
        "epic": "Trucks — Messaging & Infrastructure",
        "owner": "Pau Greus",
        "label_color": "green",
        "features": [
            {
                "name": "Consume shipment.requested.v1 — trucks",
                "desc": (
                    "Implementar `DispatchRequestedListener`. Deserializa "
                    "`shipment.requested.v1` y llama al use case `AssignTruck`.\n"
                    "Resolver originId/destinationId a coordenadas Location.\n"
                    "Integration test con Testcontainers (RabbitMQ + PostgreSQL)."
                ),
                "stories": [
                    {
                        "name": "(interno) DispatchRequestedListener",
                        "desc": (
                            "Deserializa `shipment.requested.v1` "
                            "(shipmentId, originId, destinationId, items[], requestedAt) "
                            "y llama al use case `AssignTruck`.\n"
                            "Integration test `DispatchRequestedListenerIT` con Testcontainers."
                        ),
                    }
                ],
            },
            {
                "name": "Consume time.advanced.v1 — trucks",
                "desc": (
                    "⚠️ BLOQUEADO — pendiente de acordar contrato con Rubén.\n\n"
                    "Implementar `TimeAdvancedListener`. Guarda `lastDay` en memoria.\n"
                    "Calcula `daysAdvanced = currentDay - lastDay` y llama a `AdvanceTrucks`."
                ),
                "stories": [
                    {
                        "name": "(interno) TimeAdvancedListener",
                        "desc": (
                            "Guarda `lastDay` en memoria. Calcula `daysAdvanced = currentDay - lastDay` "
                            "y llama a `AdvanceTrucks`.\n"
                            "Integration test `TimeAdvancedListenerIT` con Testcontainers."
                        ),
                    }
                ],
            },
            {
                "name": "Publish truck.position.updated.v1",
                "desc": (
                    "Publicar en cada tick para los camiones IN_TRANSIT que aún no han llegado.\n"
                    "Payload mínimo: solo `truckId` y `location`."
                ),
                "stories": [],
            },
            {
                "name": "Publish delivery.completed.v1",
                "desc": (
                    "Publicar cuando un camión llega a destino.\n"
                    "Campos: shipmentId, truckId, items[] (Value Objects, sin ID), "
                    "location, completedAt."
                ),
                "stories": [],
            },
            {
                "name": "Publish truck.status.changed.v1",
                "desc": (
                    "Publicar en registro, dispatch, entrega y retorno.\n"
                    "Incluye `oldStatus` (null en registro) y el campo `reason`.\n"
                    "Reasons: `TRUCK_REGISTERED`, `DISPATCHED`, `LOAD_UPDATED`, `RETURNED_TO_BASE`."
                ),
                "stories": [],
            },
            {
                "name": "Persistence & DB",
                "desc": (
                    "Adapters JPA para los ports definidos por Iván:\n"
                    "- `TruckEntity`, `TruckJpaRepository`, `TruckRepositoryAdapter`\n"
                    "- `DeliveryEntity`, `DeliveryJpaRepository`, `DeliveryRepositoryAdapter`\n\n"
                    "Liquibase:\n"
                    "- Tabla `trucks` (id, name, x, y, status, capacity, current_load)\n"
                    "- Tabla `deliveries` (id, shipment_id, truck_id, dest_x, dest_y, "
                    "assigned_at, completed_at)\n"
                    "- Tabla `delivery_items` (delivery_id, material_type, quantity)\n\n"
                    "`RabbitMQConfig` con exchanges, queues y bindings.\n"
                    "`application.yml` con RabbitMQ, datasource y Liquibase."
                ),
                "stories": [],
            },
        ],
    },
]

# Label names to create/reuse on the board
LABEL_EPIC = "Epic"
LABEL_FEATURE = "Feature"
LABEL_STORY = "Story"
LABEL_TEAM = "team-trucks"
LABEL_BLOCKED = "BLOCKED"

# List names to create/reuse
LIST_EPICS = "Epics"
LIST_FEATURES = "Features"
LIST_STORIES = "Stories / Backlog"


def trello_get(path, key, token, **params):
    params.update({"key": key, "token": token})
    r = requests.get(f"{BASE_URL}{path}", params=params)
    r.raise_for_status()
    return r.json()


def trello_post(path, key, token, **data):
    data.update({"key": key, "token": token})
    r = requests.post(f"{BASE_URL}{path}", json=data)
    r.raise_for_status()
    return r.json()


def trello_put(path, key, token, **data):
    data.update({"key": key, "token": token})
    r = requests.put(f"{BASE_URL}{path}", json=data)
    r.raise_for_status()
    return r.json()


def get_or_create_list(board_id, name, key, token, existing_lists):
    for lst in existing_lists:
        if lst["name"].lower() == name.lower():
            return lst["id"]
    lst = trello_post(f"/boards/{board_id}/lists", key, token, name=name)
    print(f"  + Created list: {name}")
    return lst["id"]


def get_or_create_label(board_id, name, color, key, token, existing_labels):
    for lbl in existing_labels:
        if lbl["name"].lower() == name.lower():
            return lbl["id"]
    lbl = trello_post(f"/boards/{board_id}/labels", key, token, name=name, color=color)
    print(f"  + Created label: {name}")
    return lbl["id"]


def card_exists(name, existing_cards):
    name_lower = name.strip().lower()
    for c in existing_cards:
        if c["name"].strip().lower() == name_lower:
            return c
    return None


def create_card(list_id, name, desc, label_ids, key, token):
    card = trello_post(
        "/cards",
        key,
        token,
        idList=list_id,
        name=name,
        desc=desc,
        idLabels=",".join(label_ids),
    )
    time.sleep(0.3)  # respect rate limits
    return card


def add_checklist_item(checklist_id, name, url, key, token):
    trello_post(
        f"/checklists/{checklist_id}/checkItems",
        key,
        token,
        name=f"[{name}]({url})",
    )
    time.sleep(0.2)


def main():
    parser = argparse.ArgumentParser(description="Setup Trello board for team-trucks")
    parser.add_argument("--key", required=True, help="Trello API key")
    parser.add_argument("--token", required=True, help="Trello API token")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print what would be created without calling the API",
    )
    args = parser.parse_args()
    key, token = args.key, args.token

    print(f"\n{'[DRY RUN] ' if args.dry_run else ''}Setting up board {BOARD_ID}...\n")

    if args.dry_run:
        _dry_run()
        return

    # Fetch current board state
    print("Fetching board state...")
    existing_lists = trello_get(f"/boards/{BOARD_ID}/lists", key, token)
    existing_labels = trello_get(f"/boards/{BOARD_ID}/labels", key, token)
    existing_cards = trello_get(
        f"/boards/{BOARD_ID}/cards",
        key,
        token,
        fields="name,url,idList",
        filter="open",
    )

    print(f"  Found {len(existing_lists)} lists, {len(existing_labels)} labels, "
          f"{len(existing_cards)} open cards")

    # Ensure lists exist
    print("\nSetting up lists...")
    list_epics_id = get_or_create_list(BOARD_ID, LIST_EPICS, key, token, existing_lists)
    list_features_id = get_or_create_list(BOARD_ID, LIST_FEATURES, key, token, existing_lists)
    list_stories_id = get_or_create_list(BOARD_ID, LIST_STORIES, key, token, existing_lists)

    # Ensure labels exist
    print("\nSetting up labels...")
    lbl_epic = get_or_create_label(BOARD_ID, LABEL_EPIC, "purple", key, token, existing_labels)
    lbl_feature = get_or_create_label(BOARD_ID, LABEL_FEATURE, "blue", key, token, existing_labels)
    lbl_story = get_or_create_label(BOARD_ID, LABEL_STORY, "sky", key, token, existing_labels)
    lbl_team = get_or_create_label(BOARD_ID, LABEL_TEAM, "orange", key, token, existing_labels)
    lbl_blocked = get_or_create_label(BOARD_ID, LABEL_BLOCKED, "red", key, token, existing_labels)

    created = skipped = 0

    for epic_data in STRUCTURE:
        epic_name = f"[Epic] {epic_data['epic']}"
        print(f"\n[Epic] {epic_name}")

        existing_epic = card_exists(epic_name, existing_cards)
        if existing_epic:
            epic_card = existing_epic
            print(f"  -> Already exists: {epic_card['url']}")
            skipped += 1
        else:
            epic_desc = f"**Owner:** {epic_data['owner']}\n\n"
            epic_card = create_card(
                list_epics_id,
                epic_name,
                epic_desc,
                [lbl_epic, lbl_team],
                key,
                token,
            )
            print(f"  + Created epic: {epic_card['url']}")
            created += 1

        # Checklist on epic card for features
        checklists = trello_get(f"/cards/{epic_card['id']}/checklists", key, token)
        features_checklist = next(
            (c for c in checklists if c["name"] == "Features"), None
        )
        if not features_checklist:
            features_checklist = trello_post(
                f"/cards/{epic_card['id']}/checklists",
                key,
                token,
                name="Features",
            )

        for feature_data in epic_data["features"]:
            feature_name = f"[Feature] {feature_data['name']}"
            print(f"  [Feature] {feature_name}")

            is_blocked = "BLOQUEADO" in feature_data.get("desc", "")
            feature_labels = [lbl_feature, lbl_team]
            if is_blocked:
                feature_labels.append(lbl_blocked)

            parent_ref = f"\n\n---\n**Epic:** [{epic_name}]({epic_card['url']})"
            feature_desc = feature_data.get("desc", "") + parent_ref

            existing_feature = card_exists(feature_name, existing_cards)
            if existing_feature:
                feature_card = existing_feature
                print(f"    -> Already exists — {feature_card['url']}")
                skipped += 1
            else:
                feature_card = create_card(
                    list_features_id,
                    feature_name,
                    feature_desc,
                    feature_labels,
                    key,
                    token,
                )
                print(f"    + Created feature — {feature_card['url']}")
                created += 1

            # Link feature in epic's checklist
            existing_items = [
                i["name"] for i in features_checklist.get("checkItems", [])
            ]
            if not any(feature_name in item for item in existing_items):
                add_checklist_item(
                    features_checklist["id"], feature_name, feature_card["url"], key, token
                )

            if not feature_data["stories"]:
                continue

            # Checklist on feature card for stories
            feat_checklists = trello_get(
                f"/cards/{feature_card['id']}/checklists", key, token
            )
            stories_checklist = next(
                (c for c in feat_checklists if c["name"] == "Stories"), None
            )
            if not stories_checklist:
                stories_checklist = trello_post(
                    f"/cards/{feature_card['id']}/checklists",
                    key,
                    token,
                    name="Stories",
                )

            for story_data in feature_data["stories"]:
                story_name = f"[Story] {story_data['name']}"
                print(f"    [Story] {story_name}")

                story_parent_ref = (
                    f"\n\n---\n**Feature:** [{feature_name}]({feature_card['url']})"
                    f"\n**Epic:** [{epic_name}]({epic_card['url']})"
                )
                story_desc = story_data.get("desc", "") + story_parent_ref

                existing_story = card_exists(story_name, existing_cards)
                if existing_story:
                    story_card = existing_story
                    print(f"      -> Already exists — {story_card['url']}")
                    skipped += 1
                else:
                    story_card = create_card(
                        list_stories_id,
                        story_name,
                        story_desc,
                        [lbl_story, lbl_team],
                        key,
                        token,
                    )
                    print(f"      + Created story — {story_card['url']}")
                    created += 1

                # Link story in feature's checklist
                existing_story_items = [
                    i["name"] for i in stories_checklist.get("checkItems", [])
                ]
                if not any(story_name in item for item in existing_story_items):
                    add_checklist_item(
                        stories_checklist["id"],
                        story_name,
                        story_card["url"],
                        key,
                        token,
                    )

    print(f"\nDone. Created: {created}  Skipped (already existed): {skipped}")
    print(
        f"\nBoard: https://trello.com/b/{BOARD_ID}?filter=label:{LABEL_TEAM}"
    )


def _dry_run():
    total_epics = len(STRUCTURE)
    total_features = sum(len(e["features"]) for e in STRUCTURE)
    total_stories = sum(
        len(f["stories"]) for e in STRUCTURE for f in e["features"]
    )
    print("Would create:")
    print(f"  {total_epics} epics")
    print(f"  {total_features} features")
    print(f"  {total_stories} stories")
    print(f"  = {total_epics + total_features + total_stories} cards total\n")
    for epic_data in STRUCTURE:
        print(f"[Epic] {epic_data['epic']}  (owner: {epic_data['owner']})")
        for f in epic_data["features"]:
            blocked = "[BLOCKED] " if "BLOQUEADO" in f.get("desc", "") else ""
            print(f"  [Feature] {blocked}{f['name']}")
            for s in f["stories"]:
                print(f"    [Story] {s['name']}")
    print()


if __name__ == "__main__":
    main()
