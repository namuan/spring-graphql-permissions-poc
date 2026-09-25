package com.example.securitypoc.graphql;

import java.math.BigDecimal;

public record UpdateOrderInput(String description, BigDecimal amount) {
}
