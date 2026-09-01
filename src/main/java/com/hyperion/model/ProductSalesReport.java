package com.hyperion.model;

import java.math.BigDecimal;

public class ProductSalesReport {

    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantitySold;
    private final BigDecimal totalAmount;

    public ProductSalesReport(String productName, BigDecimal unitPrice, int quantitySold, BigDecimal totalAmount) {
        this.productName = productName;
        this.unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        this.quantitySold = quantitySold;
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantitySold() {
        return quantitySold;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
