package com.hyperion.service;

import com.hyperion.model.Expense;
import com.hyperion.model.FinancialSummary;
import com.hyperion.repository.CreditInstallmentRepository;
import com.hyperion.repository.ExpenseRepository;
import com.hyperion.repository.SaleRepository;
import com.hyperion.exception.HyperionException;
import com.hyperion.exception.PartialOperationException;
import com.hyperion.exception.ValidationException;
import java.nio.file.Path;

import java.math.BigDecimal;
import java.util.List;

public class FinanceService {

    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final SaleRepository saleRepository = new SaleRepository();
    private final CreditInstallmentRepository creditInstallmentRepository = new CreditInstallmentRepository();
    private final AttachmentService attachmentService = new AttachmentService();

    public Long registerExpense(String description, String category, BigDecimal amount) {
        String normalizedDescription = normalize(description);

        if (normalizedDescription.isBlank()) {
            throw new ValidationException("Informe a descrição da despesa.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Informe um valor maior que zero.");
        }

        return expenseRepository.save(new Expense(
                normalizedDescription,
                normalize(category),
                amount
        ));
    }

    public void deleteExpense(Expense expense) {
        if (expense == null || expense.getId() == null) {
            throw new ValidationException("Selecione uma despesa para remover.");
        }

        attachmentService.deleteByEntity(AttachmentService.FINANCE_MODULE, expense.getId());
        try {
            expenseRepository.delete(expense.getId());
        } catch (HyperionException exception) {
            throw new PartialOperationException("Os anexos foram removidos, mas a despesa não pôde ser removida.", exception);
        }
    }

    public Long registerExpenseWithAttachment(String description, String category, BigDecimal amount, Path attachmentPath) {
        Long expenseId = registerExpense(description, category, amount);
        if (attachmentPath == null) {
            return expenseId;
        }
        try {
            attachmentService.attachFile(AttachmentService.FINANCE_MODULE, expenseId, attachmentPath);
            return expenseId;
        } catch (HyperionException exception) {
            throw new PartialOperationException("A despesa foi registrada, mas o comprovante não pôde ser salvo.", exception);
        }
    }

    public List<Expense> listLatestExpenses() {
        return expenseRepository.findLatest();
    }

    public FinancialSummary getSummary() {
        return new FinancialSummary(
                getTotalRealizedIncome(),
                expenseRepository.getTotalExpenses(),
                getCurrentMonthRealizedIncome(),
                expenseRepository.getCurrentMonthExpenses()
        );
    }

    private BigDecimal getTotalRealizedIncome() {
        return saleRepository.getTotalImmediateSales()
                .add(creditInstallmentRepository.getTotalPaidInstallments());
    }

    private BigDecimal getCurrentMonthRealizedIncome() {
        return saleRepository.getCurrentMonthImmediateSales()
                .add(creditInstallmentRepository.getCurrentMonthPaidInstallments());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
