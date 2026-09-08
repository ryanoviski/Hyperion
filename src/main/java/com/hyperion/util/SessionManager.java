package com.hyperion.util;

import com.hyperion.service.AppSettingsService;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.input.InputEvent;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.logging.Level;

/** Locks a PIN-protected session after a configurable period without input. */
public final class SessionManager {

    private static final String UNLOCK_VIEW = "/fxml/unlock-view.fxml";
    private static final SessionManager INSTANCE = new SessionManager();

    private final AppSettingsService appSettingsService = new AppSettingsService();
    private final PauseTransition inactivityTimer = new PauseTransition();
    private boolean locked;

    private SessionManager() {
        inactivityTimer.setOnFinished(event -> lockForInactivity());
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void install(Stage stage) {
        stage.sceneProperty().addListener((observable, previousScene, currentScene) -> configureScene(currentScene));
        configureScene(stage.getScene());
    }

    public void unlock() {
        locked = false;
        resetTimer();
    }

    public void onSceneChanged(String fxmlPath) {
        if (UNLOCK_VIEW.equals(fxmlPath)) {
            inactivityTimer.stop();
        } else if (!locked) {
            resetTimer();
        }
    }

    private void configureScene(Scene scene) {
        if (scene == null) {
            return;
        }
        scene.addEventFilter(InputEvent.ANY, event -> resetTimer());
        resetTimer();
    }

    private void resetTimer() {
        if (locked || UNLOCK_VIEW.equals(SceneManager.getCurrentView()) || !appSettingsService.isPinEnabled()) {
            inactivityTimer.stop();
            return;
        }
        inactivityTimer.setDuration(Duration.seconds(resolveTimeoutSeconds()));
        inactivityTimer.playFromStart();
    }

    private void lockForInactivity() {
        if (locked || !appSettingsService.isPinEnabled() || UNLOCK_VIEW.equals(SceneManager.getCurrentView())) {
            return;
        }
        locked = true;
        ApplicationLogger.getLogger().log(Level.INFO, "Sessão bloqueada por inatividade.");
        SceneManager.switchTo(UNLOCK_VIEW);
    }

    private double resolveTimeoutSeconds() {
        String configured = System.getProperty("hyperion.session.timeout.seconds", "600");
        try {
            return Math.max(30, Math.min(3_600, Double.parseDouble(configured)));
        } catch (NumberFormatException exception) {
            return 600;
        }
    }
}
