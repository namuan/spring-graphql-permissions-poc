CREATE TABLE tenants (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(120) NOT NULL
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    keycloak_user_id VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(32) NOT NULL REFERENCES tenants(id)
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(32) NOT NULL REFERENCES tenants(id),
    customer_id BIGINT NOT NULL REFERENCES users(id),
    description VARCHAR(500) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'CANCELLED', 'COMPLETED')),
    internal_notes VARCHAR(1000)
);

CREATE INDEX orders_tenant_id_idx ON orders(tenant_id);
CREATE INDEX orders_customer_id_idx ON orders(customer_id);

INSERT INTO tenants(id, name) VALUES ('A', 'Tenant A'), ('B', 'Tenant B');

INSERT INTO users(id, keycloak_user_id, username, tenant_id) VALUES
    (1, '11111111-1111-1111-1111-111111111111', 'alice', 'A'),
    (2, '22222222-2222-2222-2222-222222222222', 'bob', 'A'),
    (3, '33333333-3333-3333-3333-333333333333', 'charlie', 'B'),
    (4, '44444444-4444-4444-4444-444444444444', 'support-a', 'A'),
    (5, '55555555-5555-5555-5555-555555555555', 'admin-a', 'A'),
    (6, '66666666-6666-6666-6666-666666666666', 'support-b', 'B'),
    (7, '77777777-7777-7777-7777-777777777777', 'admin-b', 'B');

INSERT INTO orders(id, tenant_id, customer_id, description, amount, status, internal_notes) VALUES
    (1, 'A', 1, 'Alice first order', 125.50, 'OPEN', 'Handle carefully'),
    (2, 'A', 1, 'Alice second order', 80.00, 'COMPLETED', NULL),
    (3, 'A', 2, 'Bob order', 42.75, 'OPEN', 'Priority customer'),
    (4, 'B', 3, 'Charlie order', 99.99, 'OPEN', 'Tenant B private note');

SELECT setval(pg_get_serial_sequence('users', 'id'), 8, false);
SELECT setval(pg_get_serial_sequence('orders', 'id'), 5, false);
