package com.hyperion.service;

import com.hyperion.exception.InstallmentAlreadyPaidException;
import com.hyperion.exception.SaleCancellationNotAllowedException;
import com.hyperion.model.CreditInstallment;
import com.hyperion.model.CreditPayment;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        installmentService.markAsPaid(openInstallments.getFirst(), "Caixa 1", "PIX", "Pagamento antecipado");

        assertEquals(2, installmentService.listOpenInstallments().size());
        assertEquals(1, installmentService.listPaidInstallments().size());
        assertEquals(new BigDecimal("33.33"), new FinanceService().getSummary().getTotalIncome());
        assertTrue(installmentService.listPaidInstallments().getFirst().getDueDate().isAfter(LocalDate.now()));
        CreditPayment payment = installmentService.getPayment(installmentService.listPaidInstallments().getFirst());
        assertEquals("Caixa 1", payment.receivedBy());
        assertEquals("PIX", payment.paymentMethod());
        assertEquals("Pagamento antecipado", payment.notes());
        assertEquals(new BigDecimal("66.67"), installmentService.getOpenBalanceByCustomer(customer.getId()));
        assertEquals(3, installmentService.listInstallmentsByCustomer(customer.getId()).size());

        assertThrows(InstallmentAlreadyPaidException.class, () ->
                installmentService.markAsPaid(openInstallments.getFirst(), "Caixa 1", "PIX", "Tentativa duplicada")
        );
        assertEquals(1, installmentService.listPaidInstallments().size());
        assertEquals(new BigDecimal("33.33"), new FinanceService().getSummary().getTotalIncome());
    }

    @Test
    void preventsCancellationAfterPaymentAndPreservesAllExistingFinancialState() {
        Customer customer = createCustomer("Lia");
        Product product = createProduct("Cadeira", new BigDecimal("90.00"));
        StockService stockService = new StockService();
        stockService.registerEntry(product.getId(), 1, "Estoque inicial");
        SaleService saleService = new SaleService();
        saleService.finishSale(
                customer,
                List.of(new SaleItem(product.getId(), product.getName(), 1, product.getPrice())),
                BigDecimal.ZERO,
                "Crediário",
                new CreditSalePlan(3, LocalDate.now().plusDays(10))
        );

        CreditInstallmentService installmentService = new CreditInstallmentService();
        installmentService.markAsPaid(installmentService.listOpenInstallments().getFirst(), "Caixa 1", "Dinheiro", "");
        assertThrows(SaleCancellationNotAllowedException.class, () ->
                saleService.cancelSale(saleService.listRecentSalesForManagement(1).getFirst(), "Tentativa após recebimento")
        );

        assertEquals(0, new ProductService().findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(1, saleService.listLatestSales(10).size());
        assertEquals(2, installmentService.listOpenInstallments().size());
        assertEquals(1, installmentService.listPaidInstallments().size());
        assertEquals(new BigDecimal("30.00"), new FinanceService().getSummary().getTotalIncome());
    }
}
