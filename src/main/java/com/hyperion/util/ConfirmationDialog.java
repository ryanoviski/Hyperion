package com.hyperion.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

public final class ConfirmationDialog {

    private ConfirmationDialog() {
    }

    public static boolean confirm(Window owner, String title, String header, String message) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle(title);
        confirmation.setHeaderText(header);
        confirmation.setContentText(message);
        if (owner != null) {
            confirmation.initOwner(owner);
        }
        ThemeManager.applyTo(confirmation.getDialogPane());
        return confirmation.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }
}
