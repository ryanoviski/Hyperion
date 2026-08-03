package com.hyperion.controller;

import com.hyperion.model.Expense;
import com.hyperion.model.FinancialSummary;
import com.hyperion.service.AttachmentService;
import com.hyperion.service.FinanceService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class FinanceController {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FinanceService financeService = new FinanceService();
    private final AttachmentService attachmentService = new AttachmentService();

    private Path selectedAttachmentPath;

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
    private Button clearAttachmentButton;

    @FXML
    private Label attachmentLabel;

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
    private TableColumn<Expense, String> attachmentColumn;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        configureTableColumns();
        configureSelectionState();
        updateAttachmentSelection(null);
        loadFinanceData();
    }

    @FXML
    private void handleAddExpense() {
        try {
            Long expenseId = financeService.registerExpense(
                    descriptionField.getText(),
                    categoryField.getText(),
                    parseMoney(amountField.getText())
            );

            if (selectedAttachmentPath != null) {
                attachmentService.attachFile(
                        AttachmentService.FINANCE_MODULE,
                        expenseId,
                        selectedAttachmentPath
                );
            }

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
    private void handleSelectAttachment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar comprovante");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagens e PDFs", "*.png", "*.jpg", "*.jpeg", "*.pdf"),
                new FileChooser.ExtensionFilter("Todos os arquivos", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(descriptionField.getScene().getWindow());

        if (selectedFile != null) {
            updateAttachmentSelection(selectedFile.toPath());
        }
    }

    @FXML
    private void handleClearAttachment() {
        updateAttachmentSelection(null);
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
        attachmentColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatAttachmentCount(cellData.getValue())));
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
        updateAttachmentSelection(null);
    }

    private void updateAttachmentSelection(Path attachmentPath) {
        selectedAttachmentPath = attachmentPath;
        clearAttachmentButton.setDisable(attachmentPath == null);

        if (attachmentPath == null) {
            attachmentLabel.setText("Nenhum comprovante selecionado.");
            return;
        }

        attachmentLabel.setText("Selecionado: " + attachmentPath.getFileName());
    }

    private String formatAttachmentCount(Expense expense) {
        int count = attachmentService.countAttachments(AttachmentService.FINANCE_MODULE, expense.getId());
        return count == 0 ? "-" : count + " arquivo(s)";
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }
}
