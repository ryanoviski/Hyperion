package com.hyperion.controller;

import com.hyperion.model.Expense;
import com.hyperion.model.FinancialSummary;
import com.hyperion.service.FinanceService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class FinanceController {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FinanceService financeService = new FinanceService();

    @FXML
    private Label totalIncomeLabel;

    @FXML
    private Label totalExpensesLabel;

    @FXML
    private Label currentBalanceLabel;

    @FXML
    private Label monthlyProfitLabel;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField categoryField;

    @FXML
    private TextField amountField;

    @FXML
    private Button removeExpenseButton;

    @FXML
    private TableView<Expense> expensesTable;

    @FXML
    private TableColumn<Expense, String> dateColumn;

    @FXML
    private TableColumn<Expense, String> descriptionColumn;

    @FXML
    private TableColumn<Expense, String> categoryColumn;

    @FXML
    private TableColumn<Expense, String> amountColumn;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        configureTableColumns();
        configureSelectionState();
        loadFinanceData();
    }

    @FXML
    private void handleAddExpense() {
        try {
            financeService.registerExpense(
                    descriptionField.getText(),
                    categoryField.getText(),
                    parseMoney(amountField.getText())
            );

            clearForm();
            loadFinanceData();
            showMessage("Despesa registrada com sucesso.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    @FXML
    private void handleRemoveExpense() {
        try {
            financeService.deleteExpense(expensesTable.getSelectionModel().getSelectedItem());
            loadFinanceData();
            showMessage("Despesa removida com sucesso.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadFinanceData();
        showMessage("Financeiro atualizado.");
    }

    private void configureTableColumns() {
        dateColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                cellData.getValue().getCreatedAt().format(DATE_TIME_FORMAT)
        ));
        descriptionColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getDescription()));
        categoryColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getCategory()));
        amountColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getAmount())));
    }

    private void configureSelectionState() {
        removeExpenseButton.setDisable(true);
        expensesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedExpense) ->
                removeExpenseButton.setDisable(selectedExpense == null)
        );
    }

    private void loadFinanceData() {
        FinancialSummary summary = financeService.getSummary();
        List<Expense> expenses = financeService.listLatestExpenses();

        totalIncomeLabel.setText(formatMoney(summary.getTotalIncome()));
        totalExpensesLabel.setText(formatMoney(summary.getTotalExpenses()));
        currentBalanceLabel.setText(formatMoney(summary.getCurrentBalance()));
        monthlyProfitLabel.setText(formatMoney(summary.getMonthlyProfit()));
        expensesTable.setItems(FXCollections.observableArrayList(expenses));
    }

    private BigDecimal parseMoney(String value) {
        String normalizedValue = value == null ? "" : value.replace(",", ".").trim();

        if (normalizedValue.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(normalizedValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Informe um valor válido.");
        }
    }

    private void clearForm() {
        descriptionField.clear();
        categoryField.clear();
        amountField.clear();
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }
}
