package com.hyperion.controller;

import com.hyperion.model.PaymentMethodReport;
import com.hyperion.model.ProductSalesReport;
import com.hyperion.model.SalesReportSummary;
import com.hyperion.service.ReportService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class ReportController {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private final ReportService reportService = new ReportService();

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
    private TableView<ProductSalesReport> topProductsTable;

    @FXML
    private TableColumn<ProductSalesReport, String> productNameColumn;

    @FXML
    private TableColumn<ProductSalesReport, String> productQuantityColumn;

    @FXML
    private TableColumn<ProductSalesReport, String> productTotalColumn;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        configurePaymentMethodsTable();
        configureTopProductsTable();
        loadReports();
    }

    @FXML
    private void handleRefresh() {
        loadReports();
        messageLabel.setText("Relatórios atualizados.");
    }

    private void configurePaymentMethodsTable() {
        paymentMethodColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getPaymentMethod()));
        paymentSalesCountColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().getSalesCount())));
        paymentTotalColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getTotalAmount())));
    }

    private void configureTopProductsTable() {
        productNameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getProductName()));
        productQuantityColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().getQuantitySold())));
        productTotalColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getTotalAmount())));
    }

    private void loadReports() {
        SalesReportSummary summary = reportService.getSalesSummary();

        totalSalesLabel.setText(formatMoney(summary.getTotalSales()));
        salesCountLabel.setText(String.valueOf(summary.getSalesCount()));
        averageTicketLabel.setText(formatMoney(summary.getAverageTicket()));

        paymentMethodsTable.setItems(FXCollections.observableArrayList(reportService.listSalesByPaymentMethod()));
        topProductsTable.setItems(FXCollections.observableArrayList(reportService.listTopSellingProducts()));
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }
}
