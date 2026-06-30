package com.hyperion.service;

import com.hyperion.model.CreditInstallment;
import com.hyperion.repository.CreditInstallmentRepository;

import java.util.List;

public class CreditInstallmentService {

    private final CreditInstallmentRepository creditInstallmentRepository = new CreditInstallmentRepository();

    public List<CreditInstallment> listPendingAlerts() {
        return creditInstallmentRepository.findPendingAlerts();
    }
}
