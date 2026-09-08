package com.hyperion.app;

import com.hyperion.config.DatabaseInitializer;
import com.hyperion.service.AppSettingsService;
import com.hyperion.service.StartupService;
import com.hyperion.util.SceneManager;
import com.hyperion.util.ThemeManager;
import com.hyperion.exception.ApplicationResourceException;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;


public class MainApp extends Application {

    private static final String APP_ICON = "/images/app-icon.png";

    private final StartupService startupService = new StartupService();

    @Override
    public void start(Stage stage) {
        DatabaseInitializer.initialize();
        ThemeManager.setCurrentTheme(new AppSettingsService().getTheme());

        stage.setTitle(AppMetadata.getName() + " " + AppMetadata.getVersion());
        var iconStream = MainApp.class.getResourceAsStream(APP_ICON);
        if (iconStream == null) {
            throw new ApplicationResourceException("Não foi possível localizar o ícone da aplicação.");
        }
        stage.getIcons().add(new Image(iconStream));
        SceneManager.setStage(stage);
        SceneManager.switchTo(startupService.getInitialView());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
