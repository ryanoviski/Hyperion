package com.hyperion.service;

import com.hyperion.model.CreditSalePlan;
import com.hyperion.model.Customer;
import com.hyperion.model.FinancialSummary;
import com.hyperion.model.Product;
import com.hyperion.model.ProductSalesReport;
import com.hyperion.model.Sale;
import com.hyperion.model.SaleItem;
import com.hyperion.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancialIntegrityIntegrationTest extends DatabaseIntegrationTest {

    @Test
    void calculatesMonthlyNetProfitFromCompletedCreditSalesAndReversesItOnCancellation() {
        Customer customer = createCustomer("Marina");
        Product product = createProduct("Cafeteira", new BigDecimal("100.00"), new BigDecimal("60.00"));
        StockService stockService = new StockService();
        stockService.registerEntry(product.getId(), 1, "Estoque inicial");

        SaleService saleService = new SaleService();
        saleService.finishSale(
                customer,
                List.of(new SaleItem(product.getId(), product.getName(), 1, product.getPrice())),
                BigDecimal.ZERO,
                "Crediário",
                new CreditSalePlan(3, LocalDate.now().plusDays(30))
        );

        FinancialSummary afterSale = new FinanceService().getSummary();
        assertEquals(new BigDecimal("0.00"), afterSale.getTotalIncome(), "Venda a prazo ainda não é entrada de caixa.");
        assertEquals(new BigDecimal("40.00"), afterSale.getMonthlyProfit(), "Lucro deve considerar receita menos custo da venda.");

        Sale sale = saleService.listRecentSalesForManagement(1).getFirst();
        saleService.cancelSale(sale, "Cliente desistiu da compra");

        FinancialSummary afterCancellation = new FinanceService().getSummary();
        assertEquals(new BigDecimal("0.00"), afterCancellation.getMonthlyProfit());
        assertEquals(1, new ProductService().findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(0, new CreditInstallmentService().listOpenInstallments().size());
        assertEquals(0, new ReportService().getSalesSummary().getSalesCount());
    }

    @Test
    void keepsCostSnapshotsAndReconcilesDiscountedProductTotalsWithTheSalesReport() {
        Customer customer = createCustomer("Roberto");
        Product firstProduct = createProduct("Produto A", new BigDecimal("20.00"), new BigDecimal("12.00"));
        Product secondProduct = createProduct("Produto B", new BigDecimal("10.00"), new BigDecimal("4.00"));
        StockService stockService = new StockService();
        stockService.registerEntry(firstProduct.getId(), 1, "Estoque inicial");
        stockService.registerEntry(secondProduct.getId(), 1, "Estoque inicial");

        new SaleService().finishSale(
                customer,
                List.of(
                        new SaleItem(firstProduct.getId(), firstProduct.getName(), 1, firstProduct.getPrice()),
                        new SaleItem(secondProduct.getId(), secondProduct.getName(), 1, secondProduct.getPrice())
                ),
                new BigDecimal("1.00"),
                "PIX"
        );
        new FinanceService().registerExpense("Taxa de entrega", "Operação", new BigDecimal("3.00"));

        FinancialSummary beforeProductUpdate = new FinanceService().getSummary();
        assertEquals(new BigDecimal("29.00"), beforeProductUpdate.getTotalIncome());
        assertEquals(new BigDecimal("26.00"), beforeProductUpdate.getCurrentBalance());
        assertEquals(new BigDecimal("10.00"), beforeProductUpdate.getMonthlyProfit());

        updateProductCost(firstProduct, new BigDecimal("19.00"));
        updateProductCost(secondProduct, new BigDecimal("8.00"));

        assertEquals(new BigDecimal("10.00"), new FinanceService().getSummary().getMonthlyProfit(),
                "A alteração futura de custo não pode reescrever o lucro da venda já concluída.");

        ReportService reportService = new ReportService();
        assertEquals(new BigDecimal("29.00"), reportService.getSalesSummary().getTotalSales());
        Map<String, ProductSalesReport> products = reportService.listTopSellingProducts().stream()
                .collect(Collectors.toMap(ProductSalesReport::getProductName, report -> report));
        assertEquals(new BigDecimal("19.33"), products.get("Produto A").getTotalAmount());
        assertEquals(new BigDecimal("9.67"), products.get("Produto B").getTotalAmount());
        assertEquals(new BigDecimal("29.00"), products.values().stream()
                .map(ProductSalesReport::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void reversesImmediateSaleCashAndProfitWithoutReversingManualExpenses() {
        Customer customer = createCustomer("Sofia");
        Product product = createProduct("Liquidificador", new BigDecimal("100.00"), new BigDecimal("60.00"));
        new StockService().registerEntry(product.getId(), 1, "Estoque inicial");
        SaleService saleService = new SaleService();

        saleService.finishSale(
                customer,
                List.of(new SaleItem(product.getId(), product.getName(), 1, product.getPrice())),
                BigDecimal.ZERO,
                "Dinheiro"
        );
        new FinanceService().registerExpense("Frete", "Operação", new BigDecimal("10.00"));

        FinancialSummary afterSale = new FinanceService().getSummary();
        assertEquals(new BigDecimal("90.00"), afterSale.getCurrentBalance());
        assertEquals(new BigDecimal("30.00"), afterSale.getMonthlyProfit());

        saleService.cancelSale(saleService.listRecentSalesForManagement(1).getFirst(), "Produto devolvido");

        FinancialSummary afterCancellation = new FinanceService().getSummary();
        assertEquals(new BigDecimal("-10.00"), afterCancellation.getCurrentBalance());
        assertEquals(new BigDecimal("-10.00"), afterCancellation.getMonthlyProfit());
        assertEquals(1, new ProductService().findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(0, new ReportService().getSalesSummary().getSalesCount());
    }

    private Product createProduct(String name, BigDecimal price, BigDecimal cost) {
        ProductService productService = new ProductService();
        productService.createProduct(name, "", price, cost, "", "", "");
        return productService.searchActiveProducts(name).stream()
                .filter(product -> name.equals(product.getName()))
                .findFirst()
                .orElseThrow();
    }

    private void updateProductCost(Product product, BigDecimal cost) {
        new ProductService().updateProduct(new Product(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                cost,
                product.getStockQuantity(),
                product.getMinimumStock(),
                product.getCategory(),
                product.getBarcode(),
                product.getSupplier(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        ));
    }
}
