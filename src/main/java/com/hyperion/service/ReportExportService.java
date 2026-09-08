package com.hyperion.service;

import com.hyperion.exception.ReportExportException;
import com.hyperion.model.PaymentMethodReport;
import com.hyperion.model.ProductSalesReport;
import com.hyperion.model.SalesReportSummary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
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
    private static final NumberFormat CSV_MONEY_FORMAT = NumberFormat.getNumberInstance(Locale.of("pt", "BR"));
    private static final String CSV_DELIMITER = ";";
    private static final float PDF_MARGIN = 42;
    private static final float PDF_WIDTH = PDRectangle.A4.getWidth() - (PDF_MARGIN * 2);

    static {
        CSV_MONEY_FORMAT.setMinimumFractionDigits(2);
        CSV_MONEY_FORMAT.setMaximumFractionDigits(2);
    }

    public void exportCsv(Path file, SalesReportSummary summary, List<PaymentMethodReport> payments, List<ProductSalesReport> products) {
        List<List<ExportCell>> rows = buildRows(summary, payments, products);
        StringBuilder content = new StringBuilder("\uFEFF");
        for (List<ExportCell> row : rows) {
            for (int index = 0; index < row.size(); index++) {
                if (index > 0) {
                    content.append(CSV_DELIMITER);
                }
                content.append(toCsvCell(row.get(index)));
            }
            content.append("\r\n");
        }
        writeAtomically(file, temporaryFile -> Files.writeString(temporaryFile, content, StandardCharsets.UTF_8),
                "Não foi possível exportar o relatório em CSV.");
    }

    /** Creates a native Office Open XML workbook compatible with modern Excel. */
    public void exportExcel(Path file, SalesReportSummary summary, List<PaymentMethodReport> payments, List<ProductSalesReport> products) {
        List<List<ExportCell>> rows = buildRows(summary, payments, products);
        writeAtomically(file, temporaryFile -> {
            try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(temporaryFile), StandardCharsets.UTF_8)) {
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
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships" Target="xl/workbook.xml"/>
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
                writeZipEntry(output, "xl/worksheets/sheet1.xml", buildWorksheetXml(rows));
            }
        }, "Não foi possível exportar o relatório para Excel.");
    }

    public void exportPdf(Path file, SalesReportSummary summary, List<PaymentMethodReport> payments, List<ProductSalesReport> products) {
        List<PdfLine> lines = buildPrintableLines(summary, payments, products);
        writeAtomically(file, temporaryFile -> {
            try (PDDocument document = new PDDocument()) {
                writePdf(document, lines);
                document.save(temporaryFile.toFile());
            }
        }, "Não foi possível exportar o relatório em PDF.");
    }

    private List<List<ExportCell>> buildRows(
            SalesReportSummary summary,
            List<PaymentMethodReport> payments,
            List<ProductSalesReport> products
    ) {
        List<List<ExportCell>> rows = new ArrayList<>();
        rows.add(row(text("Relatório de vendas"), text("Gerado em " + LocalDate.now())));
        rows.add(row(text("Total vendido"), money(summary.getTotalSales())));
        rows.add(row(text("Quantidade de vendas"), number(summary.getSalesCount())));
        rows.add(row(text("Ticket médio"), money(summary.getAverageTicket())));
        rows.add(List.of());
        rows.add(row(text("Vendas por forma de pagamento")));
        rows.add(row(text("Forma"), text("Quantidade"), text("Total")));
        for (PaymentMethodReport payment : safeList(payments)) {
            rows.add(row(text(payment.getPaymentMethod()), number(payment.getSalesCount()), money(payment.getTotalAmount())));
        }
        rows.add(List.of());
        rows.add(row(text("Produtos mais vendidos")));
        rows.add(row(text("Produto"), text("Preço unitário"), text("Quantidade"), text("Total")));
        for (ProductSalesReport product : safeList(products)) {
            rows.add(row(text(product.getProductName()), money(product.getUnitPrice()),
                    number(product.getQuantitySold()), money(product.getTotalAmount())));
        }
        return rows;
    }

    private List<PdfLine> buildPrintableLines(
            SalesReportSummary summary,
            List<PaymentMethodReport> payments,
            List<ProductSalesReport> products
    ) {
        List<PdfLine> lines = new ArrayList<>();
        lines.add(new PdfLine("Hyperion - Relatório de vendas", true));
        lines.add(new PdfLine("Gerado em " + LocalDate.now(), false));
        lines.add(new PdfLine("Total vendido: " + formatMoney(summary.getTotalSales()), false));
        lines.add(new PdfLine("Quantidade de vendas: " + summary.getSalesCount(), false));
        lines.add(new PdfLine("Ticket médio: " + formatMoney(summary.getAverageTicket()), false));
        lines.add(new PdfLine("", false));
        lines.add(new PdfLine("Vendas por forma de pagamento", true));
        for (PaymentMethodReport payment : safeList(payments)) {
            lines.add(new PdfLine(payment.getPaymentMethod() + ": " + payment.getSalesCount()
                    + " venda(s) - " + formatMoney(payment.getTotalAmount()), false));
        }
        lines.add(new PdfLine("", false));
        lines.add(new PdfLine("Produtos mais vendidos", true));
        for (ProductSalesReport product : safeList(products)) {
            lines.add(new PdfLine(product.getProductName() + ": " + product.getQuantitySold()
                    + " un. - " + formatMoney(product.getTotalAmount()), false));
        }
        return lines;
    }

    private void writePdf(PDDocument document, List<PdfLine> lines) throws IOException {
        PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDPageContentStream stream = null;
        float y = 0;

        try {
            for (PdfLine line : lines) {
                PDFont font = line.bold() ? bold : regular;
                float fontSize = line.bold() ? 11 : 10;
                List<String> wrappedLines = wrapPdfText(font, toPdfText(line.value()), fontSize);
                for (String wrappedLine : wrappedLines) {
                    float lineHeight = line.bold() ? 17 : 14;
                    if (stream == null || y - lineHeight < PDF_MARGIN) {
                        if (stream != null) {
                            stream.close();
                        }
                        PDPage page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        stream = new PDPageContentStream(document, page);
                        y = PDRectangle.A4.getHeight() - PDF_MARGIN;
                    }
                    stream.beginText();
                    stream.setFont(font, fontSize);
                    stream.newLineAtOffset(PDF_MARGIN, y);
                    stream.showText(wrappedLine);
                    stream.endText();
                    y -= lineHeight;
                }
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private List<String> wrapPdfText(PDFont font, String value, float fontSize) throws IOException {
        if (value.isBlank()) {
            return List.of("");
        }

        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (String word : value.split("\\s+")) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (getPdfWidth(font, candidate, fontSize) <= PDF_WIDTH) {
                currentLine.setLength(0);
                currentLine.append(candidate);
                continue;
            }
            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
            }
            appendLongPdfWord(lines, currentLine, font, word, fontSize);
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private void appendLongPdfWord(List<String> lines, StringBuilder currentLine, PDFont font, String word, float fontSize) throws IOException {
        for (int index = 0; index < word.length(); index++) {
            currentLine.append(word.charAt(index));
            if (getPdfWidth(font, currentLine.toString(), fontSize) > PDF_WIDTH) {
                currentLine.deleteCharAt(currentLine.length() - 1);
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                }
                currentLine.setLength(0);
                currentLine.append(word.charAt(index));
            }
        }
    }

    private float getPdfWidth(PDFont font, String text, float fontSize) throws IOException {
        return font.getStringWidth(text) / 1_000 * fontSize;
    }

    private String buildWorksheetXml(List<List<ExportCell>> rows) {
        StringBuilder worksheet = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
                """);
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            worksheet.append("<row r=\"").append(rowIndex + 1).append("\">");
            List<ExportCell> row = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                ExportCell cell = row.get(columnIndex);
                String coordinate = toColumnName(columnIndex + 1) + (rowIndex + 1);
                if (cell.kind() == CellKind.TEXT) {
                    worksheet.append("<c r=\"").append(coordinate)
                            .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                            .append(escapeXml(cell.value()))
                            .append("</t></is></c>");
                } else {
                    worksheet.append("<c r=\"").append(coordinate).append("\" t=\"n\"><v>")
                            .append(cell.value()).append("</v></c>");
                }
            }
            worksheet.append("</row>");
        }
        return worksheet.append("</sheetData></worksheet>").toString();
    }

    private String toCsvCell(ExportCell cell) {
        String value = switch (cell.kind()) {
            case TEXT -> protectSpreadsheetFormula(cell.value());
            case NUMBER -> cell.value();
            case MONEY -> CSV_MONEY_FORMAT.format(new BigDecimal(cell.value()));
        };
        boolean needsQuotes = value.contains(CSV_DELIMITER) || value.contains("\"") || value.contains("\r") || value.contains("\n");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private String protectSpreadsheetFormula(String value) {
        String normalized = value == null ? "" : value;
        String trimmed = normalized.stripLeading();
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) {
            return "'" + normalized;
        }
        return normalized;
    }

    private void writeZipEntry(ZipOutputStream output, String name, String content) throws IOException {
        output.putNextEntry(new ZipEntry(name));
        output.write(content.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }

    private void writeAtomically(Path destination, FileWriter writer, String errorMessage) {
        Path temporaryFile = null;
        try {
            Path normalizedDestination = destination.toAbsolutePath().normalize();
            Path directory = normalizedDestination.getParent();
            if (directory == null) {
                throw new IOException("Diretório de destino inválido.");
            }
            Files.createDirectories(directory);
            temporaryFile = Files.createTempFile(directory, "hyperion-export-", ".tmp");
            writer.write(temporaryFile);
            moveReplacing(temporaryFile, normalizedDestination);
            temporaryFile = null;
        } catch (IOException exception) {
            throw new ReportExportException(errorMessage, exception);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // A later cleanup by the operating system is safe.
                }
            }
        }
    }

    private void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
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

    private String escapeXml(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); ) {
            int codePoint = value.codePointAt(index);
            index += Character.charCount(codePoint);
            if (!isValidXmlCodePoint(codePoint)) {
                continue;
            }
            switch (codePoint) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&apos;");
                default -> escaped.appendCodePoint(codePoint);
            }
        }
        return escaped.toString();
    }

    private boolean isValidXmlCodePoint(int codePoint) {
        return codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD
                || (codePoint >= 0x20 && codePoint <= 0xD7FF)
                || (codePoint >= 0xE000 && codePoint <= 0xFFFD)
                || (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
    }

    private String toPdfText(String value) {
        StringBuilder sanitized = new StringBuilder();
        for (int index = 0; index < value.length(); ) {
            int codePoint = value.codePointAt(index);
            index += Character.charCount(codePoint);
            if (codePoint == 0x2013 || codePoint == 0x2014) {
                sanitized.append('-');
            } else if (codePoint == 0x00A0) {
                sanitized.append(' ');
            } else if (codePoint >= 0x20 && codePoint <= 0xFF) {
                sanitized.appendCodePoint(codePoint);
            } else {
                String transliterated = Normalizer.normalize(new String(Character.toChars(codePoint)), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "");
                sanitized.append(transliterated.chars().allMatch(character -> character >= 0x20 && character <= 0x7E)
                        ? transliterated
                        : "?");
            }
        }
        return sanitized.toString();
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }

    private ExportCell text(String value) {
        return new ExportCell(value == null ? "" : value, CellKind.TEXT);
    }

    private ExportCell number(int value) {
        return new ExportCell(String.valueOf(value), CellKind.NUMBER);
    }

    private ExportCell money(BigDecimal value) {
        return new ExportCell((value == null ? BigDecimal.ZERO : value).toPlainString(), CellKind.MONEY);
    }

    private List<ExportCell> row(ExportCell... cells) {
        return List.of(cells);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record ExportCell(String value, CellKind kind) {
    }

    private record PdfLine(String value, boolean bold) {
    }

    private enum CellKind {
        TEXT,
        NUMBER,
        MONEY
    }

    @FunctionalInterface
    private interface FileWriter {
        void write(Path temporaryFile) throws IOException;
    }
}
