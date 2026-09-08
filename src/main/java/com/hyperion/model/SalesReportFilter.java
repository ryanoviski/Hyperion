package com.hyperion.model;

import java.time.LocalDate;

public record SalesReportFilter(
        LocalDate startDate,
        LocalDate endDateExclusive,
        String customerName,
        String paymentMethod,
        String category,
        String supplier
) {
    public SalesReportFilter {
        customerName = normalize(customerName);
        paymentMethod = normalize(paymentMethod);
        category = normalize(category);
        supplier = normalize(supplier);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
