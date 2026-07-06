package com.hyperion.model;

import java.math.BigDecimal;

public class SalesReportSummary {

    private final int salesCount;
    private final BigDecimal totalSales;
    private final BigDecimal averageTicket;

    public SalesReportSummary(int salesCount, BigDecimal totalSales, BigDecimal averageTicket) {
        this.salesCount = salesCount;
        this.totalSales = valueOrZero(totalSales);
        this.averageTicket = valueOrZero(averageTicket);
    }

    public int getSalesCount() {
        return salesCount;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public BigDecimal getAverageTicket() {
        return averageTicket;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
