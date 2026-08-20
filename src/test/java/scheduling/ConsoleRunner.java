package scheduling;

import java.io.File;
import java.util.concurrent.locks.LockSupport;

import scheduling.common.ThreadsController;

/**
 * Entry point for integration tests that exercises {@link ThreadsController} directly,
 * bypassing {@code Main} (which requires a real {@link java.io.Console} for console mode).
 * The non-daemon executor threads inside {@link ThreadsController} keep the JVM alive
 * until {@link ThreadsController#} calls {@code System.exit(0)}.
 */
public final class ConsoleRunner {

    private ConsoleRunner() {
    }

    static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: ConsoleRunner <input-file>");
            System.exit(1);
        }

        File input = new File(args[0]);
        ThreadsController threadsController = new ThreadsController(input, null);

        Thread controllerThread = new Thread(threadsController, "ThreadsController");
        controllerThread.start();

        while (controllerThread.isAlive()) {
            LockSupport.parkNanos(50_000_000L);
        }
    }
}
