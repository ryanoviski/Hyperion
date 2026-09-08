package com.hyperion.service;

import com.hyperion.model.PaymentMethodReport;
import com.hyperion.model.ProductSalesReport;
import com.hyperion.model.SalesReportSummary;
import com.hyperion.model.SalesReportFilter;
import com.hyperion.repository.SaleRepository;
import com.hyperion.exception.InvalidDateRangeException;

import java.time.LocalDate;
import java.util.List;

public class ReportService {

    private final SaleRepository saleRepository = new SaleRepository();

    public SalesReportSummary getSalesSummary() {
        return saleRepository.getSalesReportSummary();
    }

    public SalesReportSummary getSalesSummary(LocalDate startDate, LocalDate endDateExclusive) {
        validateDateRange(startDate, endDateExclusive);
        return saleRepository.getSalesReportSummary(startDate, endDateExclusive);
    }

    public SalesReportSummary getSalesSummary(SalesReportFilter filter) {
        validateDateRange(filter.startDate(), filter.endDateExclusive());
        return saleRepository.getSalesReportSummary(filter);
    }

    public List<PaymentMethodReport> listSalesByPaymentMethod() {
        return saleRepository.findSalesByPaymentMethod();
    }

    public List<PaymentMethodReport> listSalesByPaymentMethod(LocalDate startDate, LocalDate endDateExclusive) {
        validateDateRange(startDate, endDateExclusive);
        return saleRepository.findSalesByPaymentMethod(startDate, endDateExclusive);
    }

    public List<PaymentMethodReport> listSalesByPaymentMethod(SalesReportFilter filter) {
        validateDateRange(filter.startDate(), filter.endDateExclusive());
        return saleRepository.findSalesByPaymentMethod(filter);
    }

    public List<ProductSalesReport> listTopSellingProducts() {
        return saleRepository.findTopSellingProducts();
    }

    public List<ProductSalesReport> listTopSellingProducts(LocalDate startDate, LocalDate endDateExclusive) {
        validateDateRange(startDate, endDateExclusive);
        return saleRepository.findTopSellingProducts(startDate, endDateExclusive);
    }

    public List<ProductSalesReport> listTopSellingProducts(SalesReportFilter filter) {
        validateDateRange(filter.startDate(), filter.endDateExclusive());
        return saleRepository.findTopSellingProducts(filter);
    }

    public List<String> listCustomersForFilter() {
        return saleRepository.findReportCustomers();
    }

    public List<String> listCategoriesForFilter() {
        return saleRepository.findReportCategories();
    }

    public List<String> listSuppliersForFilter() {
        return saleRepository.findReportSuppliers();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDateExclusive) {
        if ((startDate == null) != (endDateExclusive == null)
                || (startDate != null && !startDate.isBefore(endDateExclusive))) {
            throw new InvalidDateRangeException();
        }
    }
}
