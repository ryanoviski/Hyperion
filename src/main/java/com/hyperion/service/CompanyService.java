package com.hyperion.service;

import com.hyperion.model.Company;
import com.hyperion.repository.CompanyRepository;
import com.hyperion.exception.CompanyAlreadyRegisteredException;
import com.hyperion.exception.ValidationException;

public class CompanyService {

    private final CompanyRepository companyRepository = new CompanyRepository();

    public void createInitialCompany(String companyName, String ownerName) {
        String normalizedCompanyName = normalize(companyName);
        String normalizedOwnerName = normalize(ownerName);

        if (normalizedCompanyName.isBlank()) {
            throw new ValidationException("Informe o nome da empresa.");
        }

        if (normalizedOwnerName.isBlank()) {
            throw new ValidationException("Informe o seu nome.");
        }

        if (companyRepository.exists()) {
            throw new CompanyAlreadyRegisteredException();
        }

        companyRepository.save(new Company(normalizedCompanyName, normalizedOwnerName));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
