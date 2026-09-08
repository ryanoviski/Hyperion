package com.hyperion.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/** Logs uncaught failures and presents one operator-friendly message. */
public final class GlobalExceptionHandler {

    private static final AtomicBoolean SHOWING_ERROR = new AtomicBoolean();

    private GlobalExceptionHandler() {
    }

    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            ApplicationLogger.getLogger().log(Level.SEVERE,
                    "Falha não tratada na thread " + thread.getName(), throwable);
            if (Platform.isFxApplicationThread()) {
                showOperatorMessage();
            } else {
                Platform.runLater(GlobalExceptionHandler::showOperatorMessage);
            }
        });
    }

    private static void showOperatorMessage() {
        if (!SHOWING_ERROR.compareAndSet(false, true)) {
            return;
        }

        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro no Hyperion");
            alert.setHeaderText("Não foi possível concluir a operação.");
            alert.setContentText("Tente novamente. Se o problema persistir, envie o arquivo de log para o suporte.");
            ThemeManager.applyTo(alert.getDialogPane());
            alert.showAndWait();
        } finally {
            SHOWING_ERROR.set(false);
        }
    }
}
