package com.hyperion.model;

import java.time.LocalDate;

public class CreditSalePlan {

    private final int installments;
    private final LocalDate firstDueDate;

    public CreditSalePlan(int installments, LocalDate firstDueDate) {
        this.installments = installments;
        this.firstDueDate = firstDueDate;
    }

    public int getInstallments() {
        return installments;
    }

    public LocalDate getFirstDueDate() {
        return firstDueDate;
    }
}
