package com.hyperion.controller;

import com.hyperion.model.PaymentMethodReport;
import com.hyperion.model.ProductSalesReport;
import com.hyperion.model.SalesReportSummary;
import com.hyperion.service.ReportService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

public class ReportController {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final String TODAY_FILTER = "Hoje";
    private static final String CURRENT_MONTH_FILTER = "Este m\u00eas";
    private static final String LAST_MONTH_FILTER = "M\u00eas passado";
    private static final String ALL_PERIODS_FILTER = "Todo o per\u00edodo";

    private final ReportService reportService = new ReportService();
    private BigDecimal currentTotalSales = BigDecimal.ZERO;

    @FXML
    private ChoiceBox<String> periodChoiceBox;

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
        configurePaymentMethodsTable();
        configureTopProductsTable();
        loadReports();
    }

    @FXML
    private void handleRefresh() {
        loadReports();
        messageLabel.setText("Relat\u00f3rios atualizados.");
    }

    private void configurePeriodFilter() {
        periodChoiceBox.setItems(FXCollections.observableArrayList(
                TODAY_FILTER,
                CURRENT_MONTH_FILTER,
                LAST_MONTH_FILTER,
                ALL_PERIODS_FILTER
        ));
        periodChoiceBox.setValue(CURRENT_MONTH_FILTER);
        periodChoiceBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> loadReports());
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
        DateRange dateRange = resolveDateRange();
        SalesReportSummary summary = reportService.getSalesSummary(dateRange.startDate(), dateRange.endDateExclusive());
        currentTotalSales = summary.getTotalSales();

        totalSalesLabel.setText(formatMoney(summary.getTotalSales()));
        salesCountLabel.setText(String.valueOf(summary.getSalesCount()));
        averageTicketLabel.setText(formatMoney(summary.getAverageTicket()));

        paymentMethodsTable.setItems(FXCollections.observableArrayList(
                reportService.listSalesByPaymentMethod(dateRange.startDate(), dateRange.endDateExclusive())
        ));
        topProductsTable.setItems(FXCollections.observableArrayList(
                reportService.listTopSellingProducts(dateRange.startDate(), dateRange.endDateExclusive())
        ));
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

        YearMonth currentMonth = YearMonth.now();
        return new DateRange(currentMonth.atDay(1), currentMonth.plusMonths(1).atDay(1));
    }

    private record DateRange(LocalDate startDate, LocalDate endDateExclusive) {
    }
}
