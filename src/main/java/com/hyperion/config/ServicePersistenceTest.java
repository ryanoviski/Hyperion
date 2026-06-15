package com.hyperion.config;

import com.hyperion.service.AppSettingsService;

public class ServicePersistenceTest {

    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        AppSettingsService appSettingsService = new AppSettingsService();
        System.out.println("First run completed: " + appSettingsService.isFirstRunCompleted());
    }
}
