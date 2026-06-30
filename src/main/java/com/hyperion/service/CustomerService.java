package com.hyperion.service;

import com.hyperion.model.Customer;
import com.hyperion.repository.CustomerRepository;

import java.util.List;
import java.util.Optional;

public class CustomerService {

    private final CustomerRepository customerRepository = new CustomerRepository();

    public void createCustomer(String name, String document, String phone, String email, String address, String notes) {
        String normalizedName = normalize(name);

        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do cliente.");
        }

        customerRepository.save(new Customer(
                normalizedName,
                normalize(document),
                normalize(phone),
                normalize(email),
                normalize(address),
                normalize(notes)
        ));
    }

    public void updateCustomer(Customer customer) {
        if (customer.getId() == null) {
            throw new IllegalArgumentException("Cliente inválido para atualização.");
        }

        if (normalize(customer.getName()).isBlank()) {
            throw new IllegalArgumentException("Informe o nome do cliente.");
        }

        customerRepository.update(customer);
    }

    public void deactivateCustomer(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Cliente inválido para desativação.");
        }

        customerRepository.deactivate(id);
    }

    public Optional<Customer> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        return customerRepository.findById(id);
    }

    public List<Customer> listActiveCustomers() {
        return customerRepository.findAllActive();
    }

    public List<Customer> searchActiveCustomers(String term) {
        String normalizedTerm = normalize(term);

        if (normalizedTerm.isBlank()) {
            return listActiveCustomers();
        }

        return customerRepository.searchActive(normalizedTerm);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
