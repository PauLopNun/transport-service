# Transport Service — Working Rules

## Contexto del proyecto

Workshop GFT 2026. Proyecto de evaluación — los reviewers miran el historial de commits para ver el proceso de trabajo, no solo el resultado final.

El desarrollador activo es **Pau López** (epic: Trucks — Use Cases). Ver `EPICS.md` para la división completa del trabajo entre Iván, Pau López y Pau Greus.

## Metodología de trabajo

### TDD estricto — siempre en este orden

1. Escribir el test completo con 100% de cobertura de la clase objetivo.
2. Commitear solo el test: `test: ...`
3. Implementar la clase hasta que pasen todos los tests.
4. Commitear solo la implementación: `feat: ...` o `refactor: ...`

**Nunca mezclar test e implementación en el mismo commit.**
**Nunca commitear una implementación sin su test pasando al 100%.**
La pipeline falla si el coverage baja del 100% en lógica de negocio.

### Commits pequeños y atómicos

Un commit = un fichero o un cambio conceptual pequeño.
El historial debe contar una historia: test → implementación → test → implementación.
Sin commits grandes. Sin "WIP". Sin agrupar varios ficheros distintos.

### Cobertura

100% de coverage en domain y application (use cases).
La infraestructura (listeners, publishers, JPA) se cubre con integration tests, no con unit tests.

## Qué mockear en el epic de Use Cases

Los use cases dependen de ports (interfaces). En los tests siempre se mockean:
- `TruckRepository` — lo implementa Pau Greus. Mockear hasta que esté.
- `DeliveryRepository` — lo implementa Pau Greus. Mockear hasta que esté.
- `TruckEventPublisher` — lo implementa Pau Greus. Mockear hasta que esté.
- `DeliveryEventPublisher` — lo implementa Pau Greus. Mockear hasta que esté.

Los domain services NO se mockean si son lógica pura (sin dependencias externas):
- `DistanceCalculator` — se usa real en los tests de `OptimalTruckSelector` y `AssignTruck`.
- `OptimalTruckSelector` — se puede usar real en `AssignTruck` o mockear según convenga.

## Dependencias bloqueantes con otros miembros del equipo

Avisar siempre antes de codear algo que depende de otro:

| Necesito | De quién | Cuándo avisarle |
|---|---|---|
| `TruckRepository`, `DeliveryRepository` interfaces firmadas | Iván (Epic 1) | Antes de escribir el primer use case |
| `TruckStatusChangedEvent`, `TruckRegisteredEvent`, `DeliveryCompletedEvent` clases | Iván (Epic 1) | Antes de implementar los publishers en use cases |
| `TruckEventPublisher`, `DeliveryEventPublisher` implementaciones reales | Pau Greus (Epic 3) | Solo para integration tests — en unit tests se mockean |
| Contrato `time.advanced.v1` nombre definitivo | Rubén (Simulation) | Antes de implementar `AdvanceTrucks` |

## Contratos de referencia

Los contratos de mensajería actualizados están en el documento compartido con el equipo (última versión acordada con Map el 2026-05).
Respetar siempre los nombres de campos exactos del contrato — no inventar variaciones.
