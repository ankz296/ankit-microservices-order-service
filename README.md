# ankit-microservices-order-service
📦 Order Service
Order Service is a core business microservice in the E‑Commerce Microservices System, responsible for managing order creation, retrieval, and lifecycle state management.
It is designed to be Kafka & Saga‑ready using the Outbox Pattern.

🛠 Tech Stack
Java 17
Spring Boot 3.5.x
Spring Data JPA (Hibernate 6)
PostgreSQL
Flyway (DB migrations)
Spring Cloud
Eureka Discovery Client
Config Client
API Gateway (routing)
Docker (Postgres)


🚀 Service Details
Item           Value  
ServiceName    order-service
Port            8083
Base Path       /api/v1/orders
Config Source   Spring Cloud Config Server
Discovery       Eureka Server
Database        PostgreSQL (Docker)

🔗 Service Registration & Routing

Registered with Eureka Discovery Server
All APIs are accessed via API Gateway

Gateway URL:
http://localhost:8080


📌 API Endpoints
✅ 1. Create Order
POST /api/v1/orders
Creates a new order with initial status CREATED and writes an outbox event.
Request Body
JSON{  "userId": "11111111-1111-1111-1111-111111111111",  "totalAmount": 499.99}Show more lines
Response (201 Created)
JSON{  "orderId": "9b6f3d5b-2d6f-4f4c-bf2c-5e2e8a5c5c1a",  "userId": "11111111-1111-1111-1111-111111111111",  "totalAmount": 499.99,  "status": "CREATED",  "createdAt": "2026-03-08T18:02:45.123+05:30"}Show more lines

✅ 2. Get Order by ID
GET /api/v1/orders/{orderId}
Example
GET /api/v1/orders/9b6f3d5b-2d6f-4f4c-bf2c-5e2e8a5c5c1a

Response (200 OK)
JSON{  "orderId": "9b6f3d5b-2d6f-4f4c-bf2c-5e2e8a5c5c1a",  "userId": "11111111-1111-1111-1111-111111111111",  "totalAmount": 499.99,  "status": "CREATED",  "createdAt": "2026-03-08T18:02:45.123+05:30"}Show more lines

✅ 3. Get Orders by User
GET /api/v1/orders/user/{userId}
Example
GET /api/v1/orders/user/11111111-1111-1111-1111-111111111111

Response (200 OK)
JSON[  {    "orderId": "9b6f3d5b-2d6f-4f4c-bf2c-5e2e8a5c5c1a",    "userId": "11111111-1111-1111-1111-111111111111",    "totalAmount": 499.99,    "status": "CREATED",    "createdAt": "2026-03-08T18:02:45.123+05:30"  }]Show more lines

🔄 Order Lifecycle States
The order follows a defined lifecycle to support Saga Pattern:

CREATED
PAYMENT_PENDING
PAYMENT_COMPLETED
PAYMENT_FAILED
CANCELLED


🗄 Database Schema
✅ orders
Stores order core data.

ColumnTypeDescriptionidUUIDPrimary keyuser_idUUIDUser identifiertotal_amountNUMERIC(12,2)Order totalstatusVARCHAROrder statuscreated_atTIMESTAMPTZCreated timeupdated_atTIMESTAMPTZUpdated time

✅ order_outbox
Implements Outbox Pattern for reliable event publishing.

ColumnTypeDescriptionidUUIDPrimary keyaggregate_idUUIDOrder IDevent_typeVARCHAREvent name (ORDER_CREATED)payloadTEXTEvent data (JSON string)statusVARCHARNEW, PUBLISHED, FAILEDcreated_atTIMESTAMPTZEvent creation time

🧠 Architectural Highlights

✅ One DB per Service (true microservices)
✅ Flyway‑managed schema
✅ Outbox Pattern (Kafka‑ready)
✅ API Gateway routing
✅ Eureka discovery
✅ No hardcoded configuration


🔮 Upcoming Enhancements

Kafka event publishing from outbox
Payment service integration
Saga choreography (Order → Payment)
Distributed tracing & metrics


✅ How to Run (Local)

Start Docker containers (Postgres)
Start Config Server
Start Eureka Server
Start API Gateway
Start Order Service
Call APIs via Gateway (localhost:8080)