package com.hyperion.config;

import com.hyperion.model.Customer;
import com.hyperion.service.CustomerService;

import java.util.List;

public class CustomerPersistenceTest {

    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        CustomerService customerService = new CustomerService();
        customerService.createCustomer(
                "Cliente Teste",
                "000.000.000-00",
                "(00) 00000-0000",
                "cliente@teste.com",
                "Rua de Teste, 123",
                "Cadastro criado pelo teste manual."
        );

        List<Customer> customers = customerService.searchActiveCustomers("Cliente Teste");

        System.out.println("Customers found: " + customers.size());
        customers.forEach(customer -> System.out.println(customer.getId() + " - " + customer.getName()));
    }
}
