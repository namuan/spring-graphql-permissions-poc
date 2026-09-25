package com.example.securitypoc.security;

import com.example.securitypoc.domain.OrderEntity;

public interface AuthorizationService {
    void requirePermission(CurrentUser user, Permission permission);

    void requireOrderAccess(CurrentUser user, OrderEntity order, OrderAction action);
}
