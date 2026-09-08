package com.hyperion.service;

import com.hyperion.exception.ReportExportException;
import com.hyperion.model.PaymentMethodReport;
import com.hyperion.model.ProductSalesReport;
import com.hyperion.model.SalesReportSummary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Exports the report currently displayed by the operator. */
public class ReportExportService {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    public void exportCsv(Path file, SalesReportSummary summary, List<PaymentMethodReport> payments, List<ProductSalesReport> products) {
        List<String> lines = buildDelimitedLines(summary, payments, products, ";");
        try {
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ReportExportException("Não foi possível exportar o relatório em CSV.", exception);
        }
    }

    /** Creates a native Office Open XML workbook compatible with modern Excel. */
    public void exportExcel(Path file, SalesReportSummary summary, List<PaymentMethodReport> payments, List<ProductSalesReport> products) {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            writeZipEntry(output, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    </Types>
                    """);
            writeZipEntry(output, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                    """);
            writeZipEntry(output, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                      xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets><sheet name="Relatório" sheetId="1" r:id="rId1"/></sheets>
                    </workbook>
                    """);
            writeZipEntry(output, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    </Relationships>
                    """);
            writeZipEntry(output, "xl/worksheets/sheet1.xml", buildWorksheetXml(summary, payments, products));
        } catch (IOException exception) {
            throw new ReportExportException("Não foi possível exportar o relatório para Excel.", exception);
        }
    }

    public void exportPdf(Path file, SalesReportSummary summary, List<PaymentMethodReport> payments, List<ProductSalesReport> products) {
        List<String> lines = buildPrintableLines(summary, payments, products);
        try (PDDocument document = new PDDocument()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = 800;
                for (int index = 0; index < lines.size() && y > 42; index++) {
                    stream.beginText();
                    stream.setFont(index == 0 ? bold : regular, index == 0 ? 15 : 10);
                    stream.newLineAtOffset(42, y);
                    stream.showText(toPdfText(lines.get(index)));
                    stream.endText();
                    y -= index == 0 ? 24 : 15;
                }
            }
            document.save(file.toFile());
        } catch (IOException exception) {
            throw new ReportExportException("Não foi possível exportar o relatório em PDF.", exception);
        }
    }

    private List<String> buildDelimitedLines(
            SalesReportSummary summary,
            List<PaymentMethodReport> payments,
            List<ProductSalesReport> products,
            String delimiter
    ) {
        List<String> lines = new ArrayList<>();
        lines.add(join(delimiter, "Relatório de vendas", "Gerado em " + LocalDate.now()));
        lines.add(join(delimiter, "Total vendido", formatMoney(summary.getTotalSales())));
        lines.add(join(delimiter, "Quantidade de vendas", String.valueOf(summary.getSalesCount())));
        lines.add(join(delimiter, "Ticket médio", formatMoney(summary.getAverageTicket())));
        lines.add("");
        lines.add(join(delimiter, "Vendas por forma de pagamento"));
        lines.add(join(delimiter, "Forma", "Quantidade", "Total"));
        for (PaymentMethodReport payment : payments) {
            lines.add(join(delimiter, payment.getPaymentMethod(), String.valueOf(payment.getSalesCount()), formatMoney(payment.getTotalAmount())));
        }
        lines.add("");
        lines.add(join(delimiter, "Produtos mais vendidos"));
        lines.add(join(delimiter, "Produto", "Preço unitário", "Quantidade", "Total"));
        for (ProductSalesReport product : products) {
            lines.add(join(delimiter, product.getProductName(), formatMoney(product.getUnitPrice()),
                    String.valueOf(product.getQuantitySold()), formatMoney(product.getTotalAmount())));
        }
        return lines;
    }

    private String buildWorksheetXml(
            SalesReportSummary summary,
            List<PaymentMethodReport> payments,
            List<ProductSalesReport> products
    ) {
        StringBuilder worksheet = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
                """);
        List<String> lines = buildDelimitedLines(summary, payments, products, "\t");
        for (int rowIndex = 0; rowIndex < lines.size(); rowIndex++) {
            worksheet.append("<row r=\"").append(rowIndex + 1).append("\">");
            String[] cells = lines.get(rowIndex).split("\\t", -1);
            for (int columnIndex = 0; columnIndex < cells.length; columnIndex++) {
                worksheet.append("<c r=\"")
                        .append(toColumnName(columnIndex + 1)).append(rowIndex + 1)
                        .append("\" t=\"inlineStr\"><is><t>")
                        .append(escapeXml(cells[columnIndex]))
                        .append("</t></is></c>");
            }
            worksheet.append("</row>");
        }
        return worksheet.append("</sheetData></worksheet>").toString();
    }

    private void writeZipEntry(ZipOutputStream output, String name, String content) throws IOException {
        output.putNextEntry(new ZipEntry(name));
        output.write(content.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }

    private String toColumnName(int index) {
        StringBuilder name = new StringBuilder();
        int remaining = index;
        while (remaining > 0) {
            remaining--;
            name.append((char) ('A' + (remaining % 26)));
            remaining /= 26;
        }
        return name.reverse().toString();
    }

    private List<String> buildPrintableLines(SalesReportSummary summary, List<PaymentMethodReport> payments, List<ProductSalesReport> products) {
        List<String> lines = new ArrayList<>();
        lines.add("Hyperion — Relatório de vendas");
        lines.add("Gerado em " + LocalDate.now());
        lines.add("Total vendido: " + formatMoney(summary.getTotalSales()));
        lines.add("Quantidade de vendas: " + summary.getSalesCount());
        lines.add("Ticket médio: " + formatMoney(summary.getAverageTicket()));
        lines.add("");
        lines.add("Vendas por forma de pagamento");
        for (PaymentMethodReport payment : payments) {
            lines.add(payment.getPaymentMethod() + ": " + payment.getSalesCount() + " venda(s) — " + formatMoney(payment.getTotalAmount()));
        }
        lines.add("");
        lines.add("Produtos mais vendidos");
        for (ProductSalesReport product : products) {
            lines.add(product.getProductName() + ": " + product.getQuantitySold() + " un. — " + formatMoney(product.getTotalAmount()));
        }
        return lines;
    }

    private String join(String delimiter, String... values) {
        return String.join(delimiter, values);
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private String toPdfText(String value) {
        return value.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
