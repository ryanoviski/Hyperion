package com.hyperion.util;

import com.hyperion.config.DatabaseConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Configures a rotating local support log without depending on a console. */
public final class ApplicationLogger {

    private static final Logger LOGGER = Logger.getLogger("com.hyperion");
    private static boolean configured;

    private ApplicationLogger() {
    }

    public static synchronized void configure() {
        if (configured) {
            return;
        }

        try {
            Path logDirectory = DatabaseConfig.getDataDirectory().resolve("logs");
            Files.createDirectories(logDirectory);
            FileHandler handler = new FileHandler(
                    logDirectory.resolve("hyperion-%g.log").toString(),
                    1_048_576,
                    5,
                    true
            );
            handler.setEncoding("UTF-8");
            handler.setFormatter(new SimpleFormatter());
            LOGGER.setUseParentHandlers(false);
            LOGGER.setLevel(Level.ALL);
            LOGGER.addHandler(handler);
            configured = true;
        } catch (IOException exception) {
            Logger.getLogger(ApplicationLogger.class.getName())
                    .log(Level.WARNING, "Não foi possível configurar o arquivo de log.", exception);
        }
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
