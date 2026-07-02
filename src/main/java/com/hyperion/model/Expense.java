package com.hyperion.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Expense {

    private Long id;
    private String description;
    private String category;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public Expense(String description, String category, BigDecimal amount) {
        this.description = description;
        this.category = category;
        this.amount = amount;
    }

    public Expense(Long id, String description, String category, BigDecimal amount, LocalDateTime createdAt) {
        this.id = id;
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
