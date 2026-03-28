# Microservices Extension — Task Board

---

## Branch 1: `feature/inventory-service`
Create a new Spring Boot project `inventory-service` from scratch.

- [ ] `Product` entity with `productId`, `name`, `stock` (quantity available)
- [ ] `POST /inventory` → create a product with initial stock
- [ ] `GET /inventory/{productId}` → check stock
- [ ] `POST /inventory/{productId}/reserve` → reduce stock by a given quantity, reject if not enough
- [ ] `POST /inventory/{productId}/release` → return stock (for cancelled orders)
- [ ] Unit + integration tests

---

## Branch 2: `feature/order-inventory-integration`
Connect `order-service` with `inventory-service`.

- [ ] Add `WebClient` to `order-service` (non-blocking HTTP client)
- [ ] Before placing an order, call `inventory-service` to reserve stock for each item
- [ ] If any item is out of stock → reject the order and release already-reserved items (rollback)
- [ ] Add `InventoryUnavailableException` + handler in `GlobalExceptionHandler`
- [ ] Update integration tests

---

## Branch 3: `feature/notification-service`
Replace the current `OrderNotificationListener` with RabbitMQ.

- [ ] Add RabbitMQ to `docker-compose.yml`
- [ ] In `order-service`: publish `OrderPlacedEvent` to RabbitMQ instead of Spring's internal event system
- [ ] Create new Spring Boot project `notification-service` that listens to RabbitMQ and logs the notification
- [ ] Update `docker-compose.yml` to include all 3 services
> Note: RabbitMQ docker-compose config will be provided

---

## Branch 4: `feature/api-gateway`
Single entry point for all services.

- [ ] Create new Spring Boot project `api-gateway` using Spring Cloud Gateway
- [ ] Route `/api/v1/orders/**` → `order-service`
- [ ] Route `/api/v1/inventory/**` → `inventory-service`
- [ ] Add rate limiting (max requests per second)
- [ ] Update `docker-compose.yml` — only the gateway port is exposed externally

---

## Branch 5: `feature/k8s-microservices`
Update Kubernetes manifests for the full system.

- [ ] Add `Deployment` + `Service` for each new service
- [ ] Add RabbitMQ deployment
- [ ] All services internal (`ClusterIP`) except the gateway (`LoadBalancer`)
- [ ] Update GitHub Actions to build and push all 4 images
