package com.hyperion.service;

import com.hyperion.model.CreditInstallment;
import com.hyperion.model.CreditPayment;
import com.hyperion.repository.CreditInstallmentRepository;
import com.hyperion.exception.InstallmentAlreadyPaidException;
import com.hyperion.exception.ValidationException;

import java.math.BigDecimal;
import java.util.List;

public class CreditInstallmentService {

    private final CreditInstallmentRepository creditInstallmentRepository = new CreditInstallmentRepository();

    public List<CreditInstallment> listPendingAlerts() {
        return creditInstallmentRepository.findPendingAlerts();
    }

    public List<CreditInstallment> listOpenInstallments() {
        return creditInstallmentRepository.findOpenInstallments();
    }

    public List<CreditInstallment> listPaidInstallments() {
        return creditInstallmentRepository.findPaidInstallments();
    }

    public void markAsPaid(CreditInstallment installment) {
        markAsPaid(installment, "Operador", "Dinheiro", "");
    }

    public void markAsPaid(CreditInstallment installment, String receivedBy, String paymentMethod, String notes) {
        if (installment == null || installment.getId() == null) {
            throw new ValidationException("Selecione uma parcela para marcar como paga.");
        }

        if (!"OPEN".equals(installment.getStatus())) {
            throw new InstallmentAlreadyPaidException();
        }

        String normalizedReceiver = normalize(receivedBy);
        String normalizedMethod = normalize(paymentMethod);
        if (normalizedReceiver.isBlank()) {
            throw new ValidationException("Informe quem recebeu o pagamento.");
        }
        if (!List.of("Dinheiro", "PIX", "Cartão crédito", "Cartão débito").contains(normalizedMethod)) {
            throw new ValidationException("Selecione uma forma de recebimento válida.");
        }

        CreditPayment payment = new CreditPayment(
                null,
                installment.getId(),
                installment.getAmount(),
                normalizedReceiver,
                normalizedMethod,
                normalize(notes),
                null
        );
        if (!creditInstallmentRepository.markAsPaid(installment.getId(), payment)) {
            throw new InstallmentAlreadyPaidException();
        }
    }

    public CreditPayment getPayment(CreditInstallment installment) {
        if (installment == null || installment.getId() == null) {
            throw new ValidationException("Selecione uma parcela válida.");
        }
        return creditInstallmentRepository.findPaymentByInstallment(installment.getId());
    }

    public List<CreditPayment> listPaymentsByCustomer(Long customerId) {
        if (customerId == null) {
            return List.of();
        }
        return creditInstallmentRepository.findPaymentsByCustomer(customerId);
    }

    public BigDecimal getOpenBalanceByCustomer(Long customerId) {
        if (customerId == null) {
            return BigDecimal.ZERO;
        }
        return creditInstallmentRepository.getOpenBalanceByCustomer(customerId);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
