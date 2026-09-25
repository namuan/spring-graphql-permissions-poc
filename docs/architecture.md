# Architecture

The browser obtains a Keycloak access token using Authorization Code with PKCE. Spring Boot validates the JWT, including issuer, signature, expiry, and audience, and maps the subject to the application user.

The request path is:

```text
Keycloak → React → Spring Security → operation interceptor → resolver authorization → JPA → PostgreSQL
```

The operation interceptor parses the GraphQL document and selects the operation named by the request. It rejects anonymous, unknown, wrong-type, multi-root, fragment-expanded, and mismatched operations. Root-field validation uses the actual GraphQL field name, so aliases cannot change the capability.

Resolvers enforce the role permission and then the resource policy. Repository methods include the tenant ID for every order access path. A tenant administrator is not a global administrator.

`internalNotes` is resolved explicitly through a field mapping. Object authorization alone does not grant access to that field.

The application keeps the authorization boundary in `AuthorizationService` so a policy engine can replace the code-based implementation later.
