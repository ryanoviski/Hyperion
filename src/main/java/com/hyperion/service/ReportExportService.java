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

    /** Excel opens this standards-based XML spreadsheet directly as a .xls file. */
    public void exportExcel(Path file, SalesReportSummary summary, List<PaymentMethodReport> payments, List<ProductSalesReport> products) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <?mso-application progid="Excel.Sheet"?>
                <Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
                  xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
                  <Worksheet ss:Name="Relatório"><Table>
                """);
        for (String line : buildDelimitedLines(summary, payments, products, "\t")) {
            xml.append("<Row>");
            for (String cell : line.split("\\t", -1)) {
                xml.append("<Cell><Data ss:Type=\"String\">")
                        .append(escapeXml(cell))
                        .append("</Data></Cell>");
            }
            xml.append("</Row>");
        }
        xml.append("</Table></Worksheet></Workbook>");
        try {
            Files.writeString(file, xml.toString(), StandardCharsets.UTF_8);
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
