package scheduling.common;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import scheduling.spreadsheet.SpreadsheetReader;
import scheduling.spreadsheet.SpreadsheetWriter;
import scheduling.ui.UiController;

public class ThreadsController implements Runnable {
	private final File inputFile;
	private final SpreadsheetReader spreadsheetReader;
	private final UiController uiController;
	private final boolean inUIMode;
	private int numberOfFinishedSolutions;
	private boolean outputHasBeenWritten;
	private Solution bestSolution;
	private volatile boolean stopped;
	private final AtomicBoolean informedAboutSolvableSchedule;
	private final ReentrantLock setSolutionLock;
	private ExecutorService executorService;

	public ThreadsController(File file, UiController uiController) {
		inputFile = file;
		spreadsheetReader = new SpreadsheetReader(inputFile);
		this.uiController = uiController;
		this.inUIMode = uiController != null;
		numberOfFinishedSolutions = 0;
		outputHasBeenWritten = false;
		bestSolution = null;
		stopped = false;
		informedAboutSolvableSchedule = new AtomicBoolean(false);
		setSolutionLock = new ReentrantLock();
	}

	@Override
	public void run() {
		if (!inputFile.exists()) {
			println("Error: The provided input file does not exist");
			finished();
			return;
		}

		try {
			spreadsheetReader.run();
			println("Input file has been read successfully, computing solutions...");

			executorService = Executors.newFixedThreadPool(Config.NUMBER_OF_PARALLEL_THREADS);

			for (int currentSolutionThread = 0; currentSolutionThread < Config.NUMBER_OF_PARALLEL_THREADS; currentSolutionThread++) {
				SolutionController currentSolutionController = new SolutionController(this);
				executorService.submit(currentSolutionController);
			}

			executorService.shutdown();

			try {
				executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				println("Warning: Thread pool interrupted while waiting for tasks to complete.");
				Thread.currentThread().interrupt();
			}

		} catch (Exception exception) {
			println("Error: " + exception.getMessage());
			finished();
		} finally {
			if (executorService != null && !executorService.isShutdown()) {
				executorService.shutdownNow();
			}
		}
	}

	public void informAboutSolvabilityOfSchedule(boolean solvable) {
		if (informedAboutSolvableSchedule.compareAndSet(false, true)) {
			if (solvable) {
				println("Success: This schedule is solvable");
			} else {
				println("Warning: This schedule is not solvable");
			}
		}
	}

	public void println(String message) {
		if (inUIMode) {
			uiController.println(message);
		} else {
			System.out.println(message);
		}
	}

	public void stop() {
		stopped = true;
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdownNow();
		}
	}

	public boolean isStopped() {
		return stopped;
	}

	public SpreadsheetReader getSpreadsheetReader() {
		return spreadsheetReader;
	}

	public void setSolution(Solution solution) {
		setSolutionLock.lock();
		try {
			if (this.bestSolution == null || solution.getCosts() < bestSolution.getCosts()) {
				bestSolution = solution;
				println("Costs of solution: " + bestSolution.getCosts());

				if (solution.getCosts() == Config.OPTIMAL_SOLUTION) {
					stop();
					numberOfFinishedSolutions = Config.NUMBER_OF_PARALLEL_THREADS;
				}
			}
			numberOfFinishedSolutions++;

			if ((!outputHasBeenWritten) && numberOfFinishedSolutions >= Config.NUMBER_OF_PARALLEL_THREADS) {
				outputHasBeenWritten = true;
				writeOutput();
				finished();
			}
		} finally {
			setSolutionLock.unlock();
		}
	}

	private void writeOutput() {
		try {
			new SpreadsheetWriter(bestSolution, this).run();
		} catch (Exception exception) {
			println("Error writing output: " + exception.getMessage());
		}
	}

	private void finished() {
		if (inUIMode) {
			stop();
			uiController.finished();
		} else {
			System.exit(0);
		}
	}
}
