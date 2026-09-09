package com.hyperion.service;

import com.hyperion.exception.EntityInactiveException;
import com.hyperion.exception.InvalidCreditPlanException;
import com.hyperion.exception.InsufficientStockException;
import com.hyperion.exception.ValidationException;
import com.hyperion.model.Customer;
import com.hyperion.model.Product;
import com.hyperion.model.Sale;
import com.hyperion.model.SaleItem;
import com.hyperion.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Test
    void rejectsSaleForCustomerDeactivatedAfterSelectionWithoutChangingStock() {
        Customer customer = createCustomer("Daniela");
        Product product = createProduct("Mochila", new BigDecimal("80.00"));
        new StockService().registerEntry(product.getId(), 1, "Estoque inicial");
        new CustomerService().deactivateCustomer(customer.getId());

        assertThrows(EntityInactiveException.class, () -> new SaleService().finishSale(
                customer,
                List.of(new SaleItem(product.getId(), product.getName(), 1, product.getPrice())),
                BigDecimal.ZERO,
                "PIX"
        ));

        assertEquals(1, new ProductService().findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(0, new SaleService().listLatestSales(10).size());
    }

    @Test
    void rejectsZeroValueCreditSaleBeforePersistingAnyData() {
        Customer customer = createCustomer("Eduardo");
        Product product = createProduct("Brinde", new BigDecimal("5.00"));
        new StockService().registerEntry(product.getId(), 1, "Estoque inicial");

        assertThrows(InvalidCreditPlanException.class, () -> new SaleService().finishSale(
                customer,
                List.of(new SaleItem(product.getId(), product.getName(), 1, product.getPrice())),
                new BigDecimal("5.00"),
                "Crediário",
                new com.hyperion.model.CreditSalePlan(1, LocalDate.now().plusDays(30))
        ));

        assertEquals(1, new ProductService().findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(0, new SaleService().listLatestSales(10).size());
    }

    @Test
    void rejectsSaleWhoseCalculatedTotalWouldOverflowTheDatabaseMoneyRange() {
        Customer customer = createCustomer("Fernanda");
        BigDecimal maximumMoneyValue = new BigDecimal("92233720368547758.07");
        Product product = createProduct("Produto muito caro", maximumMoneyValue);
        new StockService().registerEntry(product.getId(), 2, "Estoque inicial");

        assertThrows(ValidationException.class, () -> new SaleService().finishSale(
                customer,
                List.of(new SaleItem(product.getId(), product.getName(), 2, product.getPrice())),
                BigDecimal.ZERO,
                "PIX"
        ));

        assertEquals(2, new ProductService().findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(0, new SaleService().listLatestSales(10).size());
    }
}
