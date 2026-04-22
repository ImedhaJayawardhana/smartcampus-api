# Smart Campus Sensor & Room Management API

**University of Westminster | In Collaboration with Informatics Institute of Technology**

**5COSC022W — Client-Server Architectures | Coursework 2025/26**

| | |
|---|---|
| **Student Name** | Imedha Indeewari Ubeysingha Jayawardhana |
| **UOW Number** | 20232644 |
| **IIT Number** | W2120502 |
| **GitHub** | [github.com/ImedhaJayawardhana/smartcampus-api](https://github.com/ImedhaJayawardhana/smartcampus-api) |
| **Base URL** | `http://localhost:8081/smartcampus-api/api/v1` |
| **Submission** | 24th April 2026 |

---

## API Overview

This project implements a RESTful Smart Campus API using JAX-RS (Jersey 2.39.1) deployed on Apache Tomcat 9 as a WAR application. The system manages Rooms and Sensors across university campus buildings and maintains a historical log of sensor readings. All data is stored in-memory using ConcurrentHashMap and ArrayList data structures — no database is used. The API follows RESTful architectural principles including proper HTTP methods, meaningful status codes, JSON responses, a versioned entry point at /api/v1, sub-resource locator patterns, and comprehensive error handling through custom Exception Mappers and JAX-RS Filters.

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 11 |
| Framework | JAX-RS 2.1 (Jersey 2.39.1) |
| JSON Binding | Jackson via Jersey Media module |
| Build Tool | Apache Maven 3.x |
| Server | Apache Tomcat 9.x |
| IDE | NetBeans IDE |
| Data Storage | In-memory (ConcurrentHashMap) |

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1 | Discovery — API metadata and resource links |
| GET | /api/v1/rooms | Get all rooms |
| POST | /api/v1/rooms | Create a new room |
| GET | /api/v1/rooms/{roomId} | Get a specific room by ID |
| DELETE | /api/v1/rooms/{roomId} | Delete room (blocked if sensors exist — 409) |
| GET | /api/v1/sensors | Get all sensors (supports ?type= filter) |
| POST | /api/v1/sensors | Create a sensor (validates roomId — 422) |
| GET | /api/v1/sensors/{sensorId} | Get a specific sensor by ID |
| DELETE | /api/v1/sensors/{sensorId} | Delete a sensor |
| GET | /api/v1/sensors/{sensorId}/readings | Get all readings for a sensor |
| POST | /api/v1/sensors/{sensorId}/readings | Add a reading (updates currentValue) |

---

## Project Structure
---

## How to Build and Run

### Prerequisites
- Java JDK 11 or higher
- Apache Maven 3.6+
- Apache Tomcat 9.x
- NetBeans IDE
---

## Sample curl Commands

### 1. Get API Discovery
```bash
curl -X GET http://localhost:8081/smartcampus-api/api/v1
```

### 2. Create a Room
```bash
curl -X POST http://localhost:8081/smartcampus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"HALL-101\",\"name\":\"Main Lecture Hall\",\"capacity\":200}"
```

### 3. Get All Rooms
```bash
curl -X GET http://localhost:8081/smartcampus-api/api/v1/rooms
```

### 4. Create a Sensor
```bash
curl -X POST http://localhost:8081/smartcampus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"CO2-002\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":400.0,\"roomId\":\"LIB-301\"}"
```

### 5. Get Sensors Filtered by Type
```bash
curl -X GET "http://localhost:8081/smartcampus-api/api/v1/sensors?type=CO2"
```

### 6. Add a Sensor Reading
```bash
curl -X POST http://localhost:8081/smartcampus-api/api/v1/sensors/CO2-002/readings \
  -H "Content-Type: application/json" \
  -d "{\"value\":450.5}"
```

### 7. Get All Readings for a Sensor
```bash
curl -X GET http://localhost:8081/smartcampus-api/api/v1/sensors/CO2-002/readings
```

### 8. Delete a Room with Sensors (Expect 409 Conflict)
```bash
curl -X DELETE http://localhost:8081/smartcampus-api/api/v1/rooms/LIB-301
```

---

## Report — Question Answers

---

### Part 1 : Service Architecture & Setup

#### Question 1.1: JAX-RS Resource Lifecycle & In-Memory Data Management

The default configuration of JAX-RS is to instantiate a new instance of a Resource class each time an HTTP request is received. This is referred to as the **per-request lifecycle**. Since there is a new request object per request, any data that is stored as an instance variable in the resource class is lost immediately once the request is completed. This implies that storing rooms or sensors using instance variables between requests would make the API completely non-functional.

We apply the **Singleton design pattern** to the DataStore class to solve this issue. The DataStore maintains one fixed instance which is used by all resource classes and by all requests. Every resource class retrieves this shared instance using DataStore.getInstance(), making sure that all data remains during the lifetime of the server. Moreover, several HTTP requests may come at the same time — shared HashMaps and ArrayLists might have race conditions due to simultaneous access and data corruption. **ConcurrentHashMap** should be used in place of HashMap to ensure data access is thread-safe and no data is lost when concurrently loaded. The addReading() method is also marked **synchronized** to ensure that updating both the reading list and the parent sensor's currentValue field happens atomically, preventing any partial updates under concurrent load.

---

#### Question 1.2: HATEOAS and Its Benefits for Client Developers

HATEOAS, which stands for **Hypermedia as the Engine of Application State**, is a core principle of advanced REST architecture. It implies that all API responses consist of not only data but also hyperlinks that lead the client to the subsequent potential steps or other resources. For example, a response for a room may have a link to its sensors, and a sensor response may have a link to its readings. In our implementation, the GET /api/v1 Discovery endpoint returns links to /api/v1/rooms and /api/v1/sensors along with a full endpoint catalogue, allowing clients to navigate the entire API from a single entry point.

This approach is considered a hallmark of advanced RESTful design because it makes the API **self-discoverable**. Client developers do not have to memorise URL structures or have to totally depend on static documents which can be out of date. Instead, it is the API that informs clients about what they can do next at runtime. This would significantly reduce the risk of clients using wrong URLs, simplifies the API to integrate, and enables the server to change its URL structure over time without disrupting existing clients — provided that the links in responses are maintained correctly.

---

### Part 2 : Room Management

#### Question 2.1: Returning Only IDs vs Full Room Objects in a List Response

In coming up with a GET endpoint which will give a list of rooms, two main ways can be used: returning just room IDs or returning the entire room objects with all the fields. Returning IDs alone results in a very lightweight response consuming less bandwidth on the network significantly. This is particularly important when the system contains thousands of rooms. The trade-off is that the client must then make individual follow-up requests to retrieve the full details of each room it cares about, which increases the total number of network round trips and can significantly increase latency — a problem known as the **N+1 request problem**.

Full room objects returned provide the client with everything it requires in just one request, reducing the number of API calls required. However, this increases the size of each response, which uses more bandwidth and can slow down the API when the list is large. The best approach depends on the use case. For large collections, returning a summary or IDs is preferred. For small collections or where clients almost always need the full data, returning full objects is more efficient. For a university campus scale, a **paginated partial-object response** would offer the ideal balance between bandwidth efficiency and usability.

---

#### Question 2.2: Is the DELETE Operation Idempotent?

Yes, DELETE is idempotent in our implementation. Idempotency means that making the same request multiple times produces the same end state on the server as making it just once. In this implementation, when a client sends a DELETE request for a room that exists and has no sensors assigned, the room is removed from the ConcurrentHashMap in our DataStore and a **204 No Content** response is returned. If the same DELETE request is sent a second time, the room no longer exists in the system, so our API returns a **404 Not Found** response with a structured JSON error body. Although the HTTP status code of the first and second request is different, the state of the server does not change following both calls — the room does not exist. This meets the definition of idempotency. The client can be given varying status codes, but the resource state remains the same, which is the appropriate and anticipated behaviour of a DELETE operation as per the principles of REST.

Additionally, it is worth mentioning that in our implementation, it is not possible to delete a room when it has sensors allocated to it. Our **RoomNotEmptyExceptionMapper** catches the request and sends a **409 Conflict** response before anything can be deleted, to keep the data intact and to avoid orphaned sensors in the system.

---

### Part 3 : Sensor Operations & Linking

#### Question 3.1: Technical Consequences of Sending Wrong Content-Type to @Consumes(APPLICATION_JSON)

The @Consumes(MediaType.APPLICATION_JSON) annotation on our POST method in SensorResource declares a strict **media type contract** between the client and the server. This annotation informs the JAX-RS framework that this endpoint will only accept requests whose Content-Type header is explicitly set to application/json. In case clients try to send data in a different format like text/plain or application/xml, JAX-RS addresses this mismatch entirely at the **framework dispatcher level**, prior to our method body being invoked.

The Jersey runtime checks the Content-Type header of the incoming request at the request matching stage and verifies the value set in @Consumes. In case of a mismatch, it will automatically reject the request and send back **HTTP 415 Unsupported Media Type** to the client. This implies that none of our business logic — such as the roomId validation, the DataStore look up, and sensor registration — is ever executed at all. The Jackson deserialiser is never invoked and no resource class processes the request.

This behaviour provides important benefits to our API. First, **security** — any unanticipated content types are prevented before they can lead to parsing errors or erratic behaviour. Second, **contract clarity** — the client is given a definite status code that informs them of exactly what is wrong, making debugging much faster. Third, **clean code** — we do not need to add any manual content-type checking into our methods because the framework enforces the contract automatically, keeping resource methods focused purely on business logic.

---

#### Question 3.2: @QueryParam vs Path Parameter for Filtering Collections

Using @QueryParam for filtering (e.g. GET /api/v1/sensors?type=CO2) is considered superior to embedding the filter value in the URL path (e.g. GET /api/v1/sensors/type/CO2) for several important reasons. First, query parameters are **optional by nature**. In our SensorResource implementation, the endpoint GET /api/v1/sensors works perfectly without any query parameter, returning all sensors in the system. When the client adds ?type=CO2, the list is filtered to only include CO2 sensors. With a path parameter approach, separate route definitions would be needed for both the filtered and unfiltered versions, making the routing configuration more complex and harder to maintain.

Additionally, path parameters semantically imply a **hierarchical resource structure**. The path /api/v1/sensors/type/CO2 misleadingly suggests that type and CO2 are specific addressable sub-resources within the sensors collection, which is architecturally incorrect. A sensor type is not a resource — it is a filter criterion applied to the collection. Query parameters correctly communicate that we are performing an optional search operation on the existing /api/v1/sensors collection, not navigating to a different resource entirely. Furthermore, query parameters naturally support **multiple simultaneous filters** without changing the URL structure — for example ?type=CO2&status=ACTIVE. Query parameters are the universally accepted REST convention for search and filter operations on collections, endorsed by OpenAPI, JSON:API, and API design guidelines from Microsoft, Google, and AWS.

---

### Part 4 : Deep Nesting with Sub-Resources

#### Question 4.1: Architectural Benefits of the Sub-Resource Locator Pattern

The Sub-Resource Locator pattern is a JAX-RS architectural pattern where a resource method does not respond directly to a request but instead **returns an instance of another dedicated resource class** that will handle the nested path. In our implementation, SensorResource has a locator method annotated with @Path("/{sensorId}/readings") that returns a new SensorReadingResource instance. JAX-RS then forwards all requests to that path to SensorReadingResource.

The main architectural advantage is the **separation of concerns**. Each resource class has only one clear responsibility. SensorResource deals with sensor-level CRUD operations, and SensorReadingResource exclusively deals with reading history. This makes the codebase much easier to read, maintain, and test independently. In large APIs with numerous nested resources, placing all endpoint handlers in a single huge controller class would make it extremely long, difficult to navigate, and hard to edit without introducing bugs. The Sub-Resource Locator pattern distributes this complexity across focused classes. It also allows JAX-RS to select the appropriate handler class dynamically at runtime depending on the URL path received, enabling the routing to be flexible and scaleable as the API grows. Additional benefits include independent testability of each class, compliance with the Open/Closed Principle when adding new sub-collections, and clean context injection through the SensorReadingResource constructor which receives the sensorId directly.

---

### Part 5 : Advanced Error Handling, Exception Mapping & Logging

#### Question 5.1: Why HTTP 422 is More Semantically Accurate Than 404 for Missing References

**HTTP 404 Not Found** is intended to communicate that the requested URL or resource endpoint does not exist on the server. It is a response about the URL itself, not about the content of the request body.

However, when a client sends a POST request to a valid URL such as /api/v1/sensors but includes a roomId in the JSON body that does not exist in the system, the URL itself is perfectly valid and found. The problem is not with the URL but with the **data inside the request payload**. Returning 404 in this case would be misleading because it implies the endpoint does not exist, which is incorrect.

**HTTP 422 Unprocessable Entity** is more semantically accurate because it tells the client that the server understood the request, the URL was found, the Content-Type was correct, but the request body contains invalid or unresolvable references that prevent it from being processed. Our LinkedResourceNotFoundExceptionMapper maps this scenario to 422, giving the client precise and actionable information — the problem is specifically an invalid foreign key reference in the body — allowing the client developer to identify and fix the issue quickly.

---

#### Question 5.2: Cybersecurity Risks of Exposing Internal Java Stack Traces

Exposing raw Java stack traces in API error responses is a serious cybersecurity risk classified under **OWASP Top 10 as A05 — Security Misconfiguration**. A single stack trace can provide an attacker with significant information to compromise the system, all without any active intrusion attempt.

Stack traces reveal the **internal package structure and class names** of the application, which tells an attacker exactly which frameworks, libraries, and versions are being used. With this information, the attacker can search for known CVEs (Common Vulnerabilities and Exposures) for those specific versions and craft targeted exploits. Stack traces can also expose **file system paths**, database query structures, configuration details, and the exact line of code where an error occurred. This helps attackers understand the internal architecture of the system and identify weak points to exploit.

Our **GlobalExceptionMapper** solves this completely by catching all unexpected Throwable errors and returning only a generic HTTP 500 Internal Server Error message with no internal details, while the actual error is safely logged on the server side for developers to review.

---

#### Question 5.3: Why JAX-RS Filters are Better Than Manual Logging in Every Method

Using JAX-RS ContainerRequestFilter and ContainerResponseFilter for logging is vastly superior to manually inserting Logger.info() statements inside every single resource method for several important reasons.

First, filters follow the **DRY (Don't Repeat Yourself) principle**. A single filter class — our LoggingFilter — automatically intercepts every request and response in the entire API without any modification to resource classes. Manual logging would require adding log statements to every method across every resource class, which is tedious and highly error-prone. Second, **consistency is guaranteed** with filters. With manual logging, developers might forget to add log statements to new methods, leading to gaps in observability. Filters ensure 100% coverage automatically, including for any new endpoints added in the future. Critically, filters also capture responses generated by our Exception Mappers — something that method-level logging cannot do at all, since those responses never pass through resource methods.

Finally, if the logging format or behaviour needs to change, **only the LoggingFilter class needs to be modified**. With manual logging spread across dozens of methods, every single one would need to be updated individually, significantly increasing the risk of introducing bugs. Filters keep cross-cutting concerns cleanly separated from business logic, making the codebase more maintainable and professional.

---
