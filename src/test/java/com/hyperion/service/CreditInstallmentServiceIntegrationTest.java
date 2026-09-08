package com.hyperion.service;

import com.hyperion.model.CreditInstallment;
import com.hyperion.model.CreditSalePlan;
import com.hyperion.model.Customer;
import com.hyperion.model.Product;
import com.hyperion.model.SaleItem;
import com.hyperion.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditInstallmentServiceIntegrationTest extends DatabaseIntegrationTest {

    @Test
    void createsCreditInstallmentsAndRegistersPayment() {
        Customer customer = createCustomer("Diego");
        Product product = createProduct("Mochila", new BigDecimal("100.00"));
        new StockService().registerEntry(product.getId(), 1, "Estoque inicial");

        new SaleService().finishSale(
                customer,
                List.of(new SaleItem(product.getId(), product.getName(), 1, product.getPrice())),
                BigDecimal.ZERO,
                "Crediário",
                new CreditSalePlan(3, LocalDate.now().plusDays(10))
        );

        CreditInstallmentService installmentService = new CreditInstallmentService();
        List<CreditInstallment> openInstallments = installmentService.listOpenInstallments();
        assertEquals(3, openInstallments.size());
        assertEquals(new BigDecimal("100.00"), openInstallments.stream()
                .map(CreditInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        installmentService.markAsPaid(openInstallments.getFirst());

        assertEquals(2, installmentService.listOpenInstallments().size());
        assertEquals(1, installmentService.listPaidInstallments().size());
        assertEquals(new BigDecimal("33.33"), new FinanceService().getSummary().getTotalIncome());
        assertTrue(installmentService.listPaidInstallments().getFirst().getDueDate().isAfter(LocalDate.now()));
    }
}
