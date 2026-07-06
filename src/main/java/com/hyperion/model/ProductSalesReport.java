package com.hyperion.model;

import java.math.BigDecimal;

public class ProductSalesReport {

    private final String productName;
    private final int quantitySold;
    private final BigDecimal totalAmount;

    public ProductSalesReport(String productName, int quantitySold, BigDecimal totalAmount) {
        this.productName = productName;
        this.quantitySold = quantitySold;
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantitySold() {
        return quantitySold;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
