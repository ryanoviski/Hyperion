package com.hyperion.service;

import com.hyperion.model.Customer;
import com.hyperion.model.Product;
import com.hyperion.model.SaleItem;
import com.hyperion.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class ReportLoadIntegrationIT extends DatabaseIntegrationTest {

    private static final int SALE_COUNT = 150;

    @Test
    void producesSalesReportsPromptlyAfterAHigherVolumeOfTransactions() {
        Customer customer = createCustomer("Cliente de carga");
        Product product = createProduct("Produto de carga", new BigDecimal("9.99"));
        new StockService().registerEntry(product.getId(), SALE_COUNT, "Estoque para teste de carga");
        SaleService saleService = new SaleService();

        for (int index = 0; index < SALE_COUNT; index++) {
            saleService.finishSale(
                    customer,
                    List.of(new SaleItem(product.getId(), product.getName(), 1, product.getPrice())),
                    BigDecimal.ZERO,
                    index % 2 == 0 ? "PIX" : "Dinheiro"
            );
        }

        ReportService reportService = new ReportService();
        assertTimeout(Duration.ofSeconds(5), () -> {
            assertEquals(SALE_COUNT, reportService.getSalesSummary().getSalesCount());
            assertEquals(2, reportService.listSalesByPaymentMethod().size());
            assertEquals(1, reportService.listTopSellingProducts().size());
        });
    }
}
