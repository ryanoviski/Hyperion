package com.hyperion.controller;

import com.hyperion.model.Customer;
import com.hyperion.model.Sale;
import com.hyperion.service.CustomerService;
import com.hyperion.service.SaleService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CustomerController {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CustomerService customerService = new CustomerService();
    private final SaleService saleService = new SaleService();

    @FXML
    private TextField searchField;

    @FXML
    private Button editCustomerButton;

    @FXML
    private Button deactivateCustomerButton;

    @FXML
    private Button profileCustomerButton;

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
    private Label messageLabel;

    @FXML
    private void initialize() {
        configureTableColumns();
        configureSelectionState();
        loadCustomers();
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText();
        List<Customer> customers = customerService.searchActiveCustomers(searchTerm);
        updateTable(customers);
        showMessage(customers.size() + " cliente(s) encontrado(s).");
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

                loadCustomers();
                showMessage("Cliente cadastrado: " + formData.name() + ".");
            } catch (IllegalArgumentException | IllegalStateException exception) {
                showMessage(exception.getMessage());
            }
        });
    }

    @FXML
    private void handleEditCustomer() {
        Customer selectedCustomer = getSelectedCustomer();

        if (selectedCustomer == null) {
            showMessage("Selecione um cliente para editar.");
            return;
        }

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

    @FXML
    private void handleCustomerProfile() {
        Customer selectedCustomer = getSelectedCustomer();

        if (selectedCustomer == null) {
            showMessage("Selecione um cliente para visualizar o perfil.");
            return;
        }

        showCustomerProfileDialog(selectedCustomer);
    }

    @FXML
    private void handleDeactivateCustomer() {
        Customer selectedCustomer = getSelectedCustomer();

        if (selectedCustomer == null) {
            showMessage("Selecione um cliente para desativar.");
            return;
        }

        try {
            customerService.deactivateCustomer(selectedCustomer.getId());
            loadCustomers();
            showMessage("Cliente desativado: " + selectedCustomer.getName() + ".");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void configureTableColumns() {
        nameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getName()));
        documentColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getDocument()));
        phoneColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getPhone()));
        emailColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getEmail()));
        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().isActive() ? "Ativo" : "Inativo";
            return new ReadOnlyStringWrapper(status);
        });
    }

    private void configureSelectionState() {
        profileCustomerButton.setDisable(true);
        editCustomerButton.setDisable(true);
        deactivateCustomerButton.setDisable(true);

        customersTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedCustomer) -> {
            boolean hasSelection = selectedCustomer != null;
            profileCustomerButton.setDisable(!hasSelection);
            editCustomerButton.setDisable(!hasSelection);
            deactivateCustomerButton.setDisable(!hasSelection);
        });
    }

    private void loadCustomers() {
        List<Customer> customers = customerService.listActiveCustomers();
        updateTable(customers);
        showMessage(customers.size() + " cliente(s) ativo(s).");
    }

    private void updateTable(List<Customer> customers) {
        customersTable.setItems(FXCollections.observableArrayList(customers));
    }

    private Customer getSelectedCustomer() {
        return customersTable.getSelectionModel().getSelectedItem();
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }

    private Optional<CustomerFormData> showCustomerDialog(String title, Customer customer) {
        Dialog<CustomerFormData> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.initOwner(customersTable.getScene().getWindow());

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
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(createCustomerProfileScrollContent(customer));
        dialog.showAndWait();
    }

    private ScrollPane createCustomerProfileScrollContent(Customer customer) {
        ScrollPane scrollPane = new ScrollPane(createCustomerProfileContent(customer));
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(760);
        scrollPane.setPrefViewportHeight(420);
        return scrollPane;
    }

    private VBox createCustomerProfileContent(Customer customer) {
        VBox content = new VBox(14);
        content.setPadding(new Insets(16));
        content.setPrefWidth(720);

        Label documentLabel = new Label("Documento: " + textValue(customer.getDocument()));
        Label phoneLabel = new Label("Telefone: " + textValue(customer.getPhone()));
        Label emailLabel = new Label("Email: " + textValue(customer.getEmail()));
        Label addressLabel = new Label("Endereço: " + textValue(customer.getAddress()));
        Label purchasesTitle = new Label("Histórico de compras");
        purchasesTitle.getStyleClass().add("panel-title");

        TableView<Sale> purchasesTable = createPurchasesTable(customer);

        content.getChildren().addAll(
                documentLabel,
                phoneLabel,
                emailLabel,
                addressLabel,
                purchasesTitle,
                purchasesTable
        );

        return content;
    }

    private TableView<Sale> createPurchasesTable(Customer customer) {
        TableView<Sale> purchasesTable = new TableView<>();
        purchasesTable.setPrefHeight(260);

        TableColumn<Sale, String> dateColumn = new TableColumn<>("Data");
        dateColumn.setPrefWidth(150);
        dateColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                cellData.getValue().getCreatedAt().format(DATE_TIME_FORMAT)
        ));

        TableColumn<Sale, String> paymentColumn = new TableColumn<>("Pagamento");
        paymentColumn.setPrefWidth(160);
        paymentColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getPaymentMethod()));

        TableColumn<Sale, String> totalColumn = new TableColumn<>("Total");
        totalColumn.setPrefWidth(140);
        totalColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                MONEY_FORMAT.format(cellData.getValue().getTotal())
        ));

        TableColumn<Sale, String> discountColumn = new TableColumn<>("Desconto");
        discountColumn.setPrefWidth(140);
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

    private String textValue(String value) {
        return value == null ? "" : value;
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
