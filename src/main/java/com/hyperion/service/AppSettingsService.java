package com.hyperion.service;

import com.hyperion.repository.AppSettingsRepository;
import com.hyperion.util.PinHashUtil;

public class AppSettingsService {

    private static final int MIN_PIN_LENGTH = 4;

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

    public void updatePin(String currentPin, String newPin, String confirmPin) {
        if (isPinEnabled() && !verifyPin(currentPin)) {
            throw new IllegalArgumentException("PIN atual inválido.");
        }

        validateNewPin(newPin, confirmPin);
        appSettingsRepository.updatePin(PinHashUtil.hash(newPin.trim()));
    }

    public void removePin(String currentPin) {
        if (!isPinEnabled()) {
            throw new IllegalStateException("Nenhum PIN está ativo.");
        }

        if (!verifyPin(currentPin)) {
            throw new IllegalArgumentException("PIN atual inválido.");
        }

        appSettingsRepository.removePin();
    }

    private void validateNewPin(String newPin, String confirmPin) {
        String normalizedPin = normalize(newPin);
        String normalizedConfirmation = normalize(confirmPin);

        if (normalizedPin.isBlank()) {
            throw new IllegalArgumentException("Informe o novo PIN.");
        }

        if (normalizedPin.length() < MIN_PIN_LENGTH) {
            throw new IllegalArgumentException("O PIN deve ter pelo menos 4 caracteres.");
        }

        if (!normalizedPin.equals(normalizedConfirmation)) {
            throw new IllegalArgumentException("Os PINs informados não são iguais.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
