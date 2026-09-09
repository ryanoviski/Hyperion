package com.hyperion.service;

import com.hyperion.exception.EntityInactiveException;
import com.hyperion.exception.InsufficientStockException;
import com.hyperion.exception.ValidationException;
import com.hyperion.model.Product;
import com.hyperion.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    @Test
    void preventsEntriesThatWouldExceedTheSupportedStockRange() {
        Product product = createProduct("Estoque máximo", new BigDecimal("10.00"));
        StockService stockService = new StockService();

        stockService.registerEntry(product.getId(), Integer.MAX_VALUE, "Carga inicial");

        assertThrows(ValidationException.class,
                () -> stockService.registerEntry(product.getId(), 1, "Excede o limite"));
        assertEquals(Integer.MAX_VALUE,
                new ProductService().findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(1, stockService.listLatestMovements().size());
    }

    @Test
    void serializesConcurrentEntriesWithoutLosingStockMovements() throws Exception {
        Product product = createProduct("Movimentação concorrente", new BigDecimal("10.00"));
        int movementCount = 20;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> movements = new ArrayList<>();

        try {
            for (int index = 0; index < movementCount; index++) {
                movements.add(executor.submit(() -> {
                    start.await();
                    new StockService().registerEntry(product.getId(), 1, "Entrada concorrente");
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> movement : movements) {
                movement.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(movementCount,
                new ProductService().findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(movementCount, new StockService().listLatestMovements().size());
    }
}
