package com.hyperion.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditPayment(
        Long id,
        Long installmentId,
        BigDecimal amount,
        String receivedBy,
        String paymentMethod,
        String notes,
        LocalDateTime receivedAt
) {
}
