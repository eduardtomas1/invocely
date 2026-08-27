package app.invocely;

import javax.swing.SwingWorker;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Runs blocking application work away from Swing's event dispatch thread. */
final class BackgroundTaskRunner {
    private BackgroundTaskRunner() { }

    static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    onFailure.accept(error);
                } catch (ExecutionException error) {
                    onFailure.accept(error.getCause() != null ? error.getCause() : error);
                }
            }
        }.execute();
    }
}
