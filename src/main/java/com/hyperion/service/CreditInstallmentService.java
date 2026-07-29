package com.hyperion.service;

import com.hyperion.model.CreditInstallment;
import com.hyperion.repository.CreditInstallmentRepository;

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
        if (installment == null || installment.getId() == null) {
            throw new IllegalArgumentException("Selecione uma parcela para marcar como paga.");
        }

        if (!"OPEN".equals(installment.getStatus())) {
            throw new IllegalArgumentException("Apenas parcelas em aberto podem ser marcadas como pagas.");
        }

        creditInstallmentRepository.markAsPaid(installment.getId());
    }
}
