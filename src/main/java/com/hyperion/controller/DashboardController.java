package com.hyperion.controller;

import com.hyperion.model.CreditInstallment;
import com.hyperion.model.DailySalesSummary;
import com.hyperion.model.FinancialSummary;
import com.hyperion.model.Product;
import com.hyperion.model.Sale;
import com.hyperion.service.CreditInstallmentService;
import com.hyperion.service.CustomerService;
import com.hyperion.service.FinanceService;
import com.hyperion.service.ProductService;
import com.hyperion.service.SaleService;
import com.hyperion.util.AsyncUiTask;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class DashboardController {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final CreditInstallmentService creditInstallmentService = new CreditInstallmentService();
    private final CustomerService customerService = new CustomerService();
    private final FinanceService financeService = new FinanceService();
    private final ProductService productService = new ProductService();
    private final SaleService saleService = new SaleService();
    private final AtomicLong dashboardLoadVersion = new AtomicLong();

    @FXML
    private Label salesTodayValueLabel;

    @FXML
    private Label salesTodayFootnoteLabel;

    @FXML
    private Label customersValueLabel;

    @FXML
    private Label productsValueLabel;

    @FXML
    private Label balanceValueLabel;

    @FXML
    private Label customersFootnoteLabel;

    @FXML
    private Label productsFootnoteLabel;

    @FXML
    private TableView<Sale> latestSalesTable;

    @FXML
    private TableColumn<Sale, String> saleTimeColumn;

    @FXML
    private TableColumn<Sale, String> saleIdColumn;

    @FXML
    private TableColumn<Sale, String> saleCustomerColumn;

    @FXML
    private TableColumn<Sale, String> saleTotalColumn;

    @FXML
    private VBox alertsList;

    @FXML
    private Label emptyAlertsLabel;

    @FXML
    private Label dashboardStatusLabel;

    @FXML
    private void initialize() {
        configureLatestSalesTable();
        loadDashboard();
    }

    @FXML
    private void handleCustomers() {
        MainController.openCustomersView();
    }

    @FXML
    private void handleProducts() {
        MainController.openProductsView();
    }

    @FXML
    private void handleStock() {
        MainController.openStockView();
    }

    @FXML
    private void handleSales() {
        MainController.openSalesView();
    }

    @FXML
    private void handleReports() {
        MainController.openReportsView();
    }

    private void configureLatestSalesTable() {
        latestSalesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        saleCustomerColumn.getStyleClass().add("left-aligned-column");

        saleTimeColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                cellData.getValue().getCreatedAt().format(TIME_FORMAT)
        ));
        saleIdColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                "Venda #" + String.format("%03d", cellData.getValue().getId())
        ));
        saleCustomerColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                displayValue(cellData.getValue().getCustomerName())
        ));
        saleTotalColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                MONEY_FORMAT.format(cellData.getValue().getTotal())
        ));
        latestSalesTable.setPlaceholder(new Label("Nenhuma venda registrada ainda."));
    }

    private void loadDashboard() {
        long loadVersion = dashboardLoadVersion.incrementAndGet();
        showDashboardStatus("Carregando informações do dashboard...");

        AsyncUiTask.run(
                "carregar dashboard",
                () -> {
                    List<Product> activeProducts = List.copyOf(productService.listActiveProducts());
                    return new DashboardData(
                            customerService.listActiveCustomers().size(),
                            activeProducts,
                            saleService.getTodaySummary(),
                            financeService.getSummary(),
                            List.copyOf(saleService.listLatestSales(8)),
                            List.copyOf(creditInstallmentService.listPendingAlerts()),
                            List.copyOf(productService.listLowStockProducts())
                    );
                },
                dashboardData -> {
                    if (loadVersion != dashboardLoadVersion.get()) {
                        return;
                    }
                    applyDashboardData(dashboardData);
                    showDashboardStatus("");
                },
                exception -> {
                    if (loadVersion == dashboardLoadVersion.get()) {
                        showDashboardStatus(messageFor(exception));
                    }
                }
        );
    }

    private void applyDashboardData(DashboardData dashboardData) {
        int activeCustomers = dashboardData.activeCustomers();
        List<Product> activeProducts = dashboardData.activeProducts();
        DailySalesSummary dailySalesSummary = dashboardData.dailySalesSummary();
        FinancialSummary financialSummary = dashboardData.financialSummary();

        salesTodayValueLabel.setText(MONEY_FORMAT.format(dailySalesSummary.getTotal()));
        customersValueLabel.setText(String.valueOf(activeCustomers));
        productsValueLabel.setText(String.valueOf(activeProducts.size()));
        balanceValueLabel.setText(MONEY_FORMAT.format(financialSummary.getCurrentBalance()));

        salesTodayFootnoteLabel.setText(dailySalesSummary.getSalesCount() == 1
                ? "1 venda registrada hoje"
                : dailySalesSummary.getSalesCount() + " vendas registradas hoje");
        customersFootnoteLabel.setText(activeCustomers == 1 ? "1 cliente ativo" : activeCustomers + " clientes ativos");
        productsFootnoteLabel.setText(activeProducts.size() == 1 ? "1 produto ativo" : activeProducts.size() + " produtos ativos");
        latestSalesTable.setItems(FXCollections.observableArrayList(dashboardData.latestSales()));
        loadCreditAlerts(dashboardData.creditAlerts(), dashboardData.lowStockProducts());
    }

    private void loadCreditAlerts(List<CreditInstallment> alerts, List<Product> lowStockProducts) {
        alertsList.getChildren().clear();

        LocalDate today = LocalDate.now();
        long overdueCount = alerts.stream()
                .filter(installment -> installment.getDueDate().isBefore(today))
                .count();
        long dueTodayCount = alerts.stream()
                .filter(installment -> installment.getDueDate().isEqual(today))
                .count();

        if (overdueCount > 0) {
            alertsList.getChildren().add(createAlertCard(
                    overdueCount + " " + pluralize(overdueCount, "crediário vencido", "crediários vencidos"),
                    "Clique para abrir a lista de parcelas vencidas.",
                    "system-alert-critical"
            ));
        }

        if (dueTodayCount > 0) {
            alertsList.getChildren().add(createAlertCard(
                    dueTodayCount + " " + pluralize(dueTodayCount, "crediário vence hoje", "crediários vencem hoje"),
                    "Clique para abrir a lista a receber.",
                    "system-alert-warning"
            ));
        }

        if (!lowStockProducts.isEmpty()) {
            alertsList.getChildren().add(createStockAlertCard(lowStockProducts));
        }

        if (alertsList.getChildren().isEmpty()) {
            alertsList.getChildren().add(emptyAlertsLabel);
            emptyAlertsLabel.setText("Nenhum alerta crítico por enquanto.");
        }
    }

    private VBox createAlertCard(String title, String description, String statusClass) {
        Label titleLabel = new Label(title);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("system-alert-title");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setMaxWidth(Double.MAX_VALUE);
        descriptionLabel.setWrapText(true);
        descriptionLabel.getStyleClass().add("system-alert-description");

        VBox alertCard = new VBox(4, titleLabel, descriptionLabel);
        alertCard.setMaxWidth(Double.MAX_VALUE);
        alertCard.setFocusTraversable(true);
        alertCard.setAccessibleText(title + ". " + description + " Abrir detalhes.");
        alertCard.getStyleClass().addAll("system-alert-card", statusClass);
        alertCard.setOnMouseClicked(event -> MainController.openCreditView());
        alertCard.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                MainController.openCreditView();
                event.consume();
            }
        });
        return alertCard;
    }

    private VBox createStockAlertCard(List<Product> products) {
        long count = products.size();
        VBox card = createAlertCard(
                count + " " + pluralize(count, "produto com estoque mínimo", "produtos com estoque mínimo"),
                "Clique para abrir os produtos e ajustar o estoque ou o limite mínimo.",
                "system-alert-warning"
        );
        card.setOnMouseClicked(event -> MainController.openProductsView());
        card.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                MainController.openProductsView();
                event.consume();
            }
        });
        return card;
    }

    private String pluralize(long count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }

    private String textValue(String value) {
        return value == null ? "" : value;
    }

    private String displayValue(String value) {
        String normalizedValue = textValue(value).trim();
        return normalizedValue.isBlank() ? "—" : normalizedValue;
    }

    private String messageFor(Throwable exception) {
        String message = exception == null ? null : exception.getMessage();
        return message == null || message.isBlank()
                ? "Não foi possível carregar o dashboard. Consulte o arquivo de log para mais detalhes."
                : message;
    }

    private void showDashboardStatus(String message) {
        boolean hasMessage = message != null && !message.isBlank();
        dashboardStatusLabel.setText(hasMessage ? message : "");
        dashboardStatusLabel.setVisible(hasMessage);
        dashboardStatusLabel.setManaged(hasMessage);
    }

    private record DashboardData(
            int activeCustomers,
            List<Product> activeProducts,
            DailySalesSummary dailySalesSummary,
            FinancialSummary financialSummary,
            List<Sale> latestSales,
            List<CreditInstallment> creditAlerts,
            List<Product> lowStockProducts
    ) {
    }
}
