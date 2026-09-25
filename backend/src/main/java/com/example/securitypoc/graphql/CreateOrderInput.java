package com.example.securitypoc.graphql;

import java.math.BigDecimal;

public record CreateOrderInput(String description, BigDecimal amount) {
}
