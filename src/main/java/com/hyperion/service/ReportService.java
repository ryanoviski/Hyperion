package com.hyperion.service;

import com.hyperion.model.PaymentMethodReport;
import com.hyperion.model.ProductSalesReport;
import com.hyperion.model.SalesReportSummary;
import com.hyperion.repository.SaleRepository;

import java.util.List;

public class ReportService {

    private final SaleRepository saleRepository = new SaleRepository();

    public SalesReportSummary getSalesSummary() {
        return saleRepository.getSalesReportSummary();
    }

    public List<PaymentMethodReport> listSalesByPaymentMethod() {
        return saleRepository.findSalesByPaymentMethod();
    }

    public List<ProductSalesReport> listTopSellingProducts() {
        return saleRepository.findTopSellingProducts();
    }
}
