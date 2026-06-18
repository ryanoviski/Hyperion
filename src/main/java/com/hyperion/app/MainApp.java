package com.hyperion.app;

import com.hyperion.config.DatabaseInitializer;
import com.hyperion.service.AppSettingsService;
import com.hyperion.util.SceneManager;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    private static final String APP_ICON = "/images/app-icon.png";

    private final AppSettingsService appSettingsService = new AppSettingsService();

    @Override
    public void start(Stage stage) {
        DatabaseInitializer.initialize();

        stage.setTitle("Hyperion");
        stage.getIcons().add(new Image(Objects.requireNonNull(
                MainApp.class.getResourceAsStream(APP_ICON)
        )));
        SceneManager.setStage(stage);
        SceneManager.switchTo(getInitialView());
        stage.show();
    }

    private String getInitialView() {
        if (appSettingsService.isFirstRunCompleted()) {
            return "/fxml/dashboard-view.fxml";
        }

        return "/fxml/onboarding-view.fxml";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
