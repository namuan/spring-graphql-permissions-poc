package com.example.securitypoc.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    @Query("select u from UserEntity u join fetch u.tenant where u.keycloakUserId = :keycloakUserId")
    Optional<UserEntity> findByKeycloakUserId(@Param("keycloakUserId") String keycloakUserId);
}
