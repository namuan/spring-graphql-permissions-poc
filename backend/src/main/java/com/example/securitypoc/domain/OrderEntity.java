package com.example.securitypoc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "internal_notes", length = 1000)
    private String internalNotes;

    protected OrderEntity() {
    }

    public OrderEntity(TenantEntity tenant, UserEntity customer, String description, BigDecimal amount) {
        this.tenant = tenant;
        this.customer = customer;
        this.description = description;
        this.amount = amount;
        this.status = OrderStatus.OPEN;
    }

    public Long getId() {
        return id;
    }

    public TenantEntity getTenant() {
        return tenant;
    }

    public UserEntity getCustomer() {
        return customer;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public void update(String description, BigDecimal amount) {
        if (description != null) {
            this.description = description;
        }
        if (amount != null) {
            this.amount = amount;
        }
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }
}
