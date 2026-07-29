package com.hyperion.controller;

import com.hyperion.model.CreditInstallment;
import com.hyperion.service.CreditInstallmentService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CreditController {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CreditInstallmentService creditInstallmentService = new CreditInstallmentService();

    @FXML
    private Label openAmountLabel;

    @FXML
    private Label overdueCountLabel;

    @FXML
    private Label dueSoonCountLabel;

    @FXML
    private TableView<CreditInstallment> openInstallmentsTable;

    @FXML
    private TableColumn<CreditInstallment, String> openCustomerColumn;

    @FXML
    private TableColumn<CreditInstallment, String> openInstallmentColumn;

    @FXML
    private TableColumn<CreditInstallment, String> openDueDateColumn;

    @FXML
    private TableColumn<CreditInstallment, String> openAmountColumn;

    @FXML
    private TableColumn<CreditInstallment, String> openStatusColumn;

    @FXML
    private TableView<CreditInstallment> paidInstallmentsTable;

    @FXML
    private TableColumn<CreditInstallment, String> paidCustomerColumn;

    @FXML
    private TableColumn<CreditInstallment, String> paidInstallmentColumn;

    @FXML
    private TableColumn<CreditInstallment, String> paidDueDateColumn;

    @FXML
    private TableColumn<CreditInstallment, String> paidAmountColumn;

    @FXML
    private Button markPaidButton;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        configureOpenInstallmentsTable();
        configurePaidInstallmentsTable();
        configureSelectionState();
        loadInstallments();
    }

    @FXML
    private void handleMarkAsPaid() {
        try {
            creditInstallmentService.markAsPaid(openInstallmentsTable.getSelectionModel().getSelectedItem());
            loadInstallments();
            messageLabel.setText("Parcela marcada como paga.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadInstallments();
        messageLabel.setText("Crediário atualizado.");
    }

    private void configureOpenInstallmentsTable() {
        openCustomerColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getCustomerName()));
        openInstallmentColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatInstallment(cellData.getValue())));
        openDueDateColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatDate(cellData.getValue())));
        openAmountColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getAmount())));
        openStatusColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatOpenStatus(cellData.getValue())));
    }

    private void configurePaidInstallmentsTable() {
        paidCustomerColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getCustomerName()));
        paidInstallmentColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatInstallment(cellData.getValue())));
        paidDueDateColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatDate(cellData.getValue())));
        paidAmountColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getAmount())));
    }

    private void configureSelectionState() {
        markPaidButton.setDisable(true);
        openInstallmentsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedInstallment) ->
                markPaidButton.setDisable(selectedInstallment == null)
        );
    }

    private void loadInstallments() {
        List<CreditInstallment> openInstallments = creditInstallmentService.listOpenInstallments();
        List<CreditInstallment> paidInstallments = creditInstallmentService.listPaidInstallments();

        openInstallmentsTable.setItems(FXCollections.observableArrayList(openInstallments));
        paidInstallmentsTable.setItems(FXCollections.observableArrayList(paidInstallments));
        updateSummary(openInstallments);
    }

    private void updateSummary(List<CreditInstallment> installments) {
        LocalDate today = LocalDate.now();
        BigDecimal openAmount = BigDecimal.ZERO;
        int overdueCount = 0;
        int dueSoonCount = 0;

        for (CreditInstallment installment : installments) {
            openAmount = openAmount.add(installment.getAmount());

            if (installment.getDueDate().isBefore(today)) {
                overdueCount++;
            } else if (!installment.getDueDate().isAfter(today.plusDays(3))) {
                dueSoonCount++;
            }
        }

        openAmountLabel.setText(formatMoney(openAmount));
        overdueCountLabel.setText(String.valueOf(overdueCount));
        dueSoonCountLabel.setText(String.valueOf(dueSoonCount));
    }

    private String formatInstallment(CreditInstallment installment) {
        return installment.getInstallmentNumber() + "/" + installment.getTotalInstallments();
    }

    private String formatDate(CreditInstallment installment) {
        return installment.getDueDate().format(DATE_FORMAT);
    }

    private String formatOpenStatus(CreditInstallment installment) {
        LocalDate today = LocalDate.now();

        if (installment.getDueDate().isBefore(today)) {
            return "Vencida";
        }

        if (installment.getDueDate().isEqual(today)) {
            return "Vence hoje";
        }

        if (!installment.getDueDate().isAfter(today.plusDays(3))) {
            return "A vencer";
        }

        return "Em aberto";
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }
}
