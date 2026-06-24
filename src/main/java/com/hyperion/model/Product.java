package com.hyperion.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal cost;
    private int stockQuantity;
    private String category;
    private String barcode;
    private String supplier;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product(
            String name,
            String description,
            BigDecimal price,
            BigDecimal cost,
            int stockQuantity,
            String category,
            String barcode,
            String supplier
    ) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.cost = cost;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.barcode = barcode;
        this.supplier = supplier;
        this.active = true;
    }

    public Product(
            Long id,
            String name,
            String description,
            BigDecimal price,
            BigDecimal cost,
            int stockQuantity,
            String category,
            String barcode,
            String supplier,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.cost = cost;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.barcode = barcode;
        this.supplier = supplier;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public String getCategory() {
        return category;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getSupplier() {
        return supplier;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
