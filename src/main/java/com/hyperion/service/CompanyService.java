package com.hyperion.service;

import com.hyperion.model.Company;
import com.hyperion.repository.CompanyRepository;

public class CompanyService {

    private final CompanyRepository companyRepository = new CompanyRepository();

    public void createInitialCompany(String companyName, String ownerName) {
        String normalizedCompanyName = normalize(companyName);
        String normalizedOwnerName = normalize(ownerName);

        if (normalizedCompanyName.isBlank()) {
            throw new IllegalArgumentException("Informe o nome da empresa.");
        }

        if (normalizedOwnerName.isBlank()) {
            throw new IllegalArgumentException("Informe o seu nome.");
        }

        if (companyRepository.exists()) {
            throw new IllegalStateException("A empresa inicial já foi cadastrada.");
        }

        companyRepository.save(new Company(normalizedCompanyName, normalizedOwnerName));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
