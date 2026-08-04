package com.hyperion.controller;

import com.hyperion.model.Attachment;
import com.hyperion.model.Expense;
import com.hyperion.model.FinancialSummary;
import com.hyperion.service.AttachmentService;
import com.hyperion.service.FinanceService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
    private Button viewAttachmentsButton;

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
    private void handleViewAttachments() {
        Expense selectedExpense = expensesTable.getSelectionModel().getSelectedItem();

        if (selectedExpense == null) {
            showMessage("Selecione uma despesa para ver os anexos.");
            return;
        }

        List<Attachment> attachments = attachmentService.listAttachments(
                AttachmentService.FINANCE_MODULE,
                selectedExpense.getId()
        );

        if (attachments.isEmpty()) {
            showMessage("Esta despesa não possui anexos.");
            return;
        }

        showAttachmentsDialog(selectedExpense, attachments);
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
        descriptionColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(textValue(cellData.getValue().getDescription())));
        categoryColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(textValue(cellData.getValue().getCategory())));
        amountColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getAmount())));
        attachmentColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatAttachmentCount(cellData.getValue())));
    }

    private void configureSelectionState() {
        updateActionButtons(null);
        expensesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedExpense) ->
                updateActionButtons(selectedExpense)
        );
    }

    private void updateActionButtons(Expense selectedExpense) {
        boolean hasSelection = selectedExpense != null;
        boolean hasAttachments = hasSelection
                && attachmentService.countAttachments(AttachmentService.FINANCE_MODULE, selectedExpense.getId()) > 0;

        removeExpenseButton.setDisable(!hasSelection);
        viewAttachmentsButton.setDisable(!hasAttachments);
    }

    private void loadFinanceData() {
        FinancialSummary summary = financeService.getSummary();
        List<Expense> expenses = financeService.listLatestExpenses();

        totalIncomeLabel.setText(formatMoney(summary.getTotalIncome()));
        totalExpensesLabel.setText(formatMoney(summary.getTotalExpenses()));
        currentBalanceLabel.setText(formatMoney(summary.getCurrentBalance()));
        monthlyProfitLabel.setText(formatMoney(summary.getMonthlyProfit()));
        expensesTable.setItems(FXCollections.observableArrayList(expenses));
        updateActionButtons(expensesTable.getSelectionModel().getSelectedItem());
    }

    private void showAttachmentsDialog(Expense expense, List<Attachment> attachments) {
        Dialog<Attachment> dialog = new Dialog<>();
        dialog.setTitle("Anexos da despesa");
        dialog.setHeaderText(expense.getDescription());
        dialog.initOwner(expensesTable.getScene().getWindow());
        addDialogStyles(dialog);

        ButtonType previewButtonType = new ButtonType("Visualizar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(previewButtonType, ButtonType.CLOSE);

        TableView<Attachment> attachmentsTable = createAttachmentsTable();
        attachmentsTable.setItems(FXCollections.observableArrayList(attachments));

        Node previewButton = dialog.getDialogPane().lookupButton(previewButtonType);
        previewButton.disableProperty().bind(attachmentsTable.getSelectionModel().selectedItemProperty().isNull());

        VBox content = new VBox(12, attachmentsTable);
        content.setPrefWidth(720);
        content.setPrefHeight(360);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != previewButtonType) {
                return null;
            }

            return attachmentsTable.getSelectionModel().getSelectedItem();
        });

        dialog.showAndWait().ifPresent(attachment -> {
            try {
                showAttachmentPreviewDialog(attachment);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                showMessage(exception.getMessage());
            }
        });
    }

    private TableView<Attachment> createAttachmentsTable() {
        TableView<Attachment> table = new TableView<>();
        table.setPrefHeight(300);

        TableColumn<Attachment, String> nameColumn = new TableColumn<>("Arquivo");
        nameColumn.setPrefWidth(280);
        nameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getOriginalName()));

        TableColumn<Attachment, String> sizeColumn = new TableColumn<>("Tamanho");
        sizeColumn.setPrefWidth(120);
        sizeColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatFileSize(cellData.getValue().getFileSize())));

        TableColumn<Attachment, String> pathColumn = new TableColumn<>("Local");
        pathColumn.setPrefWidth(320);
        pathColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getFilePath()));

        table.getColumns().addAll(nameColumn, sizeColumn, pathColumn);
        return table;
    }

    private void showAttachmentPreviewDialog(Attachment attachment) {
        Path filePath = attachmentService.resolveAttachmentPath(attachment);
        String extension = getFileExtension(filePath);

        if (isImage(extension)) {
            showImagePreviewDialog(attachment, filePath);
            return;
        }

        if ("pdf".equals(extension)) {
            showPdfPreviewDialog(attachment, filePath);
            return;
        }

        showMessage("Formato de anexo não suportado para visualização interna.");
    }

    private void showImagePreviewDialog(Attachment attachment, Path filePath) {
        ImageView imageView = new ImageView(new Image(filePath.toUri().toString()));
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(840);

        ScrollPane scrollPane = new ScrollPane(imageView);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(880);
        scrollPane.setPrefViewportHeight(620);

        showPreviewDialog(attachment.getOriginalName(), scrollPane);
    }

    private void showPdfPreviewDialog(Attachment attachment, Path filePath) {
        VBox pagesContainer = new VBox(18);

        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);

            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(pageIndex, 130);
                ImageView pageView = new ImageView(toFxImage(pageImage));
                pageView.setPreserveRatio(true);
                pageView.setFitWidth(840);
                pagesContainer.getChildren().add(pageView);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível renderizar o PDF.", exception);
        }

        ScrollPane scrollPane = new ScrollPane(pagesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(880);
        scrollPane.setPrefViewportHeight(620);

        showPreviewDialog(attachment.getOriginalName(), scrollPane);
    }

    private void showPreviewDialog(String title, Node content) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Visualizar anexo");
        dialog.setHeaderText(title);
        dialog.initOwner(expensesTable.getScene().getWindow());
        addDialogStyles(dialog);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private WritableImage toFxImage(BufferedImage bufferedImage) {
        WritableImage writableImage = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
        PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                pixelWriter.setArgb(x, y, bufferedImage.getRGB(x, y));
            }
        }

        return writableImage;
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

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        double kilobytes = bytes / 1024.0;

        if (kilobytes < 1024) {
            return String.format(Locale.of("pt", "BR"), "%.1f KB", kilobytes);
        }

        double megabytes = kilobytes / 1024.0;
        return String.format(Locale.of("pt", "BR"), "%.1f MB", megabytes);
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }

    private String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int extensionStart = fileName.lastIndexOf('.');

        if (extensionStart < 0 || extensionStart == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isImage(String extension) {
        return "png".equals(extension)
                || "jpg".equals(extension)
                || "jpeg".equals(extension);
    }

    private void addDialogStyles(Dialog<?> dialog) {
        String stylesheet = FinanceController.class.getResource("/css/app.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(stylesheet);
    }

    private String textValue(String value) {
        return value == null ? "" : value;
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }
}
