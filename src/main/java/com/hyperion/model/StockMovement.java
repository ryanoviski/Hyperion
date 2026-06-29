package com.hyperion.model;

import java.time.LocalDateTime;

public class StockMovement {

    private Long id;
    private Long productId;
    private String productName;
    private String type;
    private int quantity;
    private String notes;
    private LocalDateTime createdAt;

    public StockMovement(Long productId, String type, int quantity, String notes) {
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.notes = notes;
    }

    public StockMovement(
            Long id,
            Long productId,
            String productName,
            String type,
            int quantity,
            String notes,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.type = type;
        this.quantity = quantity;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
