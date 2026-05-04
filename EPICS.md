# Work Breakdown — Transport Service
Supply Chain Workshop · GFT 2026

Dividimos el trabajo por capa hexagonal. Cada persona es dueña de una capa completa.

| Epic | Owner |
|---|---|
| Trucks — Domain Model | Iván |
| Trucks — Use Cases | Pau López |
| Trucks — Messaging & Infrastructure | Pau Greus |

TDD con 100% de coverage en la lógica de negocio. Tests antes que implementación.

---

## Cosas a acordar antes de empezar

- **Firmas de los repositorios** — Iván las define, todos codean contra ellas.
- **Nombres de exchanges y queues** — Pau Greus los define como constantes en `RabbitMQConfig`, los demás importan.
- **`materialType`** — el nombre exacto debe coincidir con team-warehouses y team-factories.
- **Nombre del evento de tiempo** — Trello dice `simulation.time.tick`, contratos actualizados dicen `time.advanced.v1`. Confirmar con Rubén antes de implementar `TimeAdvancedListener` y `AdvanceTrucks`.
- **Registro de camión** — los contratos definen dos eventos: `truck.registered.v1` y `truck.status.changed.v1 reason:TRUCK_REGISTERED`. Confirmar si publicamos los dos o solo uno.

---

## EPIC | Trucks — Domain Model
**Owner: Iván**

Todo el dominio puro. Sin Spring, sin JPA, sin dependencias de infraestructura.

---

### FEATURE | Implement Truck aggregate
Implementar `Truck.java` con sus value objects: `TruckId`, `Location`, `TruckStatus` (AVAILABLE / IN_TRANSIT / DELIVERED).
El agregado incluye `capacity`, `currentLoad`, lista de `deliveryIds` y los métodos `remainingCapacity()` y `canAccept(int items)`.
Definir también el port `TruckRepository` (interfaz).

### FEATURE | Implement Delivery aggregate
Implementar `Delivery.java` con `DeliveryId` y `DeliveryItem` (materialType + quantity).
`DeliveryItem` es un Value Object — sin ID, igualdad por atributos.
Incluye shipmentId, truckId, destination (Location), items, assignedAt, completedAt.
Definir también el port `DeliveryRepository` (interfaz).

### FEATURE | Implement DistanceCalculator domain service
Calcula la distancia Manhattan entre dos `Location`. Entrada: dos `Location`. Salida: entero.

### FEATURE | Implement OptimalTruckSelector domain service
Dado un origen y un número de items requeridos, devuelve el camión `AVAILABLE` más cercano con `remainingCapacity >= items`.
Si no hay camión disponible lanza una excepción de dominio.

### FEATURE | Implement multi-delivery capacity validation
Un camión puede llevar varias entregas mientras el total de `DeliveryItems` no supere su `capacity`.
Implica reemplazar `currentDeliveryId` (singular) por `deliveryIds` (lista) y añadir `currentLoad`.
También crear `TruckStatusChangedEvent` — **clase que no existe en el skeleton** — con campos: truckId, oldStatus, newStatus, position, currentLoad, capacity, timestamp, reason.

**Eventos de dominio a implementar:** `TruckRegisteredEvent`, `TruckStatusChangedEvent`, `TruckPositionUpdatedEvent`, `DeliveryCompletedEvent`.

---

## EPIC | Trucks — Use Cases
**Owner: Pau López**

Capa de aplicación. Depende del dominio de Iván. En los unit tests se mockean repositorios y publishers — no hace falta esperar a la infra de Pau Greus.

También incluye los ports de salida (interfaces): `TruckEventPublisher`, `DeliveryEventPublisher`.
Y los DTOs: `CreateTruckRequest`, `TruckResponse`.

---

### FEATURE | Implement RegisterTruck use case

**STORY | Register a new truck via REST**
Como team-trucks quiero que `POST /trucks` cree un camión y publique `truck.status.changed.v1` con `reason: TRUCK_REGISTERED` y `oldStatus: null`.
También exponer `GET /trucks` para que Map (UI) pueda obtener el estado inicial de la flota.

---

### FEATURE | Implement AssignTruck use case

**STORY | Assign the closest available truck to a shipment request**
Como team-trucks quiero encontrar el camión disponible más cercano al origen del shipment y asignarlo para minimizar el tiempo de entrega.

