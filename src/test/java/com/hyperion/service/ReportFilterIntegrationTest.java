package com.hyperion.service;

import com.hyperion.model.Customer;
import com.hyperion.model.PaymentMethodReport;
import com.hyperion.model.Product;
import com.hyperion.model.ProductSalesReport;
import com.hyperion.model.SaleItem;
import com.hyperion.model.SalesReportFilter;
import com.hyperion.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportFilterIntegrationTest extends DatabaseIntegrationTest {

    @Test
    void categoryAndSupplierFiltersUseHistoricalItemDataAndReconcileAllReportSections() {
        Customer customer = createCustomer("Marta");
        Product hygieneProduct = createProduct("Sabonete", new BigDecimal("20.00"), "Higiene", "Fornecedor A");
        Product stationeryProduct = createProduct("Caderno", new BigDecimal("10.00"), "Papelaria", "Fornecedor B");
        StockService stockService = new StockService();
        stockService.registerEntry(hygieneProduct.getId(), 1, "Estoque inicial");
        stockService.registerEntry(stationeryProduct.getId(), 1, "Estoque inicial");

        new SaleService().finishSale(
                customer,
                List.of(
                        new SaleItem(hygieneProduct.getId(), hygieneProduct.getName(), 1, hygieneProduct.getPrice()),
                        new SaleItem(stationeryProduct.getId(), stationeryProduct.getName(), 1, stationeryProduct.getPrice())
                ),
                new BigDecimal("1.00"),
                "PIX"
        );

        updateClassification(hygieneProduct, "Alterada", "Outro fornecedor");

        ReportService reportService = new ReportService();
        SalesReportFilter filter = new SalesReportFilter(null, null, "", "", "Higiene", "Fornecedor A");
        assertEquals(new BigDecimal("19.33"), reportService.getSalesSummary(filter).getTotalSales());
        assertEquals(1, reportService.getSalesSummary(filter).getSalesCount());

        List<PaymentMethodReport> paymentReports = reportService.listSalesByPaymentMethod(filter);
        assertEquals(1, paymentReports.size());
        assertEquals("PIX", paymentReports.getFirst().getPaymentMethod());
        assertEquals(new BigDecimal("19.33"), paymentReports.getFirst().getTotalAmount());

        List<ProductSalesReport> productReports = reportService.listTopSellingProducts(filter);
        assertEquals(1, productReports.size());
        assertEquals("Sabonete", productReports.getFirst().getProductName());
        assertEquals(new BigDecimal("19.33"), productReports.getFirst().getTotalAmount());
        assertEquals(List.of("Higiene", "Papelaria"), reportService.listCategoriesForFilter());
        assertEquals(List.of("Fornecedor A", "Fornecedor B"), reportService.listSuppliersForFilter());
    }

    private Product createProduct(String name, BigDecimal price, String category, String supplier) {
        ProductService productService = new ProductService();
        productService.createProduct(name, "", price, BigDecimal.ZERO, category, "", supplier);
        return productService.searchActiveProducts(name).stream()
                .filter(product -> name.equals(product.getName()))
                .findFirst()
                .orElseThrow();
    }

    private void updateClassification(Product product, String category, String supplier) {
        new ProductService().updateProduct(new Product(
                product.getId(), product.getName(), product.getDescription(), product.getPrice(), product.getCost(),
                product.getStockQuantity(), product.getMinimumStock(), category, product.getBarcode(), supplier,
                product.isActive(), product.getCreatedAt(), product.getUpdatedAt()
        ));
    }
}
