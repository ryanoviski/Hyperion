package com.hyperion.util;

import com.hyperion.model.AppTheme;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class ThemeManager {

    private static final String DARK_STYLESHEET = "/css/app.css";
    private static final String LIGHT_STYLESHEET = "/css/app-light.css";

    private static AppTheme currentTheme = AppTheme.DARK;

    private ThemeManager() {
    }

    public static AppTheme getCurrentTheme() {
        return currentTheme;
    }

    public static void setCurrentTheme(AppTheme theme) {
        currentTheme = theme == null ? AppTheme.DARK : theme;
    }

    public static void applyTo(Scene scene) {
        if (scene == null) {
            return;
        }

        scene.getStylesheets().setAll(getActiveStylesheets());
    }

    public static void applyTo(DialogPane dialogPane) {
        if (dialogPane == null) {
            return;
        }

        dialogPane.getStylesheets().setAll(getActiveStylesheets());
    }

    private static List<String> getActiveStylesheets() {
        List<String> stylesheets = new ArrayList<>();
        stylesheets.add(getStylesheet(DARK_STYLESHEET));

        if (currentTheme == AppTheme.LIGHT) {
            stylesheets.add(getStylesheet(LIGHT_STYLESHEET));
        }

        return stylesheets;
    }

    private static String getStylesheet(String stylesheetPath) {
        URL stylesheet = ThemeManager.class.getResource(stylesheetPath);
        if (stylesheet == null) {
            throw new IllegalStateException("Could not load stylesheet: " + stylesheetPath);
        }

        return stylesheet.toExternalForm();
    }
}
