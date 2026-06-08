package com.hyperion.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public final class SceneManager {

    private static final String APP_STYLESHEET = "/css/app.css";

    private static Stage mainStage;

    private SceneManager() {
    }

    public static void setStage(Stage stage) {
        mainStage = stage;
    }

    public static void switchTo(String fxmlPath) {
        ensureStageIsConfigured();

        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    SceneManager.class.getResource(fxmlPath)
            ));

            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(
                    SceneManager.class.getResource(APP_STYLESHEET)
            ).toExternalForm());

            mainStage.setScene(scene);
            mainStage.centerOnScreen();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load FXML: " + fxmlPath, exception);
        }
    }

    private static void ensureStageIsConfigured() {
        if (mainStage == null) {
            throw new IllegalStateException("Main stage was not configured.");
        }
    }
}
