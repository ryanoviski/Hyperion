package com.hyperion.model;

import java.time.LocalDateTime;

public class Company {

    private Long id;
    private String name;
    private String ownerName;
    private LocalDateTime createdAt;

    public Company(String name, String ownerName) {
        this.name = name;
        this.ownerName = ownerName;
    }

    public Company(Long id, String name, String ownerName, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.ownerName = ownerName;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
