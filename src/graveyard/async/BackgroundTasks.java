package graveyard.async;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** No GUI controls here. The frontend supplies SwingUtilities::invokeLater for callbacks. */
public final class BackgroundTasks implements Executor, AutoCloseable {
    private final Executor callbackExecutor;
    private final ExecutorService worker;

    public BackgroundTasks(Executor callbackExecutor) {
        this.callbackExecutor = Objects.requireNonNull(callbackExecutor);
        worker = Executors.newSingleThreadExecutor(task -> {
            // The Runnable contains the work; Thread is the worker executing it.
            Thread thread = new Thread(task, "cemetery-database-worker");
            thread.setDaemon(false); // A normal close allows accepted saves to finish.
            return thread;
        });
    }

    public <T> Future<?> submit(Callable<T> task, Consumer<T> success, Consumer<Exception> failure) {
        Objects.requireNonNull(task);
        Objects.requireNonNull(success);
        Objects.requireNonNull(failure);
        return worker.submit(() -> {
            T result;
            try {
                result = task.call();
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                callbackExecutor.execute(() -> failure.accept(ex));
                return;
            }
            callbackExecutor.execute(() -> success.accept(result));
        });
    }

    /** M4 multiple interfaces: Executor accepts a Runnable; AutoCloseable handles shutdown. */
    @Override
    public void execute(Runnable task) { worker.execute(Objects.requireNonNull(task)); }

    /** Nonblocking: stop accepting jobs, then finish already accepted jobs. */
    @Override public void close() { worker.shutdown(); }

    /** Call from a shutdown/test thread, never from Swing's event dispatch thread. */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return worker.awaitTermination(timeout, unit);
    }
}
