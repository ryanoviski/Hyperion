package com.hyperion.service;

public class StartupService {

    private static final String UNLOCK_VIEW = "/fxml/unlock-view.fxml";
    private static final String PIN_SETUP_VIEW = "/fxml/pin-setup-view.fxml";
    private static final String MAIN_VIEW = "/fxml/main-view.fxml";

    private final AppSettingsService appSettingsService = new AppSettingsService();

    public String getInitialView() {
        if (!appSettingsService.isFirstRunCompleted()) {
            return PIN_SETUP_VIEW;
        }

        if (appSettingsService.isPinEnabled()) {
            return UNLOCK_VIEW;
        }

        return MAIN_VIEW;
    }
}
