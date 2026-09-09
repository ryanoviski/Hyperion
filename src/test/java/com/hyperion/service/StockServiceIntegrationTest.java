package com.hyperion.service;

import com.hyperion.exception.EntityInactiveException;
import com.hyperion.exception.InsufficientStockException;
import com.hyperion.exception.ValidationException;
import com.hyperion.model.Product;
import com.hyperion.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockServiceIntegrationTest extends DatabaseIntegrationTest {

    @Test
    void recordsValidMovementsAndRollsBackRejectedExits() {
        Product product = createProduct("Teclado", new BigDecimal("100.00"));
        StockService stockService = new StockService();

        stockService.registerEntry(product.getId(), 3, "Recebimento inicial");
        stockService.registerExit(product.getId(), 2, "Ajuste autorizado");

        assertEquals(1, new ProductService().findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(2, stockService.listLatestMovements().size());

        assertThrows(InsufficientStockException.class,
                () -> stockService.registerExit(product.getId(), 2, "Saída acima do disponível"));
        assertEquals(1, new ProductService().findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(2, stockService.listLatestMovements().size());

        assertThrows(ValidationException.class, () -> stockService.registerEntry(product.getId(), 0, "Quantidade inválida"));
        assertEquals(2, stockService.listLatestMovements().size());
    }

    @Test
    void rejectsManualMovementsForInactiveProducts() {
        Product product = createProduct("Mouse", new BigDecimal("50.00"));
        new ProductService().deactivateProduct(product.getId());

        assertThrows(EntityInactiveException.class,
                () -> new StockService().registerEntry(product.getId(), 1, "Tentativa inválida"));
        assertEquals(0, new ProductService().findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(0, new StockService().listLatestMovements().size());
    }
}
