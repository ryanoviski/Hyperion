package com.hyperion.util;

import javafx.concurrent.Task;

import java.util.Objects;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.function.Consumer;

/** Runs blocking work away from the JavaFX application thread. */
public final class AsyncUiTask {

    private AsyncUiTask() {
    }

    public static <T> void run(
            String operationName,
            Callable<T> operation,
            Consumer<T> onSuccess,
            Consumer<Throwable> onFailure
    ) {
        Objects.requireNonNull(operationName, "operationName");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onFailure, "onFailure");

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operation.call();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            ApplicationLogger.getLogger().log(Level.SEVERE, "Falha ao " + operationName, exception);
            onFailure.accept(exception);
        });

        Thread worker = new Thread(
                task,
                "hyperion-" + operationName.replaceAll("[^a-zA-Z0-9]+", "-").toLowerCase(Locale.ROOT)
        );
        worker.setDaemon(true);
        worker.start();
    }
}
