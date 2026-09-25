package com.example.securitypoc.security;

import com.example.securitypoc.domain.UserEntity;
import com.example.securitypoc.domain.UserRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CurrentUser requireCurrentUser(Jwt jwt) {
        String subject = jwt.getSubject();
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (subject == null || tenantId == null) {
            throw new AuthorizationException();
        }

        UserEntity user = userRepository.findByKeycloakUserId(subject)
            .orElseThrow(AuthorizationException::new);
        if (!tenantId.equals(user.getTenant().getId())) {
            throw new AuthorizationException();
        }

        String username = jwt.getClaimAsString("preferred_username");
        return new CurrentUser(
            subject,
            user.getId(),
            username == null ? user.getUsername() : username,
            tenantId,
            roles(jwt)
        );
    }

    private Set<Role> roles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        Set<String> names = realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> values)
            ? Set.of()
            : values.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
        return names.stream()
            .map(name -> {
                try {
                    return Role.valueOf(name);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());
    }
}
