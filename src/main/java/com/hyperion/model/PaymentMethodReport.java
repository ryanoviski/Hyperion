package com.hyperion.model;

import java.math.BigDecimal;

public class PaymentMethodReport {

    private final String paymentMethod;
    private final int salesCount;
    private final BigDecimal totalAmount;

    public PaymentMethodReport(String paymentMethod, int salesCount, BigDecimal totalAmount) {
        this.paymentMethod = paymentMethod;
        this.salesCount = salesCount;
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public int getSalesCount() {
        return salesCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
