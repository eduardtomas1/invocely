package app.invocely;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundTaskRunnerTest {
    @Test
    void blockingWorkLeavesTheEventQueueResponsiveAndCompletesOnIt() throws Exception {
        CountDownLatch workStarted = new CountDownLatch(1);
        CountDownLatch releaseWork = new CountDownLatch(1);
        CountDownLatch eventProcessed = new CountDownLatch(1);
        CountDownLatch callbackCompleted = new CountDownLatch(1);
        AtomicBoolean workRanOnEventThread = new AtomicBoolean(true);
        AtomicBoolean callbackRanOnEventThread = new AtomicBoolean(false);
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            BackgroundTaskRunner.run(() -> {
                workRanOnEventThread.set(SwingUtilities.isEventDispatchThread());
                workStarted.countDown();
                if (!releaseWork.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Test did not release background work.");
                }
                return "saved";
            }, value -> {
                callbackRanOnEventThread.set(SwingUtilities.isEventDispatchThread());
                result.set(value);
                callbackCompleted.countDown();
            }, error -> {
                failure.set(error);
                callbackCompleted.countDown();
            });
            SwingUtilities.invokeLater(eventProcessed::countDown);
        });

        try {
            assertTrue(workStarted.await(5, TimeUnit.SECONDS), "Background work did not start.");
            assertTrue(eventProcessed.await(5, TimeUnit.SECONDS),
                    "The event queue was blocked while persistence work was pending.");
        } finally {
            releaseWork.countDown();
        }

        assertTrue(callbackCompleted.await(5, TimeUnit.SECONDS), "Completion callback did not run.");
        assertFalse(workRanOnEventThread.get());
        assertTrue(callbackRanOnEventThread.get());
        assertEquals("saved", result.get());
        assertNull(failure.get());
    }
}
