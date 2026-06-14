package com.hyperion.app;

import com.hyperion.config.DatabaseInitializer;
import com.hyperion.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        DatabaseInitializer.initialize();

        stage.setTitle("Hyperion");
        SceneManager.setStage(stage);
        SceneManager.switchTo("/fxml/onboarding-view.fxml");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
