package com.hyperion.app;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.config.DatabaseInitializer;
import com.hyperion.util.ApplicationLogger;

import java.util.Arrays;

public class AppLauncher {

    private static final String HEALTHCHECK_ARGUMENT = "--healthcheck";
    private static final String DATA_DIRECTORY_ARGUMENT = "--data-dir=";

    public static void main(String[] args) {
        configureDataDirectory(args);
        if (Arrays.asList(args).contains(HEALTHCHECK_ARGUMENT)) {
            ApplicationLogger.configure();
            DatabaseInitializer.initialize();
            return;
        }
        MainApp.main(args);
    }

    private static void configureDataDirectory(String[] args) {
        for (String argument : args) {
            if (argument != null && argument.startsWith(DATA_DIRECTORY_ARGUMENT)) {
                String directory = argument.substring(DATA_DIRECTORY_ARGUMENT.length()).trim();
                if (!directory.isBlank()) {
                    System.setProperty(DatabaseConfig.DATA_DIRECTORY_PROPERTY, directory);
                }
            }
        }
    }
}
