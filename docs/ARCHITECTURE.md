# Backend Architecture Documentation

This document outlines the architecture, responsibilities, and network flow of the core infrastructure modules within the monorepo.

## api-gateway
The API Gateway serves as the single entry point for all frontend client requests (Next.js) and acts as the central security checkpoint for the platform. Built on Spring Cloud Gateway and Spring Security Reactive, it operates completely statelessly to allow horizontal scaling.

**Core Responsibilities:**
*   **Reverse Proxy & Routing:** Intercepts all incoming HTTP traffic and routes it to the appropriate downstream microservice based on the URL path.
*   **JWT Extraction & Validation:** Reads the `jwt_token` from the `HttpOnly` cookie on incoming requests. It validates the digital signature and expiration of the token using the shared secret key.
*   **Header Mutation (The Trust Boundary):** Upon successful JWT validation, the Gateway extracts the user's ID and Role from the token payload, appends them as trusted HTTP headers (e.g., `X-User-Id`, `X-User-Role`), and strips any externally spoofed headers before forwarding the request to downstream services.
*   **Auth Bypass:** Explicitly allows unauthenticated traffic to pass directly to the `iam-service` for specific routes (`/auth-api/login`, `/auth-api/register`).

## iam-service
The Identity & Access Management (IAM) service is the single source of truth for user authentication and authorization data. Built with Spring Web and Spring Data JPA, it is the only service permitted to directly access the user credential tables in the MySQL database.

**Core Responsibilities:**
*   **User Provisioning:** Handles new student registrations, ensuring unique email constraints and persisting user profiles to MySQL.
*   **Credential Verification:** Uses `BCryptPasswordEncoder` to securely hash incoming passwords and verify them against the stored database hashes.
*   **Token Generation:** Upon successful authentication, constructs a JSON Web Token (JWT) encapsulating the user's identity and system role. 
*   **Secure Cookie Delivery:** Responsible for attaching the generated JWT to the HTTP response as an `HttpOnly`, `Secure` cookie, ensuring the Next.js frontend cannot access the token via JavaScript, thereby neutralizing Cross-Site Scripting (XSS) attack vectors.

# test-service

The `test-service` is a core microservice responsible for managing the complete lifecycle of test creation, categorization, question banking, multi-language localization, and secure publishing within a containerized microservices architecture[cite: 1].

---

### 1. High-Level Architecture & "Option A" Design
The service follows **Option A** architecture, which decouples the static question bank and test blueprints from active student attempts[cite: 1]:
* **Immutability Rule:** Once a mock test transitions out of `DRAFT` status (e.g., to `PUBLISHED`) and active student attempts begin, structural modifications (adding/removing questions or sections) are strictly blocked to protect data integrity for ongoing sessions[cite: 1].
* **Blueprint Caching:** Published test structures and answer keys are aggressively cached (via Redis) and version-controlled via database timestamps (`updated_at`) to ensure high-concurrency performance for student attempts[cite: 1].

## 3. Core Modules & Component Breakdown

### A. Admin Module (`/admin/**`)
Handles content management and administrative workflows with strict validation gates:
* **Categories & Series:** Groups mock tests under hierarchical categories that define mandatory language requirements (e.g., `["EN", "HI"]`)[cite: 1].
* **Question Bank Management:** Central repository for questions. Enforces strict schema validation matching `QuestionType` (MCQ, MULTI_CORRECT, NUMERICAL, SUBJECTIVE) and validates balanced LaTeX math delimiters (`$..$`) on save[cite: 1].
* **Publishing Engine:** Validates that all required language translations are present across all sections before converting a test status from `DRAFT` to `PUBLISHED`, locking constituent questions, and emitting a Kafka event[cite: 1].

### B. Public Catalog Module (`/catalog/**`)
Serves student-facing endpoints:
* **The Two-DTO Rule:** Strips out sensitive fields like `correctAnswerJson` and `explanation` entirely from student-facing payloads[cite: 1].
* **Localization Routing:** Resolves question text and options into the student's preferred language on-the-fly, returning a `404` if a mandatory translation is missing (no silent fallbacks)[cite: 1].

### C. Internal Module (`/internal/**`)
Serves downstream services (such as the `attempt-service`):
* **Secure Blueprints:** Exposes internal endpoints to fetch complete test structures including answer keys and point allocations[cite: 1].
* **Cluster Protection:** Guarded by a shared secret filter (`X-Service-Auth`) to prevent unauthorized access to grading keys[cite: 1].

---

### 4. Security & Interception Layer
* **Gateway-Integrated Routing:** Operates behind an API Gateway that terminates client sessions, validates tokens, and injects trusted identity headers (`x-user-id`, `X-User-Role`)[cite: 1].
* **Defensive Filtering:** Uses a custom `AdminRoleFilter` filter to block unauthorized requests across administrative endpoints[cite: 1].

---

### 5. Event-Driven & Inter-Service Flow
1. **Creation & Authoring:** Admins build categories, question banks, sections, and test structures via REST endpoints[cite: 1].
2. **Publishing:** Upon publishing, `AdminMockTestServiceImpl` locks mapped questions and triggers `KafkaPublisherService` to broadcast a `TestPublishedEvent`[cite: 1].
3. **Attempt Orchestration:** The `attempt-service` listens to test lifecycle events and fetches cached test blueprints securely via Spring `RestClient` using internal cluster authentication[cite: 1].
