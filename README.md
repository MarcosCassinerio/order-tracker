# order-tracker — Día 1: Backend

Microservicio Spring Boot para gestión de pedidos.
Proyecto de práctica para entrevistas técnicas.

---

## Estructura del proyecto

```
src/main/java/com/example/ordertracker/
├── model/
│   ├── Order.java            ← Aggregate Root (encapsulamiento real, sin setters)
│   ├── OrderItem.java        ← Value Object (factory method, invariantes)
│   └── OrderStatus.java      ← Enum con lógica de transiciones (SRP)
│
├── pricing/
│   ├── PricingStrategy.java      ← Interfaz del Strategy
│   ├── RegularPricing.java       ← Implementación sin descuento
│   ├── HolidayPricing.java       ← Implementación 20% off
│   └── PricingStrategyFactory.java ← Factory: selecciona la estrategia correcta
│
├── event/
│   ├── OrderPlacedEvent.java          ← Domain event (record inmutable)
│   └── OrderNotificationListener.java ← Observer: reacciona al evento async
│
├── repository/
│   └── OrderRepository.java  ← Interfaz JPA (Spring genera la implementación)
│
├── service/
│   └── OrderService.java     ← Lógica de negocio + orquestación
│
├── controller/
│   └── OrderController.java  ← Capa HTTP: recibe, valida, devuelve status correcto
│
├── dto/
│   ├── CreateOrderRequest.java ← DTO de entrada con Bean Validation
│   ├── OrderResponse.java      ← DTO de salida (nunca exponer entidades JPA)
│   └── UpdateStatusRequest.java
│
└── exception/
    ├── OrderNotFoundException.java
    ├── InvalidStatusTransitionException.java
    └── GlobalExceptionHandler.java  ← @ControllerAdvice + RFC 7807
```

---

## Patrones implementados (para mencionar en la entrevista)

| Patrón | Dónde | Por qué |
|--------|-------|---------|
| **Strategy** | `PricingStrategy` + implementaciones | Agregar nuevo pricing = nueva clase, sin tocar el Service |
| **Factory** | `PricingStrategyFactory` | Selecciona la estrategia sin if-else |
| **Observer** | `OrderPlacedEvent` + `OrderNotificationListener` | OrderService no conoce a los listeners |
| **Repository** | `OrderRepository` | Abstrae el acceso a datos detrás de una interfaz |
| **DTO** | `CreateOrderRequest`, `OrderResponse` | Separa el contrato de la API del modelo interno |

---

## Principios SOLID aplicados

- **S**: `OrderService` orquesta, `PricingStrategyFactory` selecciona, `OrderNotificationListener` notifica
- **O**: Agregar `VipPricing` = nueva clase `@Component("VIP")`, sin tocar `OrderService`
- **L**: `HolidayPricing` puede reemplazar a `RegularPricing` en cualquier contexto que use `PricingStrategy`
- **I**: `OrderRepository` tiene métodos específicos, no una interfaz genérica gigante
- **D**: `OrderService` depende de `OrderRepository` (interfaz), no de `JpaRepository` directamente

---

## Correr los tests

```bash
# Unit tests (sin Docker, sin DB — milisegundos)
mvn test -Dtest="OrderServiceTest,PricingStrategyTest,OrderStatusTest,OrderControllerTest"

# Integration tests (necesitan Docker corriendo — Testcontainers levanta PostgreSQL)
mvn test -Dtest="OrderServiceIntegrationTest"

# Todos los tests
mvn verify
```

---

## Endpoints disponibles (Día 2 lo pone en Docker)

```
POST   /api/v1/orders               → 201 Created + Location header
GET    /api/v1/orders/{id}          → 200 OK | 404 Not Found
GET    /api/v1/orders?page=0&size=20
GET    /api/v1/orders?customerId=1
PATCH  /api/v1/orders/{id}/status   → 200 OK | 409 Conflict | 404 Not Found

GET    /actuator/health             → K8s health probe
GET    /actuator/health/liveness    → K8s liveness probe
GET    /actuator/health/readiness   → K8s readiness probe
```

### Ejemplo de request

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "contactEmail": "marcos@example.com",
    "pricingType": "HOLIDAY",
    "items": [
      { "productId": "PROD-1", "productName": "Laptop", "quantity": 1, "unitPrice": 1000.00 },
      { "productId": "PROD-2", "productName": "Mouse",  "quantity": 2, "unitPrice": 25.00 }
    ]
  }'
```

### Respuesta esperada

```json
{
  "id": 1,
  "customerId": 1,
  "contactEmail": "marcos@example.com",
  "status": "PENDING",
  "totalAmount": 840.00,
  "rawSubtotal": 1050.00,
  "items": [ ... ],
  "createdAt": "2026-03-26T20:00:00Z"
}
```

*(Con HOLIDAY pricing: 1050 × 0.80 = 840.00)*

---

## Próximos pasos

- **Día 2**: `Dockerfile` multi-stage + `docker-compose.yml` + GitHub Actions pipeline
- **Día 3**: manifests de Kubernetes (`deployment.yaml`, `service.yaml`, `hpa.yaml`)
