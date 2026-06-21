package com.hyperion.app;

import com.hyperion.config.DatabaseInitializer;
import com.hyperion.service.StartupService;
import com.hyperion.util.SceneManager;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    private static final String APP_ICON = "/images/app-icon.png";

    private final StartupService startupService = new StartupService();

    @Override
    public void start(Stage stage) {
        DatabaseInitializer.initialize();

        stage.setTitle("Hyperion");
        stage.getIcons().add(new Image(Objects.requireNonNull(
                MainApp.class.getResourceAsStream(APP_ICON)
        )));
        SceneManager.setStage(stage);
        SceneManager.switchTo(startupService.getInitialView());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
