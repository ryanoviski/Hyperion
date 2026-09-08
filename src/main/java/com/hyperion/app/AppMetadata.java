package com.hyperion.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppMetadata {

    private static final String RESOURCE = "/application.properties";
    private static final Properties PROPERTIES = loadProperties();

    private AppMetadata() {
    }

    public static String getName() {
        return PROPERTIES.getProperty("app.name", "Hyperion");
    }

    public static String getVersion() {
        return PROPERTIES.getProperty("app.version", "desenvolvimento");
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream input = AppMetadata.class.getResourceAsStream(RESOURCE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            // Metadata is non-critical; the defaults keep the application usable.
        }

        return properties;
    }
}
