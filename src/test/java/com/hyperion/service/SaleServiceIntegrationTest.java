package com.hyperion.service;

import com.hyperion.exception.InsufficientStockException;
import com.hyperion.exception.ValidationException;
import com.hyperion.model.Customer;
import com.hyperion.model.Product;
import com.hyperion.model.Sale;
import com.hyperion.model.SaleItem;
import com.hyperion.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SaleServiceIntegrationTest extends DatabaseIntegrationTest {

    @Test
    void completesSaleWithDiscountAndUpdatesStock() {
        Customer customer = createCustomer("Ana");
        Product product = createProduct("Caderno", new BigDecimal("10.00"));
        StockService stockService = new StockService();
        stockService.registerEntry(product.getId(), 5, "Estoque inicial");

        new SaleService().finishSale(
                customer,
                List.of(new SaleItem(product.getId(), product.getName(), 2, product.getPrice())),
                new BigDecimal("2.50"),
                "PIX"
        );

        Sale sale = new SaleService().listLatestSales(1).getFirst();
        assertEquals(new BigDecimal("20.00"), sale.getSubtotal());
        assertEquals(new BigDecimal("2.50"), sale.getDiscount());
        assertEquals(new BigDecimal("17.50"), sale.getTotal());
        assertEquals(3, new ProductService().findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(1, stockService.listLatestMovements().stream()
                .filter(movement -> "OUT".equals(movement.getType()))
                .count());
    }

    @Test
    void rejectsDiscountGreaterThanSubtotalWithoutChangingStock() {
        Customer customer = createCustomer("Bruno");
        Product product = createProduct("Caneta", new BigDecimal("4.00"));
        new StockService().registerEntry(product.getId(), 2, "Estoque inicial");

        assertThrows(ValidationException.class, () -> new SaleService().finishSale(
                customer,
                List.of(new SaleItem(product.getId(), product.getName(), 1, product.getPrice())),
                new BigDecimal("4.01"),
                "Dinheiro"
        ));

        assertEquals(2, new ProductService().findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(0, new SaleService().listLatestSales(10).size());
    }

    @Test
    void rejectsSaleWhenStockIsInsufficient() {
        Customer customer = createCustomer("Carla");
        Product product = createProduct("Lápis", new BigDecimal("3.00"));
        new StockService().registerEntry(product.getId(), 1, "Estoque inicial");

        assertThrows(InsufficientStockException.class, () -> new SaleService().finishSale(
                customer,
                List.of(new SaleItem(product.getId(), product.getName(), 2, product.getPrice())),
                BigDecimal.ZERO,
                "Cartão débito"
        ));

        assertEquals(1, new ProductService().findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(0, new SaleService().listLatestSales(10).size());
    }
}
