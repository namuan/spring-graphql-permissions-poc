package com.example.securitypoc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenants")
public class TenantEntity {
    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 120)
    private String name;

    protected TenantEntity() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
