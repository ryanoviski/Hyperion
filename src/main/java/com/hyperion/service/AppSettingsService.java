package com.hyperion.service;

import com.hyperion.repository.AppSettingsRepository;
import com.hyperion.util.PinHashUtil;

public class AppSettingsService {

    private final AppSettingsRepository appSettingsRepository = new AppSettingsRepository();

    public boolean isFirstRunCompleted() {
        return appSettingsRepository.isFirstRunCompleted();
    }

    public boolean isPinEnabled() {
        return appSettingsRepository.isPinEnabled();
    }

    public void completeFirstRunWithoutPin() {
        appSettingsRepository.completeFirstRunWithoutPin();
    }

    public void completeFirstRunWithPin(String pin) {
        appSettingsRepository.completeFirstRunWithPin(PinHashUtil.hash(pin));
    }

    public boolean verifyPin(String pin) {
        return PinHashUtil.verify(pin, appSettingsRepository.findPinHash());
    }
}
