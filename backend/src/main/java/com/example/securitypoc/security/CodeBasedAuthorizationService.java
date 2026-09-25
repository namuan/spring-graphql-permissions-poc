package com.example.securitypoc.security;

import com.example.securitypoc.domain.OrderEntity;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class CodeBasedAuthorizationService implements AuthorizationService {
    private static final Map<Role, Set<Permission>> ROLE_PERMISSIONS = permissionsByRole();

    @Override
    public void requirePermission(CurrentUser user, Permission permission) {
        boolean allowed = user.roles().stream().anyMatch(role -> ROLE_PERMISSIONS.getOrDefault(role, Set.of()).contains(permission));
        if (!allowed) {
            throw new AuthorizationException();
        }
    }

    @Override
    public void requireOrderAccess(CurrentUser user, OrderEntity order, OrderAction action) {
        if (!user.tenantId().equals(order.getTenant().getId())) {
            throw new AuthorizationException();
        }

        Permission global = switch (action) {
            case READ -> Permission.ORDER_READ;
            case UPDATE -> Permission.ORDER_UPDATE;
            case CANCEL -> Permission.ORDER_CANCEL;
            case DELETE -> Permission.ORDER_DELETE;
        };
        if (hasPermission(user, global)) {
            return;
        }

        Permission own = switch (action) {
            case READ -> Permission.ORDER_READ_OWN;
            case UPDATE -> Permission.ORDER_UPDATE_OWN;
            case CANCEL -> Permission.ORDER_CANCEL_OWN;
            case DELETE -> null;
        };
        if (own != null && hasPermission(user, own) && user.appUserId().equals(order.getCustomer().getId())) {
            return;
        }

        throw new AuthorizationException();
    }

    private boolean hasPermission(CurrentUser user, Permission permission) {
        return user.roles().stream().anyMatch(role -> ROLE_PERMISSIONS.getOrDefault(role, Set.of()).contains(permission));
    }

    private static Map<Role, Set<Permission>> permissionsByRole() {
        EnumMap<Role, Set<Permission>> permissions = new EnumMap<>(Role.class);
        permissions.put(Role.CUSTOMER, EnumSet.of(
            Permission.ORDER_READ_OWN,
            Permission.ORDER_CREATE,
            Permission.ORDER_UPDATE_OWN,
            Permission.ORDER_CANCEL_OWN
        ));
        permissions.put(Role.SUPPORT, EnumSet.of(Permission.ORDER_READ, Permission.ORDER_UPDATE));
        permissions.put(Role.ADMIN, EnumSet.of(
            Permission.ORDER_READ,
            Permission.ORDER_CREATE,
            Permission.ORDER_UPDATE,
            Permission.ORDER_CANCEL,
            Permission.ORDER_DELETE,
            Permission.ORDER_READ_INTERNAL
        ));
        return Map.copyOf(permissions);
    }
}
