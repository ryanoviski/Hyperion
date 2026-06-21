package com.hyperion.config;

import com.hyperion.model.Company;
import com.hyperion.repository.CompanyRepository;
import com.hyperion.service.AppSettingsService;
import com.hyperion.service.StartupService;

import java.util.Optional;

public class InitialFlowDiagnostics {

    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        AppSettingsService appSettingsService = new AppSettingsService();
        CompanyRepository companyRepository = new CompanyRepository();
        StartupService startupService = new StartupService();

        Optional<Company> company = companyRepository.findFirst();

        System.out.println("Database file: " + DatabaseConfig.getDatabaseFile().toAbsolutePath());
        System.out.println("Company exists: " + companyRepository.exists());
        company.ifPresent(value -> {
            System.out.println("Company name: " + value.getName());
            System.out.println("Owner name: " + value.getOwnerName());
        });
        System.out.println("First run completed: " + appSettingsService.isFirstRunCompleted());
        System.out.println("PIN enabled: " + appSettingsService.isPinEnabled());
        System.out.println("Initial view: " + startupService.getInitialView());
    }
}
