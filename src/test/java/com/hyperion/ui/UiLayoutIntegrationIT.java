package com.hyperion.ui;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.config.DatabaseInitializer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiLayoutIntegrationIT {

    private static final List<String> CONTENT_VIEWS = List.of(
            "/fxml/dashboard-view.fxml",
            "/fxml/customers-view.fxml",
            "/fxml/products-view.fxml",
            "/fxml/stock-view.fxml",
            "/fxml/sales-view.fxml",
            "/fxml/credit-view.fxml",
            "/fxml/finance-view.fxml",
            "/fxml/reports-view.fxml",
            "/fxml/settings-view.fxml"
    );

    private static final List<Viewport> VIEWPORTS = List.of(
            new Viewport(1126, 720),
            new Viewport(1680, 1032)
    );

    @TempDir
    static Path testDirectory;

    @BeforeAll
    static void initializeApplication() throws Exception {
        System.setProperty(DatabaseConfig.DATA_DIRECTORY_PROPERTY, testDirectory.resolve("data").toString());
        DatabaseInitializer.initialize();
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException exception) {
            Platform.runLater(started::countDown);
        }
        assertTrue(started.await(15, TimeUnit.SECONDS), "A plataforma JavaFX não foi inicializada.");
        Platform.setImplicitExit(false);
    }

    @AfterAll
    static void closeApplication() {
        Platform.exit();
        System.clearProperty(DatabaseConfig.DATA_DIRECTORY_PROPERTY);
    }

    @Test
    void keepsVisibleControlsWithinTheSupportedDesktopWidths() throws Exception {
        for (Viewport viewport : VIEWPORTS) {
            for (String view : CONTENT_VIEWS) {
                runOnJavaFxThread(() -> assertViewFitsViewport(view, viewport));
            }
        }
    }

    private static void assertViewFitsViewport(String view, Viewport viewport) throws Exception {
        URL resource = UiLayoutIntegrationIT.class.getResource(view);
        assertNotNull(resource, "Tela ausente: " + view);
        Parent root = FXMLLoader.load(resource);
        Stage stage = new Stage();
        try {
            Scene scene = new Scene(root, viewport.width(), viewport.height());
            stage.setScene(scene);
            stage.show();
            root.applyCss();
            root.layout();

            assertTrue(root.minWidth(-1) <= viewport.width(),
                    () -> view + " exige largura mínima maior que " + viewport.width() + "px.");
            for (Node node : root.lookupAll("*")) {
                if (!(node instanceof Control) || !node.isVisible() || node.getScene() == null) {
                    continue;
                }
                Bounds bounds = node.localToScene(node.getBoundsInLocal());
                assertTrue(bounds.getMinX() >= -1 && bounds.getMaxX() <= viewport.width() + 1,
                        () -> view + " contém controle horizontalmente cortado em " + viewport.width() + "px.");
            }
        } finally {
            stage.close();
        }
    }

    private static void runOnJavaFxThread(ThrowingRunnable action) throws Exception {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable exception) {
                failure.set(exception);
            } finally {
                completed.countDown();
            }
        });
        assertTrue(completed.await(30, TimeUnit.SECONDS), "A validação de layout excedeu o tempo limite.");
        if (failure.get() != null) {
            throw new AssertionError("Falha ao validar a interface.", failure.get());
        }
    }

    private record Viewport(double width, double height) {
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
