package com.example.securitypoc.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @EntityGraph(attributePaths = {"tenant", "customer"})
    Optional<OrderEntity> findByIdAndTenant_Id(Long id, String tenantId);

    @EntityGraph(attributePaths = {"tenant", "customer"})
    List<OrderEntity> findByTenant_IdAndCustomer_IdOrderByIdDesc(String tenantId, Long customerId);

    @EntityGraph(attributePaths = {"tenant", "customer"})
    List<OrderEntity> findByTenant_IdOrderByIdDesc(String tenantId);
}
