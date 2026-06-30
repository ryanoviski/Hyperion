package com.hyperion.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreditInstallment {

    private final Long id;
    private final Long saleId;
    private final Long customerId;
    private final String customerName;
    private final int installmentNumber;
    private final int totalInstallments;
    private final BigDecimal amount;
    private final LocalDate dueDate;
    private final String status;

    public CreditInstallment(
            Long id,
            Long saleId,
            Long customerId,
            String customerName,
            int installmentNumber,
            int totalInstallments,
            BigDecimal amount,
            LocalDate dueDate,
            String status
    ) {
        this.id = id;
        this.saleId = saleId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.installmentNumber = installmentNumber;
        this.totalInstallments = totalInstallments;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getSaleId() {
        return saleId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public int getInstallmentNumber() {
        return installmentNumber;
    }

    public int getTotalInstallments() {
        return totalInstallments;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getStatus() {
        return status;
    }
}
