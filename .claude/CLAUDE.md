# CLAUDE.md — Testing Platform ("baahubali" / clearit) backend

Spring Boot 4.0.7 / Spring Cloud 2025.1.2 / Java 21 Maven monorepo. Ed-tech mock-test platform.
Read `.claude/codebase-map.md` before exploring; known security issues are in `.claude/security-findings.md`
(update that file instead of re-auditing from scratch).

## Modules (root `pom.xml`)
| Module | Context path | Role |
|---|---|---|
| `common` | – | Shared auto-config: OpenAPI, Redis cache manager, `@CacheRefreshable` aspect |
| `api-gateway` | – (port 8080, only host-exposed service) | WebFlux Spring Cloud Gateway: JWT cookie validation, identity header injection, rate limiting |
| `iam-service` | `/auth-api` | Register/login/refresh/OAuth2 (Google)/users/audit logs, JWT issuance (RS256) |
| `test-service` | `/tests-api` | Categories, series, mock tests, questions, catalog, admin CRUD, Cloudinary upload intents |
| `attempt-service` | `/attempts-api` | Test attempts (Redis hot state + MySQL), SSE, review, dashboard |
| `analytics-service` | `/analytics-api` | Reports, leaderboard, performance (Kafka consumers) |
| `community-service` | `/community-api` | Reviews (moderation), FAQs |
| `notification-service` | `/notifications-api` | Email + in-app (SSE) notifications from Kafka |
| `payment-service` | `/payments-api` | Razorpay orders, webhooks, coupons, refunds, entitlements |

Infra: MySQL (external, TLS), Redis, Kafka. Each service has Flyway migrations in `src/main/resources/db`.

## Trust model (important for any auth/security work)
1. Gateway (`JwtAuthenticationGlobalFilter`) validates the `ACCESS_TOKEN` HttpOnly cookie (RS256, issuer/audience checked,
   `sessionId` claim must match Redis `user:session:<userId>` → single active session per user).
2. Gateway strips client `X-User-Id`/`X-User-Role`/`X-Service-*` and sets `X-User-Id`, `X-User-Role`,
   `X-Service-Caller: api-gateway`, `X-Service-Auth: <API_GATEWAY_SECRET>` on every forwarded request (also public ones).
3. Downstream services trust `X-User-Id` / `X-User-Role` headers with no cryptographic check. Authorization (admin role, ownership)
   is done per service from those headers. `InternalAuthInterceptor` (shared-secret per caller, `internal.auth.allowed-clients`)
   guards `/internal/**` — in some services `/**`.
4. `InternalRouteBlockerFilter` blocks any gateway path containing `/internal/`.
5. Public gateway paths are listed in `api-gateway/src/main/resources/application.yml` (`gateway.security.public-paths`).
   `SecurityConfig` is `anyExchange().permitAll()` — real enforcement is the global filter, not Spring Security.

## Commands
```bash
./mvnw -B -ntp clean verify                 # build + all tests (what CI runs)
./mvnw -pl iam-service -am test             # one module (+ common)
docker-compose up -d --build                # needs .env (see .env.production.example) + secrets/jwt-{private,public}.pem
```
Dockerfile builds one service via `--build-arg SERVICE=<module>`; runs as non-root.

## Configuration
- One root `.env` is injected into every container via `env_file` (untracked; `.env.production.example` is the template).
- Never read/print `.env` or `secrets/*.pem`; they contain live credentials (denied in `.claude/settings.json`).
- Config is `${ENV_VAR}` in each `application.yml`; shared keys: `INTERNAL`/`*_SERVICE_SECRET`, `JWT_*`, `COOKIE_*`, `CORS_ALLOWED_ORIGINS`, `REDIS_*`, `MYSQL_*`.

## Conventions
- Package roots: `com.example.<service>` (`iam`, `testservice`, `attemptservice`, `analyticsservice`, `communityservice`, `notification`, `payment`, `apigateway`).
- Controllers read identity from `@RequestHeader("X-User-Id")` (or `CurrentUserResolver` in payment, `UserContextHolder` in community).
- Responses wrapped in a per-service `ApiResponse`; Kafka for async events; Redis caching via `common` cache config.
- Tests are sparse: attempt/community/notification have none; payment and analytics have the most.

## Workflow rules (from user's global CLAUDE.md)
Small sections; one separate Git commit per section; commit message exactly ONE line; no secrets in commits;
only implement what is requested; report checks run and results.
