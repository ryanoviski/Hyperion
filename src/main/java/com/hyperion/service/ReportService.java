package com.hyperion.service;

import com.hyperion.model.PaymentMethodReport;
import com.hyperion.model.ProductSalesReport;
import com.hyperion.model.SalesReportSummary;
import com.hyperion.repository.SaleRepository;

import java.time.LocalDate;
import java.util.List;

public class ReportService {

    private final SaleRepository saleRepository = new SaleRepository();

    public SalesReportSummary getSalesSummary() {
        return saleRepository.getSalesReportSummary();
    }

    public SalesReportSummary getSalesSummary(LocalDate startDate, LocalDate endDateExclusive) {
        return saleRepository.getSalesReportSummary(startDate, endDateExclusive);
    }

    public List<PaymentMethodReport> listSalesByPaymentMethod() {
        return saleRepository.findSalesByPaymentMethod();
    }

    public List<PaymentMethodReport> listSalesByPaymentMethod(LocalDate startDate, LocalDate endDateExclusive) {
        return saleRepository.findSalesByPaymentMethod(startDate, endDateExclusive);
    }

    public List<ProductSalesReport> listTopSellingProducts() {
        return saleRepository.findTopSellingProducts();
    }

    public List<ProductSalesReport> listTopSellingProducts(LocalDate startDate, LocalDate endDateExclusive) {
        return saleRepository.findTopSellingProducts(startDate, endDateExclusive);
    }
}
