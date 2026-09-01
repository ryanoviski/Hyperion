package com.hyperion.controller;

import com.hyperion.model.CreditInstallment;
import com.hyperion.service.CreditInstallmentService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CreditController {

    private static final String ALL_FILTER = "Status: Todas";
    private static final String OPEN_FILTER = "Em aberto";
    private static final String OVERDUE_FILTER = "Vencidas";
    private static final String DUE_SOON_FILTER = "Vencendo";
    private static final String PAID_FILTER = "Pagas";
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CreditInstallmentService creditInstallmentService = new CreditInstallmentService();
    private final ObservableList<CreditInstallment> allInstallments = FXCollections.observableArrayList();

    @FXML
    private Label openAmountLabel;

    @FXML
    private Label openCountLabel;

    @FXML
    private Label overdueCountLabel;

    @FXML
    private Label dueSoonCountLabel;

    @FXML
    private TextField searchField;

    @FXML
    private ChoiceBox<String> statusFilterChoiceBox;

    @FXML
    private TableView<CreditInstallment> installmentsTable;

    @FXML
    private TableColumn<CreditInstallment, String> customerColumn;

    @FXML
    private TableColumn<CreditInstallment, String> installmentColumn;

    @FXML
    private TableColumn<CreditInstallment, String> dueDateColumn;

    @FXML
    private TableColumn<CreditInstallment, String> amountColumn;

    @FXML
    private TableColumn<CreditInstallment, String> statusColumn;

    @FXML
    private TableColumn<CreditInstallment, CreditInstallment> actionsColumn;

    @FXML
    private Button markPaidButton;

    @FXML
    private Label messageLabel;

    @FXML
    private Label tableSummaryLabel;

    @FXML
    private void initialize() {
        configureFilters();
        configureInstallmentsTable();
        configureSelectionState();
        loadInstallments();
    }

    @FXML
    private void handleMarkSelectedAsPaid() {
        List<CreditInstallment> selectedInstallments = new ArrayList<>(installmentsTable.getSelectionModel().getSelectedItems());
        List<CreditInstallment> payableInstallments = selectedInstallments.stream()
                .filter(this::canMarkAsPaid)
                .toList();

        if (payableInstallments.isEmpty()) {
            messageLabel.setText("Selecione ao menos uma parcela em aberto ou vencida.");
            return;
        }

        try {
            for (CreditInstallment installment : payableInstallments) {
                creditInstallmentService.markAsPaid(installment);
            }

            loadInstallments();
            messageLabel.setText(payableInstallments.size() + " parcela(s) marcada(s) como paga(s).");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadInstallments();
        messageLabel.setText("Crediário atualizado.");
    }

    private void configureFilters() {
        statusFilterChoiceBox.setItems(FXCollections.observableArrayList(
                ALL_FILTER,
                OPEN_FILTER,
                OVERDUE_FILTER,
                DUE_SOON_FILTER,
                PAID_FILTER
        ));
        statusFilterChoiceBox.setValue(ALL_FILTER);
        statusFilterChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void configureInstallmentsTable() {
        installmentsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        installmentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        customerColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getCustomerName()));
        installmentColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatInstallment(cellData.getValue())));
        dueDateColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatDate(cellData.getValue())));
        amountColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getAmount())));
        statusColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatStatus(cellData.getValue())));
        statusColumn.setCellFactory(column -> createStatusCell());
        actionsColumn.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue()));
        actionsColumn.setCellFactory(column -> createActionsCell());
    }

    private TableCell<CreditInstallment, String> createStatusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }

                Label badge = new Label(status);
                badge.getStyleClass().addAll("status-badge", statusStyleClass(status));
                setGraphic(badge);
            }
        };
    }

    private TableCell<CreditInstallment, CreditInstallment> createActionsCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(CreditInstallment installment, boolean empty) {
                super.updateItem(installment, empty);

                if (empty || installment == null) {
                    setGraphic(null);
                    return;
                }

                Button payButton = new Button("Pagar");
                payButton.getStyleClass().add("action-text-button");
                payButton.setDisable(!canMarkAsPaid(installment));
                payButton.setOnAction(event -> markSingleInstallmentAsPaid(installment));

                Button detailsButton = new Button("Detalhes");
                detailsButton.getStyleClass().add("action-text-button");
                detailsButton.setOnAction(event -> showDetailsDialog(installment));

                HBox actions = new HBox(8, payButton, detailsButton);
                actions.setAlignment(Pos.CENTER);
                setGraphic(actions);
            }
        };
    }

    private void configureSelectionState() {
        markPaidButton.setDisable(true);
        installmentsTable.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<CreditInstallment>) change ->
                markPaidButton.setDisable(installmentsTable.getSelectionModel().getSelectedItems().stream().noneMatch(this::canMarkAsPaid))
        );
    }

    private void markSingleInstallmentAsPaid(CreditInstallment installment) {
        try {
            creditInstallmentService.markAsPaid(installment);
            loadInstallments();
            messageLabel.setText("Parcela marcada como paga.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    private void loadInstallments() {
        List<CreditInstallment> openInstallments = creditInstallmentService.listOpenInstallments();
        List<CreditInstallment> paidInstallments = creditInstallmentService.listPaidInstallments();

        allInstallments.setAll(openInstallments);
        allInstallments.addAll(paidInstallments);
        updateSummary(openInstallments);
        applyFilters();
    }

    private void applyFilters() {
        String term = normalize(searchField.getText());
        String selectedFilter = statusFilterChoiceBox.getValue();

        List<CreditInstallment> filteredInstallments = allInstallments.stream()
                .filter(installment -> matchesSearch(installment, term))
                .filter(installment -> matchesStatusFilter(installment, selectedFilter))
                .toList();

        installmentsTable.setItems(FXCollections.observableArrayList(filteredInstallments));
        tableSummaryLabel.setText(filteredInstallments.size() + " parcela(s)");
    }

    private boolean matchesSearch(CreditInstallment installment, String term) {
        return term.isBlank() || normalize(installment.getCustomerName()).contains(term);
    }

    private boolean matchesStatusFilter(CreditInstallment installment, String selectedFilter) {
        if (selectedFilter == null || ALL_FILTER.equals(selectedFilter)) {
            return true;
        }

        return selectedFilter.equals(formatStatus(installment));
    }

    private void updateSummary(List<CreditInstallment> openInstallments) {
        LocalDate today = LocalDate.now();
        BigDecimal openAmount = BigDecimal.ZERO;
        int overdueCount = 0;
        int dueSoonCount = 0;

        for (CreditInstallment installment : openInstallments) {
            openAmount = openAmount.add(installment.getAmount());

            if (installment.getDueDate().isBefore(today)) {
                overdueCount++;
            } else if (!installment.getDueDate().isAfter(today.plusDays(3))) {
                dueSoonCount++;
            }
        }

        openAmountLabel.setText(formatMoney(openAmount));
        openCountLabel.setText(openInstallments.size() + " parcela(s) não paga(s)");
        overdueCountLabel.setText(String.valueOf(overdueCount));
        dueSoonCountLabel.setText(String.valueOf(dueSoonCount));
    }

    private void showDetailsDialog(CreditInstallment installment) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Detalhes da parcela");
        dialog.setHeaderText(installment.getCustomerName());
        dialog.initOwner(installmentsTable.getScene().getWindow());
        addDialogStyles(dialog);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(8,
                new Label("Parcela: " + formatInstallment(installment)),
                new Label("Vencimento: " + formatDate(installment)),
                new Label("Valor: " + formatMoney(installment.getAmount())),
                new Label("Status: " + formatStatus(installment)),
                new Label("Venda ID: #" + installment.getSaleId())
        );
        content.setPrefWidth(360);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void addDialogStyles(Dialog<?> dialog) {
        String stylesheet = CreditController.class.getResource("/css/app.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(stylesheet);
    }

    private boolean canMarkAsPaid(CreditInstallment installment) {
        return installment != null && "OPEN".equals(installment.getStatus());
    }

    private String formatInstallment(CreditInstallment installment) {
        return installment.getInstallmentNumber() + "/" + installment.getTotalInstallments();
    }

    private String formatDate(CreditInstallment installment) {
        return installment.getDueDate().format(DATE_FORMAT);
    }

    private String formatStatus(CreditInstallment installment) {
        if ("PAID".equals(installment.getStatus())) {
            return PAID_FILTER;
        }

        LocalDate today = LocalDate.now();

        if (installment.getDueDate().isBefore(today)) {
            return OVERDUE_FILTER;
        }

        if (!installment.getDueDate().isAfter(today.plusDays(3))) {
            return DUE_SOON_FILTER;
        }

        return OPEN_FILTER;
    }

    private String statusStyleClass(String status) {
        return switch (status) {
            case PAID_FILTER -> "status-paid";
            case OVERDUE_FILTER -> "status-overdue";
            case DUE_SOON_FILTER -> "status-due-soon";
            default -> "status-open";
        };
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
