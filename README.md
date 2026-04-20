<<<<<<< HEAD
# Smart Campus Sensor & Room Management API

> **5COSC022W — Client-Server Architectures Coursework**
> University of Westminster | 2025/26

A fully RESTful JAX-RS API built with Jersey 2, Maven, and Apache Tomcat for managing campus rooms, sensors, and sensor readings.

---

## Table of Contents

1. [API Overview](#api-overview)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [Build & Run Instructions](#build--run-instructions)
5. [Sample curl Commands](#sample-curl-commands)
6. [Conceptual Report — Question Answers](#conceptual-report--question-answers)

---

## API Overview

The Smart Campus API provides a RESTful interface for campus facilities managers to:

- **Manage Rooms** — Create, retrieve, and safely decommission campus rooms
- **Manage Sensors** — Register CO2, Temperature, Occupancy and other sensors with room linkage validation
- **Track Readings** — Append and retrieve historical sensor readings with automatic parent data sync
- **Error Handling** — Structured JSON error responses for all failure scenarios (no raw stack traces ever exposed)
- **Observability** — Every request and response is logged via a cross-cutting JAX-RS filter

**Base URL:** `http://localhost:8080/smartcampus-api/api/v1`

---

## Technology Stack

| Component        | Technology                      |
|------------------|---------------------------------|
| Language         | Java 11                         |
| Framework        | JAX-RS 2.1 (Jersey 2.39.1)      |
| JSON Binding     | Jackson via Jersey Media module  |
| Build Tool       | Apache Maven 3.x                |
| Server           | Apache Tomcat 9.x               |
| IDE              | NetBeans IDE                    |
| Data Storage     | In-memory (`ConcurrentHashMap`) |

> ⚠️ **No Spring Boot. No database. No ZIP submission.** All constraints satisfied.

---

## Project Structure

```
smartcampus-api/
├── pom.xml
└── src/
    └── main/
        ├── java/com/smartcampus/
        │   ├── SmartCampusApplication.java      # JAX-RS Application config (@ApplicationPath)
        │   ├── model/
        │   │   ├── Room.java
        │   │   ├── Sensor.java
        │   │   ├── SensorReading.java
        │   │   └── ErrorResponse.java
        │   ├── resource/
        │   │   ├── DiscoveryResource.java        # GET /api/v1
        │   │   ├── RoomResource.java             # /api/v1/rooms
        │   │   ├── SensorResource.java           # /api/v1/sensors
        │   │   └── SensorReadingResource.java    # Sub-resource: /sensors/{id}/readings
        │   ├── exception/
        │   │   ├── RoomNotEmptyException.java
        │   │   ├── RoomNotEmptyExceptionMapper.java
        │   │   ├── LinkedResourceNotFoundException.java
        │   │   ├── LinkedResourceNotFoundExceptionMapper.java
        │   │   ├── SensorUnavailableException.java
        │   │   ├── SensorUnavailableExceptionMapper.java
        │   │   └── GlobalExceptionMapper.java
        │   ├── filter/
        │   │   └── LoggingFilter.java
        │   └── util/
        │       └── DataStore.java               # Singleton in-memory store
        └── webapp/
            └── WEB-INF/
                └── web.xml
```

---

## Build & Run Instructions

### Prerequisites

- Java JDK 11 or higher installed
- Apache Maven 3.6+ installed
- Apache Tomcat 9.x installed
- NetBeans IDE (with Tomcat configured as a server)

---

### Step 1 — Clone the Repository

```bash
git clone https://github.com/your-username/smartcampus-api.git
cd smartcampus-api
```

---

### Step 2 — Build the WAR File

```bash
mvn clean package
```

This produces `target/smartcampus-api.war`.

---

### Step 3 — Deploy to Tomcat

**Option A — Via NetBeans (Recommended):**

1. Open NetBeans → File → Open Project → select the `smartcampus-api` folder
2. Right-click the project → Properties → Run → set Server to Apache Tomcat 9
3. Right-click the project → Run (or press F6)
4. NetBeans will build, deploy, and open the browser automatically

**Option B — Manual Tomcat Deployment:**

```bash
# Copy the WAR to Tomcat's webapps directory
cp target/smartcampus-api.war /path/to/tomcat/webapps/

# Start Tomcat
/path/to/tomcat/bin/startup.sh      # Linux/macOS
/path/to/tomcat/bin/startup.bat     # Windows
```

---

### Step 4 — Verify Deployment

Open your browser or run:

```bash
curl http://localhost:8080/smartcampus-api/api/v1
```

You should receive a JSON discovery response confirming the API is live.

---

### Step 5 — Stop the Server

```bash
/path/to/tomcat/bin/shutdown.sh    # Linux/macOS
/path/to/tomcat/bin/shutdown.bat   # Windows
```

---

## Sample curl Commands

> Replace `http://localhost:8080/smartcampus-api` with your actual server URL if different.

### 1. API Discovery

```bash
curl -X GET http://localhost:8080/smartcampus-api/api/v1
```

Expected: 200 OK with API metadata, version, contact info, and resource links.

---

### 2. Create a New Room

```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "HALL-101",
    "name": "Main Lecture Hall",
    "capacity": 200
  }'
```

Expected: 201 Created with a `Location` header and the created room JSON.

---

### 3. Get All Rooms

```bash
curl -X GET http://localhost:8080/smartcampus-api/api/v1/rooms
```

Expected: 200 OK with an array of all room objects.

---

### 4. Register a New Sensor (with roomId validation)

```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "CO2-002",
    "type": "CO2",
    "status": "ACTIVE",
    "currentValue": 400.0,
    "roomId": "HALL-101"
  }'
```

Expected: 201 Created. If `HALL-101` doesn't exist, returns 422 Unprocessable Entity.

---

### 5. Filter Sensors by Type

```bash
curl -X GET "http://localhost:8080/smartcampus-api/api/v1/sensors?type=CO2"
```

Expected: 200 OK with only CO2-type sensors in the response array.

---

### 6. Post a Sensor Reading

```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/sensors/CO2-002/readings \
  -H "Content-Type: application/json" \
  -d '{
    "value": 450.5
  }'
```

Expected: 201 Created. The parent sensor's `currentValue` is also updated to 450.5.

---

### 7. Get All Readings for a Sensor

```bash
curl -X GET http://localhost:8080/smartcampus-api/api/v1/sensors/CO2-002/readings
```

Expected: 200 OK with an array of historical readings.

---

### 8. Delete a Room That Has Sensors (Should Fail)

```bash
curl -X DELETE http://localhost:8080/smartcampus-api/api/v1/rooms/LIB-301
```

Expected: 409 Conflict with a JSON error body explaining the room has active sensors.

---

### 9. Post a Reading to a MAINTENANCE Sensor (Should Fail)

```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 10.0}'
```

Expected: 403 Forbidden — OCC-001 is seeded with MAINTENANCE status.

---

### 10. Register a Sensor with Non-existent Room (Should Fail)

```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEMP-999",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 20.0,
    "roomId": "DOES-NOT-EXIST"
  }'
```

Expected: 422 Unprocessable Entity.

---

## Conceptual Report — Question Answers

---

### Part 1.1 — JAX-RS Resource Lifecycle

**Question:** Explain the default lifecycle of a JAX-RS resource class. Is a new instance created per request or treated as a singleton? How does this affect in-memory data management?

**Answer:**

By default, JAX-RS follows a **request-scoped lifecycle** — a brand-new instance of each resource class is instantiated for every incoming HTTP request and destroyed once the response is sent. This is the opposite of a singleton pattern.

This design decision has significant implications for in-memory data management. Since each request creates a new object, **instance fields cannot hold shared state** — any data stored as an instance variable would be lost immediately after the request completes and would not be visible to the next request.

To manage shared in-memory data correctly, a **Singleton pattern** must be used for the data layer. In this project, `DataStore` is implemented as a static singleton, so every request-scoped resource instance accesses the same underlying data. Furthermore, to prevent **race conditions** — where two simultaneous requests might read and write the same map entry concurrently — all maps use `ConcurrentHashMap` instead of `HashMap`. The `addReading` method is also marked `synchronized` to ensure atomicity when updating both the reading list and the parent sensor's `currentValue` in one operation. Without these safeguards, the API could suffer data corruption, lost updates, or `ConcurrentModificationException` errors under load.

---

### Part 1.2 — HATEOAS

**Question:** Why is Hypermedia (HATEOAS) considered a hallmark of advanced RESTful design? How does it benefit client developers compared to static documentation?

**Answer:**

HATEOAS (Hypermedia As The Engine Of Application State) is the principle that REST API responses should include navigational links to related resources, rather than requiring clients to construct URLs from memory or external docs.

The key benefit is **decoupling**. When a client follows links embedded in responses (e.g., `"rooms": "/api/v1/rooms"`), it does not need to hard-code any URIs. If the server restructures its URL scheme, only the server-side responses change — existing clients that navigate via links continue working without modification.

In contrast, static documentation quickly becomes outdated, forcing clients to be updated manually every time the API evolves. HATEOAS makes the API **self-discoverable**: a developer can start at `GET /api/v1` and navigate the entire system using the returned links, much like browsing a website. This dramatically reduces onboarding friction and integration errors.

---

### Part 2.1 — ID-only vs Full Object Lists

**Question:** What are the implications of returning only IDs versus full room objects in a list response?

**Answer:**

Returning **only IDs** minimises payload size — beneficial for bandwidth-constrained environments or mobile clients. However, it forces clients to issue a separate `GET /rooms/{id}` request for every room they want to display, creating an **N+1 request problem** that multiplies network overhead significantly when the list is large.

Returning **full objects** increases individual response payload size but allows clients to render complete UIs in a single round-trip. This is generally preferred for moderate-sized collections. The correct choice depends on context: for large datasets, a **paginated partial-object** response (e.g., ID + name + capacity only, without nested sensorIds) offers a practical middle ground balancing payload size and usability.

---

### Part 2.2 — DELETE Idempotency

**Question:** Is DELETE idempotent in your implementation? Justify with what happens across repeated calls.

**Answer:**

In this implementation, DELETE is **partially idempotent** in terms of server state, but not in terms of HTTP response codes.

- **First call:** The room is found and deleted → `204 No Content`
- **Second call:** The room no longer exists → `404 Not Found`

Strict REST theory defines idempotency as: *"multiple identical requests have the same effect on server state."* The server state after both calls is identical — the room is absent. In that sense, idempotency holds. However, the differing status codes (204 vs 404) mean the *responses* differ, which is a pragmatic and widely accepted REST implementation choice. The important invariant — that the resource is gone — is preserved regardless of how many times DELETE is called.

---

### Part 3.1 — @Consumes and Content-Type Mismatches

**Question:** What happens if a client sends data as `text/plain` or `application/xml` instead of `application/json` to a method annotated with `@Consumes(APPLICATION_JSON)`?

**Answer:**

JAX-RS enforces the `@Consumes` contract at the **framework level**, before the method body is even entered. If a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`, the Jersey runtime will immediately reject the request and return:

> **HTTP 415 Unsupported Media Type**

No business logic is executed. This is handled entirely by the JAX-RS dispatcher during the request matching phase. The client receives a clear signal that the data format is incompatible. This provides strict contract enforcement without requiring any manual `Content-Type` checking inside resource methods.

---

### Part 3.2 — @QueryParam vs @PathParam for Filtering

**Question:** Why is `GET /sensors?type=CO2` (query param) generally superior to `GET /sensors/type/CO2` (path param) for filtering?

**Answer:**

Query parameters are semantically designed for **optional filtering and searching**, while path parameters represent **identity** — they locate a specific, distinct resource.

Key reasons query parameters are superior for filtering:

1. **Optionality:** `GET /sensors` still works without the filter. There is no equivalent "no filter" path with path params.
2. **Composability:** Multiple filters combine naturally: `?type=CO2&status=ACTIVE`. Path params cannot compose without complex routing.
3. **Semantic correctness:** A path like `/sensors/type/CO2` implies that `type/CO2` is a named sub-resource, which is architecturally misleading.
4. **Caching & tooling:** HTTP caches, proxies, and API tools universally understand query-string filtering conventions.
5. **RESTful resource model:** The collection `/sensors` is the resource; filtering is an operation *on* that collection, not a sub-resource within it.

---

### Part 4.1 — Sub-Resource Locator Pattern

**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern versus defining all nested paths in one class.

**Answer:**

The Sub-Resource Locator pattern allows a resource method to return an **object instance** that JAX-RS then uses to handle the remainder of the path, rather than defining every nested endpoint in a single controller.

Benefits:

1. **Separation of concerns:** `SensorResource` handles sensor-level operations; `SensorReadingResource` handles reading history. Each class has a single, clear responsibility.
2. **Reduced complexity:** A single "mega-controller" managing `/sensors`, `/sensors/{id}`, and `/sensors/{id}/readings/{rid}` would quickly become hundreds of lines, making maintenance and debugging extremely difficult.
3. **Independent testability:** Each resource class can be unit-tested in isolation without needing to instantiate the parent class.
4. **Scalability:** As the API grows (e.g., adding `/sensors/{id}/alerts`), new sub-resource classes are added without modifying existing ones, adhering to the Open/Closed Principle.
5. **Context injection:** The sub-resource receives its context (`sensorId`) via constructor, keeping logic clean and focused.

---

### Part 5.2 — Why HTTP 422 over HTTP 404?

**Question:** Why is HTTP 422 Unprocessable Entity more semantically accurate than 404 when a JSON payload references a non-existent resource?

**Answer:**

HTTP **404 Not Found** means the **requested URL** could not be found on the server. In our case, `POST /api/v1/sensors` is a perfectly valid, existing endpoint — a 404 would be technically incorrect and misleading.

HTTP **422 Unprocessable Entity** means the server understood the request (correct URL, correct Content-Type, valid JSON syntax) but **could not process it** because of a semantic problem within the payload. The issue is not that the URL is wrong — it is that the `roomId` field inside the JSON body references a resource that does not exist.

This distinction is critical for API clients: a 404 signals "wrong URL, fix your endpoint," while a 422 signals "your request is well-formed but your data references something that doesn't exist." Clients can display much more helpful, actionable error messages with a 422 response. It is the semantically honest status code for dependency validation failures inside request bodies.

---

### Part 5.4 — Stack Trace Cybersecurity Risks

**Question:** From a cybersecurity standpoint, what risks arise from exposing Java stack traces to external API consumers?

**Answer:**

Exposing raw Java stack traces to external consumers creates several serious security vulnerabilities:

1. **Internal path disclosure:** Stack traces reveal absolute file system paths (e.g., `/home/deploy/webapps/smartcampus/...`), helping attackers map the server environment for directory traversal or file inclusion attacks.

2. **Technology fingerprinting:** Class names and package structures reveal the exact framework, library, and version being used (e.g., `org.glassfish.jersey 2.39.1`). Attackers can immediately look up known CVEs (Common Vulnerabilities and Exposures) for those specific versions.

3. **Business logic exposure:** The call stack reveals the exact execution path through the application — which methods call which, how data flows, and where decision points exist. This allows attackers to craft targeted inputs to exploit specific logic branches.

4. **Database and integration details:** Stack traces from ORM layers or connection pools can reveal database driver names, table structures, and query patterns.

5. **Reduced attacker effort:** All of the above intelligence is gathered passively from a single failed request, with zero active intrusion required.

The `GlobalExceptionMapper` solves this by catching all `Throwable` instances, logging the full trace **server-side** for developers, and returning only a generic `500 Internal Server Error` message to the client — providing full observability internally with zero information leakage externally.

---

### Part 5.5 — Why Filters for Cross-Cutting Concerns?

**Question:** Why use JAX-RS filters for logging rather than inserting Logger.info() statements in every resource method?

**Answer:**

Inserting `Logger.info()` calls into every resource method violates the **DRY principle** (Don't Repeat Yourself) and introduces several practical problems:

1. **Inconsistency risk:** With dozens of endpoints, a developer will inevitably forget to add logging to some methods, resulting in gaps in observability.
2. **Code clutter:** Business logic becomes interleaved with logging infrastructure, reducing readability and maintainability.
3. **Maintenance burden:** If the log format needs to change, every single resource method must be updated.
4. **Missing coverage:** Manual logging cannot easily capture responses that were generated by exception mappers — those never pass through resource methods at all.

A **ContainerRequestFilter / ContainerResponseFilter** intercepts every single HTTP interaction at the framework level, regardless of which resource method handles it. Logging is guaranteed to be complete, consistent, and formatted uniformly. Changing the log format requires editing exactly one class. This is the **cross-cutting concern** principle in action — concerns like logging, authentication, and CORS that span all endpoints belong in filters, not in business logic.

---

*Report prepared in accordance with 5COSC022W Coursework Specification v3.1 | University of Westminster 2025/26*
=======
# smartcampus-api
Smart Campus Sensor &amp; Room Management API - JAX-RS REST API
>>>>>>> a1fe57b6bb844b9cb45dde9318ea45548e42d94f
