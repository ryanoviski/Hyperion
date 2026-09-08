package com.hyperion.service;

import com.hyperion.model.PaymentMethodReport;
import com.hyperion.model.ProductSalesReport;
import com.hyperion.model.SalesReportSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportExportServiceTest {

    @TempDir
    Path testDirectory;

    @Test
    void exportsANativeXlsxWorkbook() throws Exception {
        Path file = testDirectory.resolve("relatorio.xlsx");
        new ReportExportService().exportExcel(
                file,
                new SalesReportSummary(1, new BigDecimal("12.50"), new BigDecimal("12.50")),
                List.of(new PaymentMethodReport("PIX", 1, new BigDecimal("12.50"))),
                List.of(new ProductSalesReport("Produto", new BigDecimal("12.50"), 1, new BigDecimal("12.50")))
        );

        assertTrue(Files.size(file) > 0);
        try (ZipFile workbook = new ZipFile(file.toFile())) {
            assertNotNull(workbook.getEntry("[Content_Types].xml"));
            assertNotNull(workbook.getEntry("xl/workbook.xml"));
            assertNotNull(workbook.getEntry("xl/worksheets/sheet1.xml"));
        }
    }
}
