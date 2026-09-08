package com.hyperion.service;

import com.hyperion.model.PaymentMethodReport;
import com.hyperion.model.ProductSalesReport;
import com.hyperion.model.SalesReportSummary;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
            String worksheet = new String(workbook.getInputStream(workbook.getEntry("xl/worksheets/sheet1.xml")).readAllBytes());
            assertTrue(worksheet.contains("t=\"n\"><v>12.50</v>"));
        }
    }

    @Test
    void escapesCsvValuesAndNeutralizesSpreadsheetFormulas() throws Exception {
        Path file = testDirectory.resolve("relatorio.csv");
        new ReportExportService().exportCsv(
                file,
                new SalesReportSummary(1, new BigDecimal("12.50"), new BigDecimal("12.50")),
                List.of(new PaymentMethodReport("PIX", 1, new BigDecimal("12.50"))),
                List.of(new ProductSalesReport("=SOMA(A1:A2); \"Café\"", new BigDecimal("12.50"), 1, new BigDecimal("12.50")))
        );

        String csv = Files.readString(file);
        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("'=SOMA(A1:A2)"));
        assertTrue(csv.contains("\"\"Café\"\""));
    }

    @Test
    void preservesPortugueseCharactersAndPaginatesPdfExports() throws Exception {
        Path file = testDirectory.resolve("relatorio.pdf");
        List<ProductSalesReport> products = java.util.stream.IntStream.range(0, 120)
                .mapToObj(index -> new ProductSalesReport(
                        "Ação café " + index,
                        new BigDecimal("12.50"),
                        1,
                        new BigDecimal("12.50")
                ))
                .toList();

        new ReportExportService().exportPdf(
                file,
                new SalesReportSummary(120, new BigDecimal("1500.00"), new BigDecimal("12.50")),
                List.of(new PaymentMethodReport("PIX", 120, new BigDecimal("1500.00"))),
                products
        );

        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            assertTrue(document.getNumberOfPages() > 1);
            String content = new PDFTextStripper().getText(document);
            assertTrue(content.contains("Relatório"));
            assertTrue(content.contains("Ação café"));
        }
    }
}
