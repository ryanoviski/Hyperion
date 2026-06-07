package com.hyperion.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/onboarding-view.fxml"));

        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(Objects.requireNonNull(
                MainApp.class.getResource("/css/app.css")
        ).toExternalForm());

        stage.setTitle("Hyperion");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
