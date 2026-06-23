package com.hyperion.service;

public class StartupService {

    private static final String ONBOARDING_VIEW = "/fxml/onboarding-view.fxml";
    private static final String UNLOCK_VIEW = "/fxml/unlock-view.fxml";
    private static final String MAIN_VIEW = "/fxml/main-view.fxml";

    private final AppSettingsService appSettingsService = new AppSettingsService();

    public String getInitialView() {
        if (!appSettingsService.isFirstRunCompleted()) {
            return ONBOARDING_VIEW;
        }

        if (appSettingsService.isPinEnabled()) {
            return UNLOCK_VIEW;
        }

        return MAIN_VIEW;
    }
}
