# Demo script

1. Start dependencies, backend, and frontend.
2. Log in as `alice`.
3. Show `myOrders` and identify Alice's order.
4. Use the playground to request order `3` as Alice. It is Bob's order.
5. Request order `4` as Alice. It belongs to tenant B.
6. Run an anonymous operation and an unknown operation.
7. Run the malicious document named `GetOrder` with `orders` as its root field. With full structural validation it is rejected; with `NAME_ONLY_OPERATIONS_ENABLED=true` it demonstrates the name-only limitation for a user who otherwise has access to that capability.
8. Request `internalNotes` as Alice and observe the field error.
9. Log out and log in as `admin-a` to read tenant A internal notes.
10. Request tenant B data as `admin-a` and observe that ADMIN is not cross-tenant.
