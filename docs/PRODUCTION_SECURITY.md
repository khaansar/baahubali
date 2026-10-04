# Production security architecture

Production is intentionally fail-closed: the gateway and IAM default to RS256
and need `JWT_PRIVATE_KEY_PATH` (IAM only), `JWT_PUBLIC_KEY_PATH` (gateway
only), `JWT_ISSUER`, and `JWT_AUDIENCE`. Store these in a managed secret store
and mount them as runtime secrets; never restore the prior shared JWT secret.

Deploy `docker-compose.production.yml` only as a topology reference. In a real
environment, use a workload platform and enforce these boundaries:

1. Terminate public TLS at the load balancer/WAF and expose only the gateway.
2. Require mutual TLS with workload identities for gateway-to-service and
   service-to-service traffic. Network segmentation is defence in depth, not
   identity. Remove `X-Service-Auth` compatibility headers after the mesh is
   enforcing identities.
3. Use managed Redis with TLS/ACLs and managed Kafka with TLS, SCRAM or mTLS,
   per-service principals, and topic ACLs. Kafka UI belongs in a separate
   operations plane behind SSO/VPN.
4. Put databases on private endpoints, use separate least-privilege database
   users per service, and retain TLS hostname verification.
5. Let only a payment/entitlement service decide access to paid tests. The
   current application deliberately rejects paid attempts until that service is
   integrated. Payment webhooks must verify provider signatures and persist a
   unique provider-event id before changing an entitlement.
6. Send access logs and audit logs to append-only, access-controlled storage.
   Run secret scanning, dependency/SAST scanning, container scanning, and
   infrastructure policy checks in CI before deployment.

Rotate the credentials that were previously present in `.env` before deploying.
They should be considered exposed even if the file was ignored by Git.
