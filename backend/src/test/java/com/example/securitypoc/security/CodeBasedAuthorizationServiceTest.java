package com.example.securitypoc.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.securitypoc.domain.OrderEntity;
import com.example.securitypoc.domain.TenantEntity;
import com.example.securitypoc.domain.UserEntity;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CodeBasedAuthorizationServiceTest {
    private final CodeBasedAuthorizationService service = new CodeBasedAuthorizationService();

    @Test
    void customerCanReadOwnOrder() {
        OrderEntity order = order("A", 10L);
        CurrentUser user = new CurrentUser("subject", 10L, "alice", "A", Set.of(Role.CUSTOMER));

        service.requireOrderAccess(user, order, OrderAction.READ);
    }

    @Test
    void customerCannotReadAnotherCustomersOrder() {
        OrderEntity order = order("A", 11L);
        CurrentUser user = new CurrentUser("subject", 10L, "alice", "A", Set.of(Role.CUSTOMER));

        assertThatThrownBy(() -> service.requireOrderAccess(user, order, OrderAction.READ))
            .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void administratorCannotCrossTenantBoundary() {
        OrderEntity order = order("B", 12L);
        CurrentUser user = new CurrentUser("subject", 12L, "admin-a", "A", Set.of(Role.ADMIN));

        assertThatThrownBy(() -> service.requireOrderAccess(user, order, OrderAction.READ))
            .isInstanceOf(AuthorizationException.class);
    }

    private OrderEntity order(String tenantId, Long customerId) {
        TenantEntity tenant = mock(TenantEntity.class);
        when(tenant.getId()).thenReturn(tenantId);
        UserEntity customer = mock(UserEntity.class);
        when(customer.getId()).thenReturn(customerId);
        OrderEntity order = mock(OrderEntity.class);
        when(order.getTenant()).thenReturn(tenant);
        when(order.getCustomer()).thenReturn(customer);
        return order;
    }
}
