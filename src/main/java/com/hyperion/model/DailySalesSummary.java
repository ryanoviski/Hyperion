package com.hyperion.model;

import java.math.BigDecimal;

public class DailySalesSummary {

    private final BigDecimal total;
    private final int salesCount;

    public DailySalesSummary(BigDecimal total, int salesCount) {
        this.total = total;
        this.salesCount = salesCount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public int getSalesCount() {
        return salesCount;
    }
}
