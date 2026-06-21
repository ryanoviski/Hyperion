package com.hyperion.controller;

import com.hyperion.model.Customer;
import com.hyperion.service.CustomerService;
import com.hyperion.util.SceneManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

public class CustomerController {

    private final CustomerService customerService = new CustomerService();

    @FXML
    private TextField searchField;

    @FXML
    private Button editCustomerButton;

    @FXML
    private Button deactivateCustomerButton;

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
    private void handleBackToDashboard() {
        SceneManager.switchTo("/fxml/dashboard-view.fxml");
    }

    @FXML
    private void handleNewCustomer() {
        showMessage("Formulario de novo cliente sera criado no proximo passo.");
    }

    @FXML
    private void handleEditCustomer() {
        Customer selectedCustomer = getSelectedCustomer();

        if (selectedCustomer == null) {
            showMessage("Selecione um cliente para editar.");
            return;
        }

        showMessage("Edicao do cliente sera criada no proximo passo: " + selectedCustomer.getName() + ".");
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
        editCustomerButton.setDisable(true);
        deactivateCustomerButton.setDisable(true);

        customersTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedCustomer) -> {
            boolean hasSelection = selectedCustomer != null;
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
}
