package com.hyperion.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Sale {

    private Long id;
    private Long customerId;
    private String customerName;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private List<SaleItem> items;

    public Sale(
            Long customerId,
            String customerName,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total,
            String paymentMethod,
            List<SaleItem> items
    ) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.items = items;
    }

    public Sale(
            Long id,
            Long customerId,
            String customerName,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total,
            String paymentMethod,
            LocalDateTime createdAt,
            List<SaleItem> items
    ) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<SaleItem> getItems() {
        return items;
    }
}
