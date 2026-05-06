# Transport Service — Trello

Board (team-trucks filter): https://trello.com/b/WtpHmvMt/supply-chain-workshop-gft-2026?filter=label:team-trucks
Full board (all teams): https://trello.com/b/WtpHmvMt/supply-chain-workshop-gft-2026

Status legend: `To Do` · `Doing` · `Revision` · `Done`

---

### [TRK-9] EPIC | Trucks — Domain Model `Revision` — @inay

- **[TRK-32] FEATURE | Implement Truck aggregate** `Revision`
  - [TRK-189] STORY | Implement Truck aggregate root, value objects and TruckRepository port `Revision`
- **[TRK-33] FEATURE | Implement Delivery aggregate** `Revision`
  - [TRK-190] STORY | Implement Delivery aggregate, DeliveryItem value object and DeliveryRepository port `Revision`
- **[TRK-35] FEATURE | Implement DistanceCalculator domain service** `Revision`
  - [TRK-191] STORY | Implement DistanceCalculator — Manhattan distance and isOnRoute `Revision`
- **[TRK-34] FEATURE | Implement OptimalTruckSelector domain service** `Revision`
  - [TRK-192] STORY | Implement OptimalTruckSelector — closest AVAILABLE truck and IN_TRANSIT fallback `Revision`
- **[TRK-66] FEATURE | Implement multi-delivery capacity validation** `Revision`
  - [TRK-193] STORY | Add deliveryIds list and currentLoad to Truck aggregate `Revision`
  - [TRK-194] STORY | Implement domain events: TruckRegisteredEvent, TruckStatusChangedEvent, TruckPositionUpdatedEvent, DeliveryCompletedEvent `Revision`

---

### [TRK-10] EPIC | Trucks — Use Cases `Revision` — @paulopeznunez

- **[TRK-36] FEATURE | Implement RegisterTruck use case** `Revision`
  - [TRK-56] STORY | Register a new truck via REST `Revision`
- **[TRK-37] FEATURE | Implement AssignTruck use case** `Revision`
  - [TRK-57] STORY | Assign the closest available truck to a shipment request `Revision`
  - [TRK-67] STORY | Assign additional shipment to IN_TRANSIT truck passing by origin `Revision`
- **[TRK-38] FEATURE | Implement AdvanceTrucks use case** `Revision`
  - [TRK-58] STORY | Move trucks forward on each time tick `Revision`
  - [TRK-59] STORY | Publish position update for moving trucks `Revision`
  - [TRK-60] STORY | Confirm delivery and return truck to available `Revision`
  - [TRK-68] STORY | Keep truck IN_TRANSIT after partial delivery until all deliveries completed `Revision`
  - [TRK-69] STORY | Return truck to AVAILABLE only when all deliveries are completed `Revision`

---

### [TRK-11] EPIC | Trucks — Messaging `Doing` — @pugz2

- **[TRK-134] FEATURE | Persistence & DB** `Revision`
  - [TRK-146] STORY | TDD — Persistence and configuration tests `Revision`
  - [TRK-147] STORY | Base configuration: application.yml + Liquibase `Revision`
  - [TRK-148] STORY | JPA Adapter — Trucks `Revision`
  - [TRK-150] STORY | RabbitMQ Config `Revision`
- **[TRK-39] FEATURE | Consume shipment.requested.v1 — trucks** `To Do`
  - [TRK-177] STORY | (interno) DispatchRequestedListener `To Do`
- **[TRK-40] FEATURE | Consume time.advanced.v1 — trucks** `To Do`
  - [TRK-179] STORY | (interno) TimeAdvancedListener `To Do`
- **[TRK-41] FEATURE | Publish truck.position.updated.v1** `To Do`
  - [TRK-195] STORY | Implement RabbitMQ publisher for truck.position.updated.v1 `To Do`
- **[TRK-42] FEATURE | Publish truck.status.changed.v1** `To Do`
  - [TRK-196] STORY | Implement RabbitMQ publisher for truck.status.changed.v1 `To Do`
- **[TRK-43] FEATURE | Publish delivery.completed.v1** `To Do`
  - [TRK-197] STORY | Implement RabbitMQ publisher for delivery.completed.v1 `To Do`
