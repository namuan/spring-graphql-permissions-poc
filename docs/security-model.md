# Security model

| Layer | Enforcement |
|---|---|
| Authentication | Keycloak-issued signed JWT and Spring Security resource server |
| Operation contract | Named operation allow-list in `OperationSecurityInterceptor` |
| GraphQL capability | Parsed operation type and exactly one expected root field |
| Permission | `AuthorizationService.requirePermission` |
| Tenant | Tenant-scoped repository methods and resource checks |
| Ownership | Customer permissions require the order customer to equal the application user |
| Sensitive field | Explicit `Order.internalNotes` resolver permission |

The operation contract is not treated as authorization. A client can rename an operation and the structural policy still checks the actual root field. Resource and field checks remain authoritative for every request.

The development `NAME_ONLY_OPERATIONS_ENABLED` setting exists only to demonstrate the name-only attack. It must remain false in the default configuration.

GraphQL resolver errors may be returned with HTTP 200 according to the GraphQL over HTTP convention. Clients must inspect both the HTTP status and the response `errors` array. Authentication failures are handled by the resource-server entry point.
