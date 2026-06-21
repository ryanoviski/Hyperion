package com.hyperion.model;

import java.time.LocalDateTime;

public class Customer {

    private Long id;
    private String name;
    private String document;
    private String phone;
    private String email;
    private String address;
    private String notes;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Customer(String name, String document, String phone, String email, String address, String notes) {
        this.name = name;
        this.document = document;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.notes = notes;
        this.active = true;
    }

    public Customer(
            Long id,
            String name,
            String document,
            String phone,
            String email,
            String address,
            String notes,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.document = document;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.notes = notes;
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

    public String getDocument() {
        return document;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getNotes() {
        return notes;
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