**STORY | Assign additional shipment to IN_TRANSIT truck passing by origin**
Como team-trucks quiero asignar un nuevo shipment a un camión ya en tránsito si pasa por el origen y tiene capacidad, publicando `truck.status.changed.v1` con `reason: LOAD_UPDATED`.

---

### FEATURE | Implement AdvanceTrucks use case
**BLOQUEADO** — pendiente de acordar contrato `time.advanced.v1` con Rubén (Simulation).

**STORY | Move trucks forward on each time tick**
Como team-trucks quiero avanzar todas las entregas IN_TRANSIT los días correspondientes al tick recibido.

**STORY | Publish position update for moving trucks**
Como team-trucks quiero publicar `truck.position.updated.v1` en cada tick para cada camión en tránsito para que el mapa actualice el icono.

**STORY | Confirm delivery and return truck to available**
Como team-trucks quiero publicar `delivery.completed.v1` y `truck.status.changed.v1 reason:DELIVERED` cuando un camión llega para notificar a warehouses y reporting.

**STORY | Keep truck IN_TRANSIT after partial delivery until all deliveries completed**
Como team-trucks quiero que el camión siga `IN_TRANSIT` al completar una entrega si aún lleva otras, publicando `delivery.completed.v1` y `truck.status.changed.v1 reason:LOAD_UPDATED`.

**STORY | Return truck to AVAILABLE only when all deliveries are completed**
Como team-trucks quiero que el camión pase a `AVAILABLE` solo cuando no le queden entregas, publicando `truck.status.changed.v1 reason:RETURNED_TO_BASE`.

---

## EPIC | Trucks — Messaging & Infrastructure
**Owner: Pau Greus**

Toda la capa de infraestructura: config, listeners, publishers, persistencia y tests de integración.
Depende de los use cases de Pau López para delegar la lógica.

---

### FEATURE | Consume shipment.requested.v1 — trucks

**STORY | (interno)** Implementar `DispatchRequestedListener`. Deserializa `shipment.requested.v1` (shipmentId, originId, destinationId, items[], requestedAt) y llama al use case `AssignTruck`.
Integration test `DispatchRequestedListenerIT` con Testcontainers (RabbitMQ + PostgreSQL).

---

### FEATURE | Consume simulation.time.tick — trucks
**BLOQUEADO** — pendiente de acordar contrato con Rubén.

**STORY | (interno)** Implementar `TimeAdvancedListener`. Guarda `lastDay` en memoria. Calcula `daysAdvanced = currentDay - lastDay` y llama a `AdvanceTrucks`.
Integration test `TimeAdvancedListenerIT` con Testcontainers.

---

### FEATURE | Publish truck.position.updated.v1
Publicar en cada tick para los camiones IN_TRANSIT que aún no han llegado. Payload mínimo: solo `truckId` y `location`.

---

### FEATURE | Publish delivery.completed.v1
Publicar cuando un camión llega a destino. Campos: shipmentId, truckId, items[] (Value Objects, sin ID), location, completedAt.

---

### FEATURE | Publish truck.status.changed.v1
Publicar en registro, dispatch, entrega y retorno. Incluye `oldStatus` (null en registro) y el campo `reason`.
Reasons: `TRUCK_REGISTERED`, `DISPATCHED`, `LOAD_UPDATED`, `RETURNED_TO_BASE`.

---

### FEATURE | Persistence & DB
Implementar los adapters JPA que dan vida a los ports definidos por Iván:
- `TruckEntity`, `TruckJpaRepository`, `TruckRepositoryAdapter`
- `DeliveryEntity`, `DeliveryJpaRepository`, `DeliveryRepositoryAdapter`

Liquibase:
- Tabla `trucks` (id, name, x, y, status, capacity, current_load)
- Tabla `deliveries` (id, shipment_id, truck_id, dest_x, dest_y, assigned_at, completed_at)
- Tabla `delivery_items` (delivery_id, material_type, quantity)

`RabbitMQConfig` con exchanges, queues y bindings. `application.yml` con RabbitMQ, datasource y Liquibase.

---

## Dependencias

Epic 2 depende de las interfaces de Epic 1. Epic 3 depende de los use cases de Epic 2.
En la práctica Epic 2 puede arrancar en paralelo con Epic 1 mockeando los repositorios en los tests.
Lo que está bloqueado para todos: el contrato de tiempo con Rubén.
