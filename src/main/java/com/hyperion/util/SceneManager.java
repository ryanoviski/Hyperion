package com.hyperion.util;

import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import com.hyperion.exception.ApplicationResourceException;

public final class SceneManager {

    private static Stage mainStage;
    private static String currentView;

    private SceneManager() {
    }

    public static void setStage(Stage stage) {
        mainStage = stage;
    }

    public static void switchTo(String fxmlPath) {
        ensureStageIsConfigured();

        try {
            boolean wasFullScreen = mainStage.isFullScreen();

            var resource = SceneManager.class.getResource(fxmlPath);
            if (resource == null) {
                throw new ApplicationResourceException("Não foi possível localizar a tela: " + fxmlPath);
            }
            Parent root = FXMLLoader.load(resource);

            Scene scene = new Scene(root);
            ThemeManager.applyTo(scene);

            mainStage.setScene(scene);
            currentView = fxmlPath;
            SessionManager.getInstance().onSceneChanged(fxmlPath);
            if (wasFullScreen) {
                Platform.runLater(() -> mainStage.setFullScreen(true));
            } else {
                mainStage.centerOnScreen();
            }
        } catch (IOException exception) {
            throw new ApplicationResourceException("Não foi possível carregar a tela: " + fxmlPath, exception);
        }
    }

    public static String getCurrentView() {
        return currentView;
    }

    private static void ensureStageIsConfigured() {
        if (mainStage == null) {
            throw new ApplicationResourceException("A janela principal não foi configurada.");
        }
    }
}
