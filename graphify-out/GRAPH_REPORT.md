# Graph Report - .  (2026-05-05)

## Corpus Check
- Corpus is ~7,789 words - fits in a single context window. You may not need a graph.

## Summary
- 257 nodes · 341 edges · 54 communities (30 shown, 24 thin omitted)
- Extraction: 82% EXTRACTED · 18% INFERRED · 0% AMBIGUOUS · INFERRED: 62 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Controller & API Tests|Controller & API Tests]]
- [[_COMMUNITY_Persistence & Domain Contracts|Persistence & Domain Contracts]]
- [[_COMMUNITY_Architecture Decisions|Architecture Decisions]]
- [[_COMMUNITY_Delivery Domain Model|Delivery Domain Model]]
- [[_COMMUNITY_Delivery Repository|Delivery Repository]]
- [[_COMMUNITY_Truck Domain Model|Truck Domain Model]]
- [[_COMMUNITY_Truck Selection Logic|Truck Selection Logic]]
- [[_COMMUNITY_Advance Trucks Tests|Advance Trucks Tests]]
- [[_COMMUNITY_Distance Calculation|Distance Calculation]]
- [[_COMMUNITY_Assign Truck Tests|Assign Truck Tests]]
- [[_COMMUNITY_Domain Value Objects|Domain Value Objects]]
- [[_COMMUNITY_Delivery Persistence IT|Delivery Persistence IT]]
- [[_COMMUNITY_Register Truck Tests|Register Truck Tests]]
- [[_COMMUNITY_Truck Repository Port|Truck Repository Port]]
- [[_COMMUNITY_Delivery Repository Port|Delivery Repository Port]]
- [[_COMMUNITY_Domain Exceptions|Domain Exceptions]]
- [[_COMMUNITY_REST Controller|REST Controller]]
- [[_COMMUNITY_App Bootstrap|App Bootstrap]]
- [[_COMMUNITY_JPA Delivery Repo|JPA Delivery Repo]]
- [[_COMMUNITY_Get Trucks Use Case|Get Trucks Use Case]]
- [[_COMMUNITY_Sanity Tests|Sanity Tests]]
- [[_COMMUNITY_Delivery Completed Event|Delivery Completed Event]]
- [[_COMMUNITY_Delivery JPA Entity|Delivery JPA Entity]]
- [[_COMMUNITY_Delivery Item JPA Embeddable|Delivery Item JPA Embeddable]]
- [[_COMMUNITY_Create Truck DTO|Create Truck DTO]]
- [[_COMMUNITY_Truck Position Event|Truck Position Event]]
- [[_COMMUNITY_Truck Registered Event|Truck Registered Event]]
- [[_COMMUNITY_Truck Status Changed Event|Truck Status Changed Event]]
- [[_COMMUNITY_OpenAPI Config|OpenAPI Config]]
- [[_COMMUNITY_TDD Coverage Policy|TDD Coverage Policy]]

## God Nodes (most connected - your core abstractions)
1. `AdvanceTrucksTest` - 12 edges
2. `AssignTruckTest` - 11 edges
3. `DeliveryRepositoryAdapterIT` - 9 edges
4. `TruckTest` - 9 edges
5. `OptimalTruckSelectorTest` - 9 edges
6. `Epic 1: Trucks — Domain Model` - 9 edges
7. `Epic 2: Trucks — Use Cases` - 9 edges
8. `DistanceCalculatorTest` - 8 edges
9. `Epic 3: Trucks — Messaging & Infrastructure` - 8 edges
10. `AdvanceTrucks` - 7 edges

## Surprising Connections (you probably didn't know these)
- `Truck Aggregate` --references--> `Location Value Object`  [INFERRED]
  EPICS.md → README.md
- `RegisterTruck Use Case` --references--> `POST /trucks Endpoint`  [EXTRACTED]
  EPICS.md → src/main/resources/openapi.yaml
- `AssignTruck Use Case` --references--> `truck.status.changed.v1 Event`  [EXTRACTED]
  EPICS.md → README.md
- `Truck Registration Dual-Event Decision` --rationale_for--> `truck.registered.v1 Event`  [EXTRACTED]
  EPICS.md → README.md
- `Truck Registration Dual-Event Decision` --rationale_for--> `truck.status.changed.v1 Event`  [EXTRACTED]
  EPICS.md → README.md

## Hyperedges (group relationships)
- **Supply Chain Event-Driven Flow: Simulation -> Transport -> Warehouses/Reporting/Map** — service_simulation, event_time_advanced_v1, infra_time_advanced_listener, usecase_advance_trucks, event_delivery_completed_v1, service_warehouses, service_reporting, service_map_ui [EXTRACTED 0.95]
- **Shipment Dispatch Flow: Warehouses -> Transport -> Truck Assignment** — service_warehouses, event_shipment_requested_v1, infra_dispatch_requested_listener, usecase_assign_truck, domain_optimal_truck_selector, event_truck_status_changed_v1 [EXTRACTED 0.95]
- **Hexagonal Architecture Layers: Domain / Application / Infrastructure** — epic1_domain_model, epic2_use_cases, epic3_messaging_infra, pattern_hexagonal_architecture, port_truck_repository, port_delivery_repository, port_truck_event_publisher, port_delivery_event_publisher [EXTRACTED 0.95]

## Communities (54 total, 24 thin omitted)

### Community 0 - "Controller & API Tests"
Cohesion: 0.11
Nodes (6): from(), TruckControllerTest, CoverageMarker, CoverageMarkerTest, GetTrucksTest, RegisterTruck

### Community 1 - "Persistence & Domain Contracts"
Cohesion: 0.16
Nodes (19): deliveries DB Table, delivery_items DB Table, trucks DB Table, DistanceCalculator Domain Service, OptimalTruckSelector Domain Service, Epic 1: Trucks — Domain Model, Epic 2: Trucks — Use Cases, Epic 3: Trucks — Messaging & Infrastructure (+11 more)

### Community 2 - "Architecture Decisions"
Cohesion: 0.23
Nodes (17): materialType Naming Convention Constraint, time.advanced.v1 Contract Pending, Truck Registration Dual-Event Decision, delivery.completed.v1 Event, shipment.requested.v1 Event (Consumed), time.advanced.v1 Event (Consumed), truck.position.updated.v1 Event, truck.registered.v1 Event (+9 more)

### Community 3 - "Delivery Domain Model"
Cohesion: 0.21
Nodes (3): Delivery, DeliveryTest, AdvanceTrucks

### Community 4 - "Delivery Repository"
Cohesion: 0.16
Nodes (3): DeliveryRepository, DeliveryRepositoryAdapter, AssignTruck

### Community 10 - "Domain Value Objects"
Cohesion: 0.24
Nodes (11): Delivery Aggregate, DeliveryItem Value Object, Location Value Object, Truck Aggregate, TruckStatus Enum, CreateTruckRequest DTO, TruckResponse DTO, Transport Service OpenAPI Spec (+3 more)

## Knowledge Gaps
- **15 isolated node(s):** `DeliveryCompletedEvent`, `DeliveryEntity`, `DeliveryItemEmbeddable`, `CreateTruckRequest`, `TruckPositionUpdatedEvent` (+10 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **24 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AdvanceTrucks` connect `Delivery Domain Model` to `Delivery Repository`?**
  _High betweenness centrality (0.011) - this node is a cross-community bridge._
- **What connects `DeliveryCompletedEvent`, `DeliveryEntity`, `DeliveryItemEmbeddable` to the rest of the system?**
  _15 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Controller & API Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.11 - nodes in this community are weakly interconnected._