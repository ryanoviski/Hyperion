package com.hyperion.service;

import com.hyperion.repository.AppSettingsRepository;
import com.hyperion.util.PinHashUtil;

public class AppSettingsService {

    private final AppSettingsRepository appSettingsRepository = new AppSettingsRepository();

    public boolean isFirstRunCompleted() {
        return appSettingsRepository.isFirstRunCompleted();
    }

    public void completeFirstRunWithoutPin() {
        appSettingsRepository.completeFirstRunWithoutPin();
    }

    public void completeFirstRunWithPin(String pin) {
        appSettingsRepository.completeFirstRunWithPin(PinHashUtil.hash(pin));
    }
}
