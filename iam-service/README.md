# iam-service

Identity & Access Management microservice for the ed-tech platform.
Java 21 · Spring Boot 3.2.5 · MySQL · Spring Security · JJWT.

## Before running

1. Copy this `iam-service/` folder into your Maven multi-module monorepo as a module,
   or run it standalone.
2. Set environment variables for production (`application.yml` is the default profile
   and enforces TLS `VERIFY_IDENTITY` — it expects a real MySQL TLS certificate,
   e.g. a managed/cloud MySQL instance):
   - `MYSQL_HOST`, `MYSQL_PORT`, `IAM_SERVICE_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`
   - `JWT_SECRET` — a random Base64 secret, 256 bits or longer
   - `JWT_EXPIRATION_MS` (default 1 hour)
   - `COOKIE_DOMAIN`, `COOKIE_SECURE` (true in prod, requires HTTPS end-to-end), `COOKIE_SAME_SITE`
   - `CORS_ALLOWED_ORIGINS` — your Next.js client origin(s)
3. `ddl-auto: update` is for local dev only. Wire in Flyway/Liquibase before production.

## Local development

The default `application.yml` requires a TLS-secured MySQL server and will fail against
a plain local MySQL/Docker container. For local dev, use the `local` Spring profile
(`application-local.yml`), which relaxes SSL and cookie-security settings to match a
local, non-TLS MySQL container:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

With the `local` profile active, all `MYSQL_*` variables default sensibly for a local
Docker container (`localhost:3306`, `root`/`root`, database `iam_db`), so no environment
variables are required to just get it running locally.

## Run locally

```bash
mvn spring-boot:run
```

## Endpoints

| Method | Path                  | Auth required | Description                              |
|--------|-----------------------|----------------|-------------------------------------------|
| POST   | /auth-api/register    | No             | Create account, sets HttpOnly JWT cookie  |
| POST   | /auth-api/login       | No             | Verify credentials, sets HttpOnly JWT cookie |
| POST   | /auth-api/logout      | No             | Clears the auth cookie                    |
| GET    | /api/users/me         | Yes            | Returns the authenticated user's profile  |

## Notes

- The Gateway proxies `/auth-api/register` and `/auth-api/login` straight through to this
  service — this service is the sole issuer of the JWT.
- The JWT is delivered only via an `HttpOnly`, `Secure` cookie — never in the JSON body.
- Passwords are hashed with `BCryptPasswordEncoder` (strength 12).

## Trust boundary — IMPORTANT

`iam-service` does **not** verify JWTs itself. The API Gateway is the platform's sole
JWT validator: it checks the token's signature and expiry, then forwards the caller's
identity downstream as `X-User-Id` and `X-User-Role` headers, stripping any such headers
a caller tried to spoof. `GatewayHeaderAuthenticationFilter` in this service simply
trusts those two headers as given — it performs no cryptographic verification.

This means `iam-service` (and every other downstream service on this platform) **must
never be reachable directly from outside the internal network**. Only the Gateway should
be able to reach it. If this service is ever exposed publicly, anyone could set
`X-User-Id`/`X-User-Role` headers themselves and impersonate any user. Enforce this at
the network layer (e.g. Kubernetes NetworkPolicy, security groups, or a private subnet) —
application-level checks alone are not sufficient here.
