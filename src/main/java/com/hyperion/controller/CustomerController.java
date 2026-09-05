package com.hyperion.controller;

import com.hyperion.model.Customer;
import com.hyperion.model.Sale;
import com.hyperion.service.CustomerService;
import com.hyperion.util.ThemeManager;
import com.hyperion.service.SaleService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CustomerController {

    private static final String ACTIVE_FILTER = "Ativos";
    private static final String INACTIVE_FILTER = "Inativos";
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String PERSON_ICON = "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z";
    private static final String EDIT_ICON = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zm17.71-10.04c.39-.39.39-1.02 0-1.41l-2.51-2.51a.9959.9959 0 0 0-1.41 0l-1.96 1.96L18.75 9.17l1.96-1.96z";
    private static final String HIDE_ICON = "M12 6.5c3.79 0 6.17 2.13 7.44 3.76-.45.58-1.03 1.22-1.76 1.82L19.1 13.5c1.15-.94 2.02-2.1 2.54-3.01.18-.31.18-.69 0-1C20.62 7.73 17.46 4.5 12 4.5c-1.39 0-2.61.21-3.67.55l1.62 1.62c.64-.11 1.32-.17 2.05-.17zM2.71 3.16 1.39 4.48l3.1 3.1c-.9.77-1.6 1.62-2.13 2.41-.2.3-.2.7 0 1C3.38 12.76 6.54 16 12 16c1.27 0 2.41-.18 3.42-.48l3.1 3.09 1.32-1.32L2.71 3.16zM12 14c-3.79 0-6.17-2.13-7.44-3.76.35-.45.8-.94 1.34-1.4l1.45 1.45c-.03.17-.05.34-.05.51 0 1.49 1.21 2.7 2.7 2.7.17 0 .34-.02.51-.05l1.5 1.5H12zm.67-2.07-3.6-3.6c.31-.15.66-.23 1.03-.23 1.49 0 2.7 1.21 2.7 2.7 0 .4-.05.77-.13 1.13z";
    private static final String SHOW_ICON = "M12 4.5c-5.46 0-8.62 3.23-9.64 4.99-.18.31-.18.69 0 1C3.38 12.27 6.54 15.5 12 15.5s8.62-3.23 9.64-5.01c.18-.31.18-.69 0-1C20.62 7.73 17.46 4.5 12 4.5zm0 9c-3.79 0-6.17-2.13-7.44-3.76C5.83 8.11 8.21 6.5 12 6.5s6.17 1.61 7.44 3.24C18.17 11.37 15.79 13.5 12 13.5zm0-5.5c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z";

    private final CustomerService customerService = new CustomerService();
    private final SaleService saleService = new SaleService();

    @FXML
    private TextField searchField;

    @FXML
    private ChoiceBox<String> statusFilterChoiceBox;

    @FXML
    private TableView<Customer> customersTable;

    @FXML
    private TableColumn<Customer, String> nameColumn;

    @FXML
    private TableColumn<Customer, String> documentColumn;

    @FXML
    private TableColumn<Customer, String> phoneColumn;

    @FXML
    private TableColumn<Customer, String> emailColumn;

    @FXML
    private TableColumn<Customer, String> statusColumn;

    @FXML
    private TableColumn<Customer, Customer> actionsColumn;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        configureFilter();
        configureTableColumns();
        loadCustomers();
    }

    @FXML
    private void handleSearch() {
        loadCustomers();
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        loadCustomers();
    }

    @FXML
    private void handleNewCustomer() {
        Optional<CustomerFormData> result = showCustomerDialog("Novo cliente", null);

        result.ifPresent(formData -> {
            try {
                customerService.createCustomer(
                        formData.name(),
                        formData.document(),
                        formData.phone(),
                        formData.email(),
                        formData.address(),
                        formData.notes()
                );

                statusFilterChoiceBox.setValue(ACTIVE_FILTER);
                loadCustomers();
                showMessage("Cliente cadastrado: " + formData.name() + ".");
            } catch (IllegalArgumentException | IllegalStateException exception) {
                showMessage(exception.getMessage());
            }
        });
    }

    private void handleEditCustomer(Customer selectedCustomer) {
        Optional<CustomerFormData> result = showCustomerDialog("Editar cliente", selectedCustomer);

        result.ifPresent(formData -> {
            Customer updatedCustomer = new Customer(
                    selectedCustomer.getId(),
                    formData.name(),
                    formData.document(),
                    formData.phone(),
                    formData.email(),
                    formData.address(),
                    formData.notes(),
                    selectedCustomer.isActive(),
                    selectedCustomer.getCreatedAt(),
                    selectedCustomer.getUpdatedAt()
            );

            try {
                customerService.updateCustomer(updatedCustomer);
                loadCustomers();
                showMessage("Cliente atualizado: " + formData.name() + ".");
            } catch (IllegalArgumentException | IllegalStateException exception) {
                showMessage(exception.getMessage());
            }
        });
    }

    private void handleDeactivateCustomer(Customer selectedCustomer) {
        try {
            customerService.deactivateCustomer(selectedCustomer.getId());
            loadCustomers();
            showMessage("Cliente desativado: " + selectedCustomer.getName() + ".");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void handleReactivateCustomer(Customer selectedCustomer) {
        try {
            customerService.reactivateCustomer(selectedCustomer.getId());
            loadCustomers();
            showMessage("Cliente reativado: " + selectedCustomer.getName() + ".");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void configureFilter() {
        statusFilterChoiceBox.setItems(FXCollections.observableArrayList(ACTIVE_FILTER, INACTIVE_FILTER));
        statusFilterChoiceBox.setValue(ACTIVE_FILTER);
        statusFilterChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> loadCustomers());
    }

    private void configureTableColumns() {
        customersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        nameColumn.getStyleClass().add("left-aligned-column");
        emailColumn.getStyleClass().add("left-aligned-column");

        nameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(displayValue(cellData.getValue().getName())));
        documentColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatDocument(cellData.getValue().getDocument())));
        phoneColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatPhone(cellData.getValue().getPhone())));
        emailColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(displayValue(cellData.getValue().getEmail())));
        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().isActive() ? "Ativo" : "Inativo";
            return new ReadOnlyStringWrapper(status);
        });
        statusColumn.setCellFactory(column -> createStatusCell());

        actionsColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        actionsColumn.setCellFactory(column -> createActionsCell());
    }

    private TableCell<Customer, String> createStatusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(status);
                badge.getStyleClass().addAll("status-badge", "Ativo".equals(status) ? "status-active" : "status-inactive");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private TableCell<Customer, Customer> createActionsCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);

                if (empty || customer == null) {
                    setGraphic(null);
                    return;
                }

                Button profileButton = createIconButton("Perfil", PERSON_ICON, () -> showCustomerProfileDialog(customer));
                Button editButton = createIconButton("Editar", EDIT_ICON, () -> handleEditCustomer(customer));
                Button activeToggleButton = customer.isActive()
                        ? createIconButton("Desativar", HIDE_ICON, () -> handleDeactivateCustomer(customer))
                        : createIconButton("Reativar", SHOW_ICON, () -> handleReactivateCustomer(customer));

                HBox actions = new HBox(10, profileButton, editButton, activeToggleButton);
                actions.setAlignment(Pos.CENTER);
                actions.setMinWidth(128);
                setGraphic(actions);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private Button createIconButton(String tooltipText, String svgContent, Runnable action) {
        SVGPath icon = new SVGPath();
        icon.setContent(svgContent);
        icon.getStyleClass().add("action-icon-shape");

        Button button = new Button();
        button.setGraphic(icon);
        button.setTooltip(new Tooltip(tooltipText));
        button.setMinSize(36, 36);
        button.setPrefSize(36, 36);
        button.setMaxSize(36, 36);
        button.getStyleClass().add("action-icon-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private void loadCustomers() {
        boolean showingActive = isShowingActive();
        String searchTerm = searchField.getText();
        List<Customer> customers = showingActive
                ? customerService.searchActiveCustomers(searchTerm)
                : customerService.searchInactiveCustomers(searchTerm);

        customersTable.setItems(FXCollections.observableArrayList(customers));
        showMessage(customers.size() + (showingActive ? " cliente(s) ativo(s)." : " cliente(s) inativo(s)."));
    }

    private boolean isShowingActive() {
        return !INACTIVE_FILTER.equals(statusFilterChoiceBox.getValue());
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }

    private Optional<CustomerFormData> showCustomerDialog(String title, Customer customer) {
        Dialog<CustomerFormData> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.initOwner(customersTable.getScene().getWindow());
        addDialogStyles(dialog);

        ButtonType saveButtonType = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(createCustomerForm(customer));

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(customerNameField.textProperty().isEmpty());

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }

            return new CustomerFormData(
                    customerNameField.getText(),
                    customerDocumentField.getText(),
                    customerPhoneField.getText(),
                    customerEmailField.getText(),
                    customerAddressField.getText(),
                    customerNotesArea.getText()
            );
        });

        return dialog.showAndWait();
    }

    private void showCustomerProfileDialog(Customer customer) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Perfil do cliente");
        dialog.setHeaderText(customer.getName());
        dialog.initOwner(customersTable.getScene().getWindow());
        addDialogStyles(dialog);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(createCustomerProfileScrollContent(customer));
        dialog.showAndWait();
    }

    private ScrollPane createCustomerProfileScrollContent(Customer customer) {
        ScrollPane scrollPane = new ScrollPane(createCustomerProfileContent(customer));
        scrollPane.getStyleClass().add("dialog-content");
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(760);
        scrollPane.setPrefViewportHeight(420);
        return scrollPane;
    }

    private VBox createCustomerProfileContent(Customer customer) {
        VBox content = new VBox(14);
        content.getStyleClass().add("dialog-content");
        content.setPadding(new Insets(16));
        content.setPrefWidth(720);
        content.setMaxWidth(Double.MAX_VALUE);

        Label documentLabel = new Label("Documento: " + formatDocument(customer.getDocument()));
        Label phoneLabel = new Label("Telefone: " + formatPhone(customer.getPhone()));
        Label emailLabel = new Label("Email: " + displayValue(customer.getEmail()));
        Label addressLabel = new Label("Endereço: " + displayValue(customer.getAddress()));
        Label purchasesTitle = new Label("Histórico de compras");
        purchasesTitle.getStyleClass().add("panel-title");

        content.getChildren().addAll(
                documentLabel,
                phoneLabel,
                emailLabel,
                addressLabel,
                purchasesTitle,
                createPurchasesTable(customer)
        );

        return content;
    }

    private TableView<Sale> createPurchasesTable(Customer customer) {
        TableView<Sale> purchasesTable = new TableView<>();
        purchasesTable.setPrefHeight(260);
        purchasesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Sale, String> dateColumn = new TableColumn<>("Data");
        dateColumn.setPrefWidth(180);
        dateColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                cellData.getValue().getCreatedAt().format(DATE_TIME_FORMAT)
        ));

        TableColumn<Sale, String> paymentColumn = new TableColumn<>("Pagamento");
        paymentColumn.setPrefWidth(220);
        paymentColumn.getStyleClass().add("left-aligned-column");
        paymentColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(displayValue(cellData.getValue().getPaymentMethod())));

        TableColumn<Sale, String> totalColumn = new TableColumn<>("Total");
        totalColumn.setPrefWidth(160);
        totalColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                MONEY_FORMAT.format(cellData.getValue().getTotal())
        ));

        TableColumn<Sale, String> discountColumn = new TableColumn<>("Desconto");
        discountColumn.setPrefWidth(160);
        discountColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                MONEY_FORMAT.format(cellData.getValue().getDiscount())
        ));

        purchasesTable.getColumns().addAll(dateColumn, paymentColumn, totalColumn, discountColumn);
        purchasesTable.setItems(FXCollections.observableArrayList(saleService.listCustomerPurchases(customer.getId())));

        if (purchasesTable.getItems().isEmpty()) {
            showMessage("Cliente sem compras registradas.");
        }

        return purchasesTable;
    }

    private GridPane createCustomerForm(Customer customer) {
        customerNameField = new TextField();
        customerDocumentField = new TextField();
        customerPhoneField = new TextField();
        customerEmailField = new TextField();
        customerAddressField = new TextField();
        customerNotesArea = new TextArea();

        customerNameField.setPromptText("Nome do cliente");
        customerDocumentField.setPromptText("CPF/CNPJ");
        customerPhoneField.setPromptText("Telefone");
        customerEmailField.setPromptText("Email");
        customerAddressField.setPromptText("Endereço");
        customerNotesArea.setPromptText("Observações");
        customerNotesArea.setPrefRowCount(3);

        if (customer != null) {
            customerNameField.setText(textValue(customer.getName()));
            customerDocumentField.setText(textValue(customer.getDocument()));
            customerPhoneField.setText(textValue(customer.getPhone()));
            customerEmailField.setText(textValue(customer.getEmail()));
            customerAddressField.setText(textValue(customer.getAddress()));
            customerNotesArea.setText(textValue(customer.getNotes()));
        }

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.setPadding(new Insets(16));
        form.setPrefWidth(520);
        form.getColumnConstraints().addAll(createLabelColumn(), createFieldColumn());

        form.add(new Label("Nome"), 0, 0);
        form.add(customerNameField, 1, 0);
        form.add(new Label("Documento"), 0, 1);
        form.add(customerDocumentField, 1, 1);
        form.add(new Label("Telefone"), 0, 2);
        form.add(customerPhoneField, 1, 2);
        form.add(new Label("Email"), 0, 3);
        form.add(customerEmailField, 1, 3);
        form.add(new Label("Endereço"), 0, 4);
        form.add(customerAddressField, 1, 4);
        form.add(new Label("Observações"), 0, 5);
        form.add(customerNotesArea, 1, 5);

        return form;
    }

    private ColumnConstraints createLabelColumn() {
        ColumnConstraints column = new ColumnConstraints();
        column.setMinWidth(120);
        column.setPrefWidth(120);
        return column;
    }

    private ColumnConstraints createFieldColumn() {
        ColumnConstraints column = new ColumnConstraints();
        column.setMinWidth(360);
        column.setPrefWidth(360);
        return column;
    }

    private String formatDocument(String value) {
        String digits = digitsOnly(value);

        if (digits.length() == 11) {
            return digits.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }

        if (digits.length() == 14) {
            return digits.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }

        return displayValue(value);
    }

    private String formatPhone(String value) {
        String digits = digitsOnly(value);

        if (digits.length() == 11) {
            return digits.replaceFirst("(\\d{2})(\\d{5})(\\d{4})", "($1) $2-$3");
        }

        if (digits.length() == 10) {
            return digits.replaceFirst("(\\d{2})(\\d{4})(\\d{4})", "($1) $2-$3");
        }

        return displayValue(value);
    }

    private String digitsOnly(String value) {
        return textValue(value).replaceAll("\\D", "");
    }

    private String textValue(String value) {
        return value == null ? "" : value;
    }

    private String displayValue(String value) {
        String normalizedValue = textValue(value).trim();
        return normalizedValue.isBlank() ? "—" : normalizedValue;
    }

    private void addDialogStyles(Dialog<?> dialog) {
        ThemeManager.applyTo(dialog.getDialogPane());
    }

    private TextField customerNameField;
    private TextField customerDocumentField;
    private TextField customerPhoneField;
    private TextField customerEmailField;
    private TextField customerAddressField;
    private TextArea customerNotesArea;

    private record CustomerFormData(
            String name,
            String document,
            String phone,
            String email,
            String address,
            String notes
    ) {
    }
}
