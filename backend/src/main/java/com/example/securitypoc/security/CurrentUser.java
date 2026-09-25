package com.example.securitypoc.security;

import java.util.Set;

public record CurrentUser(
    String subject,
    Long appUserId,
    String username,
    String tenantId,
    Set<Role> roles
) {
    public CurrentUser {
        roles = Set.copyOf(roles);
    }
}
