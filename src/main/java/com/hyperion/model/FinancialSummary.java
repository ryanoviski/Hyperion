package com.hyperion.model;

import java.math.BigDecimal;

public class FinancialSummary {

    private final BigDecimal totalIncome;
    private final BigDecimal totalExpenses;
    private final BigDecimal currentBalance;
    private final BigDecimal monthlyIncome;
    private final BigDecimal monthlyExpenses;
    private final BigDecimal monthlyProfit;

    public FinancialSummary(
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            BigDecimal monthlyIncome,
            BigDecimal monthlyExpenses
    ) {
        this.totalIncome = valueOrZero(totalIncome);
        this.totalExpenses = valueOrZero(totalExpenses);
        this.currentBalance = this.totalIncome.subtract(this.totalExpenses);
        this.monthlyIncome = valueOrZero(monthlyIncome);
        this.monthlyExpenses = valueOrZero(monthlyExpenses);
        this.monthlyProfit = this.monthlyIncome.subtract(this.monthlyExpenses);
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public BigDecimal getMonthlyExpenses() {
        return monthlyExpenses;
    }

    public BigDecimal getMonthlyProfit() {
        return monthlyProfit;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
