package com.hyperion.controller;

import com.hyperion.model.PaymentMethodReport;
import com.hyperion.model.ProductSalesReport;
import com.hyperion.model.SalesReportSummary;
import com.hyperion.model.SalesReportFilter;
import com.hyperion.service.ReportService;
import com.hyperion.service.ReportExportService;
import com.hyperion.exception.HyperionException;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class ReportController {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final String TODAY_FILTER = "Hoje";
    private static final String CURRENT_MONTH_FILTER = "Este m\u00eas";
    private static final String LAST_MONTH_FILTER = "M\u00eas passado";
    private static final String ALL_PERIODS_FILTER = "Todo o per\u00edodo";
    private static final String CUSTOM_PERIOD_FILTER = "Período personalizado";
    private static final String ALL_CUSTOMERS_FILTER = "Todos os clientes";
    private static final String ALL_CATEGORIES_FILTER = "Todas as categorias";
    private static final String ALL_SUPPLIERS_FILTER = "Todos os fornecedores";
    private static final String ALL_PAYMENT_METHODS_FILTER = "Todas as formas";

    private final ReportService reportService = new ReportService();
    private final ReportExportService reportExportService = new ReportExportService();
    private BigDecimal currentTotalSales = BigDecimal.ZERO;
    private SalesReportSummary currentSummary = new SalesReportSummary(0, BigDecimal.ZERO, BigDecimal.ZERO);
    private List<PaymentMethodReport> currentPayments = List.of();
    private List<ProductSalesReport> currentProducts = List.of();

    @FXML
    private ChoiceBox<String> periodChoiceBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ChoiceBox<String> customerFilterChoiceBox;

    @FXML
    private ChoiceBox<String> paymentFilterChoiceBox;

    @FXML
    private ChoiceBox<String> categoryFilterChoiceBox;

    @FXML
    private ChoiceBox<String> supplierFilterChoiceBox;

    @FXML
    private Label totalSalesLabel;

    @FXML
    private Label salesCountLabel;

    @FXML
    private Label averageTicketLabel;

    @FXML
    private TableView<PaymentMethodReport> paymentMethodsTable;

    @FXML
    private TableColumn<PaymentMethodReport, String> paymentMethodColumn;

    @FXML
    private TableColumn<PaymentMethodReport, String> paymentSalesCountColumn;

    @FXML
    private TableColumn<PaymentMethodReport, String> paymentTotalColumn;

    @FXML
    private TableColumn<PaymentMethodReport, String> paymentPercentColumn;

    @FXML
    private TableColumn<PaymentMethodReport, PaymentMethodReport> paymentShareColumn;

    @FXML
    private TableView<ProductSalesReport> topProductsTable;

    @FXML
    private TableColumn<ProductSalesReport, String> productNameColumn;

    @FXML
    private TableColumn<ProductSalesReport, String> productUnitPriceColumn;

    @FXML
    private TableColumn<ProductSalesReport, String> productQuantityColumn;

    @FXML
    private TableColumn<ProductSalesReport, String> productTotalColumn;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        configurePeriodFilter();
        configureReportFilters();
        configurePaymentMethodsTable();
        configureTopProductsTable();
        loadReports();
    }

    @FXML
    private void handleRefresh() {
        loadReports();
        messageLabel.setText("Relat\u00f3rios atualizados.");
    }

    @FXML
    private void handleExportCsv() {
        export("CSV", "*.csv", "csv");
    }

    @FXML
    private void handleExportExcel() {
        export("Excel", "*.xlsx", "xlsx");
    }

    @FXML
    private void handleExportPdf() {
        export("PDF", "*.pdf", "pdf");
    }

    private void configurePeriodFilter() {
        periodChoiceBox.setItems(FXCollections.observableArrayList(
                TODAY_FILTER,
                CURRENT_MONTH_FILTER,
                LAST_MONTH_FILTER,
                ALL_PERIODS_FILTER,
                CUSTOM_PERIOD_FILTER
        ));
        periodChoiceBox.setValue(CURRENT_MONTH_FILTER);
        periodChoiceBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> loadReports());
        startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> loadReports());
        endDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> loadReports());
    }

    private void configureReportFilters() {
        customerFilterChoiceBox.setItems(FXCollections.observableArrayList(ALL_CUSTOMERS_FILTER));
        customerFilterChoiceBox.getItems().addAll(reportService.listCustomersForFilter());
        customerFilterChoiceBox.setValue(ALL_CUSTOMERS_FILTER);

        categoryFilterChoiceBox.setItems(FXCollections.observableArrayList(ALL_CATEGORIES_FILTER));
        categoryFilterChoiceBox.getItems().addAll(reportService.listCategoriesForFilter());
        categoryFilterChoiceBox.setValue(ALL_CATEGORIES_FILTER);

        supplierFilterChoiceBox.setItems(FXCollections.observableArrayList(ALL_SUPPLIERS_FILTER));
        supplierFilterChoiceBox.getItems().addAll(reportService.listSuppliersForFilter());
        supplierFilterChoiceBox.setValue(ALL_SUPPLIERS_FILTER);

        paymentFilterChoiceBox.setItems(FXCollections.observableArrayList(
                ALL_PAYMENT_METHODS_FILTER, "Dinheiro", "PIX", "Cartão crédito", "Cartão débito", "Crediário"
        ));
        paymentFilterChoiceBox.setValue(ALL_PAYMENT_METHODS_FILTER);

        customerFilterChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> loadReports());
        categoryFilterChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> loadReports());
        supplierFilterChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> loadReports());
        paymentFilterChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> loadReports());
    }

    private void configurePaymentMethodsTable() {
        paymentMethodsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        paymentMethodColumn.getStyleClass().add("left-aligned-column");
        paymentMethodColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(displayValue(cellData.getValue().getPaymentMethod())));
        paymentSalesCountColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().getSalesCount())));
        paymentTotalColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getTotalAmount())));
        paymentPercentColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatPercentage(cellData.getValue().getTotalAmount())));
        paymentShareColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        paymentShareColumn.setCellFactory(column -> createShareCell());
    }

    private void configureTopProductsTable() {
        topProductsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        productNameColumn.getStyleClass().add("left-aligned-column");
        productNameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(displayValue(cellData.getValue().getProductName())));
        productUnitPriceColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getUnitPrice())));
        productQuantityColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().getQuantitySold())));
        productTotalColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getTotalAmount())));
    }

    private void loadReports() {
        try {
            DateRange dateRange = resolveDateRange();
            SalesReportFilter filter = new SalesReportFilter(
                    dateRange.startDate(),
                    dateRange.endDateExclusive(),
                    selectedFilterValue(customerFilterChoiceBox, ALL_CUSTOMERS_FILTER),
                    selectedFilterValue(paymentFilterChoiceBox, ALL_PAYMENT_METHODS_FILTER),
                    selectedFilterValue(categoryFilterChoiceBox, ALL_CATEGORIES_FILTER),
                    selectedFilterValue(supplierFilterChoiceBox, ALL_SUPPLIERS_FILTER)
            );
            currentSummary = reportService.getSalesSummary(filter);
            currentTotalSales = currentSummary.getTotalSales();

            totalSalesLabel.setText(formatMoney(currentSummary.getTotalSales()));
            salesCountLabel.setText(String.valueOf(currentSummary.getSalesCount()));
            averageTicketLabel.setText(formatMoney(currentSummary.getAverageTicket()));

            currentPayments = reportService.listSalesByPaymentMethod(filter);
            currentProducts = reportService.listTopSellingProducts(filter);
            paymentMethodsTable.setItems(FXCollections.observableArrayList(currentPayments));
            topProductsTable.setItems(FXCollections.observableArrayList(currentProducts));
        } catch (HyperionException | IllegalArgumentException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }

    private String displayValue(String value) {
        String normalizedValue = value == null ? "" : value.trim();
        return normalizedValue.isBlank() ? "—" : normalizedValue;
    }

    private String formatPercentage(BigDecimal value) {
        return calculateParticipation(value).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private TableCell<PaymentMethodReport, PaymentMethodReport> createShareCell() {
        return new TableCell<>() {
            private final ProgressBar progressBar = new ProgressBar();
            private final HBox container = new HBox(progressBar);

            {
                progressBar.setMaxWidth(Double.MAX_VALUE);
                progressBar.getStyleClass().add("report-share-bar");
                container.setAlignment(Pos.CENTER);
                container.getStyleClass().add("report-share-cell");
            }

            @Override
            protected void updateItem(PaymentMethodReport report, boolean empty) {
                super.updateItem(report, empty);

                if (empty || report == null) {
                    setGraphic(null);
                    return;
                }

                progressBar.setProgress(calculateParticipation(report.getTotalAmount()).doubleValue());
                setGraphic(container);
            }
        };
    }

    private BigDecimal calculateParticipation(BigDecimal value) {
        if (currentTotalSales == null || currentTotalSales.compareTo(BigDecimal.ZERO) == 0 || value == null) {
            return BigDecimal.ZERO;
        }

        return value.divide(currentTotalSales, 4, RoundingMode.HALF_UP);
    }

    private DateRange resolveDateRange() {
        String selectedFilter = periodChoiceBox.getValue();
        LocalDate today = LocalDate.now();

        if (TODAY_FILTER.equals(selectedFilter)) {
            return new DateRange(today, today.plusDays(1));
        }

        if (LAST_MONTH_FILTER.equals(selectedFilter)) {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            return new DateRange(lastMonth.atDay(1), lastMonth.plusMonths(1).atDay(1));
        }

        if (ALL_PERIODS_FILTER.equals(selectedFilter)) {
            return new DateRange(null, null);
        }

        if (CUSTOM_PERIOD_FILTER.equals(selectedFilter)) {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            if (startDate == null || endDate == null) {
                return new DateRange(null, null);
            }
            return new DateRange(startDate, endDate.plusDays(1));
        }

        YearMonth currentMonth = YearMonth.now();
        return new DateRange(currentMonth.atDay(1), currentMonth.plusMonths(1).atDay(1));
    }

    private record DateRange(LocalDate startDate, LocalDate endDateExclusive) {
    }

    private void export(String type, String extensionPattern, String extension) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar relatório em " + type);
        fileChooser.setInitialFileName("relatorio-vendas-" + LocalDate.now() + "." + extension);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(type, extensionPattern));
        File selectedFile = fileChooser.showSaveDialog(messageLabel.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        Path file = selectedFile.toPath();
        switch (extension) {
            case "csv" -> reportExportService.exportCsv(file, currentSummary, currentPayments, currentProducts);
            case "xlsx" -> reportExportService.exportExcel(file, currentSummary, currentPayments, currentProducts);
            case "pdf" -> reportExportService.exportPdf(file, currentSummary, currentPayments, currentProducts);
            default -> throw new IllegalArgumentException("Formato de exportação não suportado.");
        }
        messageLabel.setText("Relatório exportado em " + file.toAbsolutePath() + ".");
    }

    private String selectedFilterValue(ChoiceBox<String> choiceBox, String allValue) {
        String value = choiceBox == null ? "" : choiceBox.getValue();
        return allValue.equals(value) ? "" : value;
    }
}
