package com.hyperion.service;

import com.hyperion.model.Expense;
import com.hyperion.model.FinancialSummary;
import com.hyperion.repository.ExpenseRepository;
import com.hyperion.repository.SaleRepository;

import java.math.BigDecimal;
import java.util.List;

public class FinanceService {

    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final SaleRepository saleRepository = new SaleRepository();

    public void registerExpense(String description, String category, BigDecimal amount) {
        String normalizedDescription = normalize(description);

        if (normalizedDescription.isBlank()) {
            throw new IllegalArgumentException("Informe a descrição da despesa.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Informe um valor maior que zero.");
        }

        expenseRepository.save(new Expense(
                normalizedDescription,
                normalize(category),
                amount
        ));
    }

    public void deleteExpense(Expense expense) {
        if (expense == null || expense.getId() == null) {
            throw new IllegalArgumentException("Selecione uma despesa para remover.");
        }

        expenseRepository.delete(expense.getId());
    }

    public List<Expense> listLatestExpenses() {
        return expenseRepository.findLatest();
    }

    public FinancialSummary getSummary() {
        return new FinancialSummary(
                saleRepository.getTotalSales(),
                expenseRepository.getTotalExpenses(),
                saleRepository.getCurrentMonthSales(),
                expenseRepository.getCurrentMonthExpenses()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
