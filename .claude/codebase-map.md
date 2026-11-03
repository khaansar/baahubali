# Codebase map (so it need not be re-scanned)

Root: `pom.xml` (modules), `Dockerfile` (per-service via `SERVICE` arg), `docker-compose.yml` / `docker-compose.production.yml`
(near-identical; compose has Redis, Kafka (KRaft, PLAINTEXT), 8 services, optional `kafka-ui` under profile `debug`; MySQL is external),
`.github/workflows/ci_runs.yml` (build+test only), `docs/` (ARCHITECTURE, PAYMENT_SERVICE, PRODUCTION_SECURITY, refresh-token research).

## api-gateway (`com.example.apigateway`)
- `filter/JwtAuthenticationGlobalFilter` (order -100) → `security/{PublicRouteMatcher,CookieTokenExtractor,JwtTokenValidator,IdentityHeaderPropagator}`
- `filter/CookieOriginProtectionFilter` (Origin allow-list for unsafe methods with cookies, skipped on public paths)
- `filter/InternalRouteBlockerFilter` (`path.contains("/internal/")` → 404), `SecurityResponseHeadersFilter`, `RateLimitResponseDecoratorFilter`
- `config/SecurityConfig` (CORS, CSRF off, permitAll), `config/RateLimiterConfig` (key = route + user id or remote IP)
- Routes + rate limits + public paths + JWT settings: `src/main/resources/application.yml`

## iam-service (`com.example.iam`, context `/auth-api`)
- `controller/{AuthController,UserController,AuditLogController}`, `controller/internal/InternalUserController`
- `service/impl/AuthServiceImpl` (register/login/refresh/logout/forgot/reset), `service/impl/UserServiceImpl` (profile, change password)
- `security/{JwtService,RefreshTokenService (Redis, rotating families),AuthCookieFactory,GatewayHeaderAuthenticationFilter,OAuth2*}`
- `config/SecurityConfig` (Spring Security: `/admin/**`+`/users` need ADMIN, `/internal/**` permitAll but guarded by interceptor)
- Avatars: `AvatarUploadService` (signed Cloudinary intent), `AvatarProfileService` (verifies asset via Cloudinary API)

## test-service (`com.example.testservice`, `/tests-api`)
- `controller/Admin*Controller` (`/admin/**`, `AdminRoleInterceptor` checks `X-User-Role == ADMIN`), `Public*`/`CatalogController` (public),
  `Internal*Controller` (service-to-service), `QuestionImageUploadController`
- `config/{WebConfig,InternalAuthInterceptor,AuditLoggingInterceptor}`; actuator exposes health,info,metrics,prometheus

## attempt-service (`com.example.attemptservice`, `/attempts-api`)
- `controller/{AttemptController,AttemptDashboardController,InternalAttemptController}`; `service/AttemptService` (≈1100 lines;
  `verifyOwnership` on every attempt-id route); `filter/GatewaySecurityFilter` (requires non-empty `X-User-Id`); Redis hash + flush worker + outbox

## analytics-service (`com.example.analyticsservice`, `/analytics-api`)
- `web/controller/AnalyticsController`, `web/service/ReportCacheService` (ownership checked), Kafka listeners in `core/event`

## community-service (`com.example.communityservice`, `/community-api`)
- `controller/{ReviewController,FaqController}`, `config/{UserContextFilter,AdminAuthorizationInterceptor,InternalAuthInterceptor}`

## notification-service (`com.example.notification`, `/notifications-api`)
- `controller/{StudentNotificationController (/notifications, SSE),NotificationController (/internal)}`, Kafka consumer → email/in-app

## payment-service (`com.example.payment`, `/payments-api`)
- `config/{InternalAuthFilter (ALL paths except /actuator),CurrentUserResolver}`, controllers (user, `admin/*`, `Internal*`, `WebhookController`)
- `service/{CheckoutService,OrderService,PaymentService,WebhookProcessor}`, `gateway/razorpay/RazorpayPaymentGateway` (HMAC-SHA256, constant-time)

## common
- `cache/config/SharedCacheAutoConfiguration` (Jackson default typing for Redis values), `cache/{aspect,security}` (`X-Cache-Refresh-Token`), `openapi/`
