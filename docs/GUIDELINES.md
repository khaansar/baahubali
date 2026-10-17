# Platform Engineering Guidelines & Specifications

## 1. API Response Standardization

All services must return a strictly uniform JSON structure for every request to eliminate parsing ambiguity for frontend clients.

* **Mandatory Fields:** Every response must be wrapped in a generic envelope containing exactly three fields: `status` (integer), `data` (object/array/string), and `timestamp` (ISO-8601 string).
* **Data Payload:** The `data` field must directly contain the requested resource, a simple string message, or structured error details. Extraneous fields must be omitted.

**Success Response Example (Resource):**

```json
{
  "status": 200,
  "data": {
    "id": "10439a5f-43bd-41d2-9a1f-84c6e57dcbdb",
    "email": "user@example.com",
    "role": "STUDENT",
    "createdAt": "2026-09-22T09:25:58Z"
  },
  "timestamp": "2026-09-22T09:26:00.123Z"
}

```

**Success Response Example (Simple Action / Logout):**

```json
{
  "status": 200,
  "data": "Logout successful",
  "timestamp": "2026-09-22T09:26:05.442Z"
}

```

**Error Response Example:**

```json
{
  "status": 401,
  "data": {
    "error": "Unauthorized",
    "code": "SESSION_EXPIRED",
    "message": "Session expired or user logged in from another device.",
    "path": "/api/users/me"
  },
  "timestamp": "2026-09-22T09:26:10.891Z"
}

```

## 2. Separation of Concerns

Code must be strictly segregated to maintain testability and prevent business logic from leaking into the web layer.

* **Controllers (Web Layer):** Must contain zero business logic. Responsibilities are strictly limited to receiving HTTP requests, validating DTOs, routing to the Service layer, and formatting the HTTP response (e.g., setting `HttpOnly` cookies, headers, and HTTP status codes using `ResponseEntity`).
* **Services (Business Layer):** Must own the complete transaction end-to-end. Services orchestrate database calls, cache evictions, and token generation. Services must never accept or return HTTP-specific objects (like `HttpServletRequest` or `ResponseCookie`).

## 3. Authentication & Session Management

The platform utilizes stateless JSON Web Tokens (JWTs) combined with stateful Redis tracking to enforce strict session control.

* **Single-Device Login Enforcement:** Concurrent sessions are prohibited. Upon authentication, a unique `sessionId` must be generated, embedded in the JWT payload, and stored as a plain UTF-8 string in Redis (`user:session:{userId}`).
* **Stateful Verification:** On every request, the provided token's `sessionId` must be non-blockingly checked against the active session ID in Redis. If they do not match, the token must be rejected.
* **Secure Delivery:** JWTs must only be delivered to the client via `HttpOnly`, `Secure` cookies. Tokens must never be placed in the JSON response body.

## 4. Architecture & The Trust Boundary

The system utilizes an API Gateway pattern, establishing the gateway as the absolute trust boundary for the platform.

* **Centralized Security:** The API Gateway is the exclusive service responsible for cryptographically verifying JWT signatures and expirations.
* **Identity Propagation:** Upon successful verification, the gateway extracts the payload claims and propagates them downstream as trusted HTTP headers (`X-User-Id` and `X-User-Role`).
* **Downstream Implicit Trust:** Internal microservices must never parse or verify JWTs. They must implicitly trust the `X-User-Id` and `X-User-Role` headers provided by the gateway.
* **Header Sanitization:** The gateway must aggressively strip any identity headers injected by external clients before routing traffic downstream.